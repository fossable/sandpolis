//! The CVE matching service.
//!
//! Each pass refreshes a cached copy of the NVD feed (the fkie-cad mirror,
//! which republishes NVD API 2.0 records as per-year files) in the server's
//! data directory, then streams every record past the packages agents have
//! reported. Matching CPE criteria against distribution package names is
//! inherently fuzzy — CPE names products, not packages — so the matcher stays
//! deliberately conservative: only application CPEs, only exact (normalized)
//! name equality, and only version constraints that actually bound something.

use super::{CveSeverity, VulnerabilityData};
use crate::package::PackageData;
use crate::version::vercmp;
use anyhow::{Context, Result};
use liblzma::read::XzDecoder;
use sandpolis_instance::database::{DataScope, RealmDatabase};
use sandpolis_instance::notification::{self, Notification};
use sandpolis_instance::service::{Service, ServiceReport, ServiceSchedule};
use sandpolis_instance::{InstanceId, LayerName};
use serde::Deserialize;
use std::cmp::Ordering;
use std::collections::{HashMap, HashSet};
use std::fs;
use std::io::BufReader;
use std::path::{Path, PathBuf};
use std::sync::Arc;
use std::time::Duration;
use tokio_util::sync::CancellationToken;
use tracing::{debug, info, warn};

/// The first year the feed publishes a file for.
const FIRST_FEED_YEAR: i32 = 1999;

/// How much of a CVE description is kept on a finding.
const MAX_SUMMARY_CHARS: usize = 240;

/// Decides whether this server currently owns an instance's data. Passed in
/// from the server binary so this crate doesn't depend on the ownership table.
pub type OwnedFn = Arc<dyn Fn(InstanceId) -> bool + Send + Sync>;

/// Matches the NVD feed against every owned agent's installed packages.
pub struct CveService {
    realm: RealmDatabase,
    /// Where feed files are cached, `<data>/cve`.
    dir: PathBuf,
    http: reqwest::Client,
    config: crate::config::CveConfig,
    owned: OwnedFn,
}

impl CveService {
    pub fn new(
        realm: RealmDatabase,
        dir: PathBuf,
        config: &crate::config::CveConfig,
        owned: OwnedFn,
    ) -> Result<Self> {
        Ok(Self {
            realm,
            dir,
            http: reqwest::Client::builder()
                .user_agent(concat!(
                    "sandpolis/",
                    env!("CARGO_PKG_VERSION"),
                    " (+https://github.com/fossable/sandpolis)"
                ))
                .build()?,
            config: config.clone(),
            owned,
        })
    }
}

impl Service for CveService {
    fn name(&self) -> &'static str {
        "cve"
    }

    fn layer(&self) -> LayerName {
        LayerName::from("Inventory")
    }

    fn description(&self) -> &'static str {
        "Matches public CVE feeds against installed packages"
    }

    fn schedule(&self) -> ServiceSchedule {
        ServiceSchedule::every(Duration::from_secs(self.config.interval.max(60)))
    }

    async fn run(&self, cancel: CancellationToken) -> Result<ServiceReport> {
        let mut report = ServiceReport::default();

        // Nothing to match against means nothing to download either.
        let packages = owned_packages(&self.realm, &self.owned)?;
        if packages.is_empty() {
            debug!("No packages reported by any owned agent yet");
            return Ok(report);
        }

        report.failed += self.refresh_feed(&cancel).await;
        if cancel.is_cancelled() {
            return Ok(report);
        }

        // Parsing the whole feed is seconds of pure CPU, which doesn't belong
        // on the async executor.
        let dir = self.dir.clone();
        let min_severity = self.config.min_severity;
        let by_name = index_by_name(&packages);
        let pass_cancel = cancel.clone();
        let outcome = tokio::task::spawn_blocking(move || {
            match_feed(&dir, &by_name, min_severity, &pass_cancel)
        })
        .await??;
        report.scanned += outcome.scanned;
        report.failed += outcome.failed;

        if outcome.parsed_files == 0 {
            // No feed at all (first run offline, say): report the failures
            // rather than treating "no data" as "no vulnerabilities".
            return Ok(report);
        }

        // A pass that failed to read part of the feed can still add findings,
        // but must not remove any: the missing files are exactly where the
        // evidence for the existing rows lives.
        let clean = outcome.failed == 0 && !cancel.is_cancelled();
        let mut new_findings = 0;
        let mut new_agents = 0;
        let mut worst = CveSeverity::Low;
        for instance in packages.keys() {
            let found = outcome.findings.get(instance);
            match store(&self.realm, *instance, found, clean) {
                Ok(inserted) => {
                    report.updated += inserted.len() as u64;
                    if !inserted.is_empty() {
                        new_findings += inserted.len();
                        new_agents += 1;
                        worst = worst.max(inserted.iter().map(|f| f.severity).max().unwrap());
                    }
                }
                // An LS loses write authority when ownership moves away
                // mid-pass; the new owner's next pass covers this instance.
                Err(e) => debug!(instance = %instance, error = %e, "Not storing findings"),
            }
        }

        if new_findings > 0 {
            info!(
                count = new_findings,
                agents = new_agents,
                "Found new vulnerabilities"
            );
            let title = format!(
                "{new_findings} new {} across {new_agents} {}",
                if new_findings == 1 {
                    "vulnerability"
                } else {
                    "vulnerabilities"
                },
                if new_agents == 1 { "agent" } else { "agents" },
            );
            let notification = if worst >= CveSeverity::Critical {
                Notification::error("Inventory", title)
            } else {
                Notification::warn("Inventory", title)
            };
            notification::notify(notification.body(format!("Worst severity: {worst}")));
        }
        Ok(report)
    }
}

impl CveService {
    /// Bring the cached feed files up to date, returning how many downloads
    /// failed. A failed download leaves any previously cached file in place.
    async fn refresh_feed(&self, cancel: &CancellationToken) -> u64 {
        if let Err(e) = fs::create_dir_all(&self.dir) {
            warn!(dir = %self.dir.display(), error = %e, "Cannot create the feed cache");
            return 1;
        }

        let mut failed = 0;
        let current_year = chrono::Datelike::year(&chrono::Utc::now());
        let refresh_after = Duration::from_secs(self.config.refresh_after);

        // The overlay files change daily and are small, so they refresh every
        // pass; the year files only once the refresh window lapses.
        let mut files: Vec<(String, bool)> = vec![
            ("CVE-Modified.json.xz".into(), true),
            ("CVE-Recent.json.xz".into(), true),
        ];
        for year in FIRST_FEED_YEAR..=current_year {
            files.push((format!("CVE-{year}.json.xz"), false));
        }

        for (name, always) in files {
            if cancel.is_cancelled() {
                break;
            }
            let path = self.dir.join(&name);
            if !always && is_fresh(&path, refresh_after) {
                continue;
            }
            if let Err(e) = self.download(&name, &path).await {
                debug!(file = %name, error = %e, "Failed to download a feed file");
                failed += 1;
            }
        }
        failed
    }

    /// Download one feed file, replacing `path` only once the whole body is on
    /// disk so an interrupted transfer never corrupts the cache.
    async fn download(&self, name: &str, path: &Path) -> Result<()> {
        let url = format!("{}/{}", self.config.feed_url.trim_end_matches('/'), name);
        let response = self.http.get(&url).send().await?.error_for_status()?;
        let body = response.bytes().await?;

        let partial = path.with_extension("part");
        fs::write(&partial, &body)?;
        fs::rename(&partial, path)?;
        debug!(file = %name, bytes = body.len(), "Downloaded a feed file");
        Ok(())
    }
}

/// Whether a cached file exists and is younger than `refresh_after`.
fn is_fresh(path: &Path, refresh_after: Duration) -> bool {
    fs::metadata(path)
        .and_then(|m| m.modified())
        .ok()
        .and_then(|modified| modified.elapsed().ok())
        .is_some_and(|age| age < refresh_after)
}

/// One installed package, as the matcher needs it.
#[derive(Debug, Clone)]
struct InstalledPackage {
    instance: InstanceId,
    name: String,
    version: String,
}

/// Every package reported by an agent this server owns, grouped by agent.
fn owned_packages(
    realm: &RealmDatabase,
    owned: &OwnedFn,
) -> Result<HashMap<InstanceId, Vec<InstalledPackage>>> {
    let r = realm.r_transaction()?;
    let packages: Vec<PackageData> = r
        .scan()
        .primary::<PackageData>()?
        .all()?
        .collect::<std::result::Result<Vec<_>, _>>()?;
    drop(r);

    let mut map: HashMap<InstanceId, Vec<InstalledPackage>> = HashMap::new();
    for package in packages {
        if package.name.is_empty() || !owned(package._instance_id) {
            continue;
        }
        map.entry(package._instance_id)
            .or_default()
            .push(InstalledPackage {
                instance: package._instance_id,
                name: package.name,
                version: package.version,
            });
    }
    Ok(map)
}

/// Index packages by normalized name for O(1) probes per CPE.
fn index_by_name(
    packages: &HashMap<InstanceId, Vec<InstalledPackage>>,
) -> HashMap<String, Vec<InstalledPackage>> {
    let mut map: HashMap<String, Vec<InstalledPackage>> = HashMap::new();
    for package in packages.values().flatten() {
        map.entry(normalize_name(&package.name))
            .or_default()
            .push(package.clone());
    }
    map
}

/// A matched vulnerability, before it becomes a row.
#[derive(Debug, Clone)]
struct Finding {
    version: String,
    severity: CveSeverity,
    score: Option<f64>,
    summary: Option<String>,
    published: Option<String>,
}

/// What one matching pass over the cached feed produced.
#[derive(Default)]
struct PassOutcome {
    /// Per agent, findings keyed by `(cve_id, package name)`.
    findings: HashMap<InstanceId, HashMap<(String, String), Finding>>,
    /// Feed files that parsed successfully.
    parsed_files: u64,
    /// CVE records considered.
    scanned: u64,
    /// Feed files that couldn't be read or parsed.
    failed: u64,
}

/// Stream every cached feed file past the installed packages.
///
/// The overlay files (`Recent`/`Modified`) carry newer versions of records
/// that also appear in the year files, so they go first and their ids mask the
/// stale copies.
fn match_feed(
    dir: &Path,
    by_name: &HashMap<String, Vec<InstalledPackage>>,
    min_severity: CveSeverity,
    cancel: &CancellationToken,
) -> Result<PassOutcome> {
    let mut outcome = PassOutcome::default();
    let mut masked: HashSet<String> = HashSet::new();

    let mut year_files: Vec<PathBuf> = Vec::new();
    for entry in fs::read_dir(dir).with_context(|| format!("reading {}", dir.display()))? {
        let path = entry?.path();
        let Some(name) = path.file_name().and_then(|n| n.to_str()) else {
            continue;
        };
        if name.starts_with("CVE-1") || name.starts_with("CVE-2") {
            year_files.push(path);
        }
    }
    year_files.sort();

    for (path, overlay) in [
        (dir.join("CVE-Modified.json.xz"), true),
        (dir.join("CVE-Recent.json.xz"), true),
    ]
    .into_iter()
    .chain(year_files.into_iter().map(|p| (p, false)))
    {
        if cancel.is_cancelled() {
            // An interrupted pass parsed less than the whole feed, which the
            // caller must treat like a failed one when deleting stale rows.
            outcome.failed += 1;
            break;
        }
        if overlay && !path.exists() {
            continue;
        }

        match parse_file(&path) {
            Ok(feed) => {
                outcome.parsed_files += 1;
                for record in feed.cve_items {
                    if !overlay && masked.contains(&record.id) {
                        continue;
                    }
                    if overlay {
                        masked.insert(record.id.clone());
                    }
                    outcome.scanned += 1;
                    match_record(&record, by_name, min_severity, &mut outcome.findings);
                }
            }
            Err(e) => {
                warn!(file = %path.display(), error = %e, "Failed to parse a feed file");
                outcome.failed += 1;
            }
        }
    }
    Ok(outcome)
}

/// Parse one `.json.xz` feed file.
///
/// The whole file's records are held in memory at once — tens of megabytes for
/// the biggest year with these lean structs, which beats hand-rolling a
/// streaming JSON visitor for a background pass.
fn parse_file(path: &Path) -> Result<Feed> {
    let file = fs::File::open(path)?;
    Ok(serde_json::from_reader(BufReader::new(XzDecoder::new(
        BufReader::new(file),
    )))?)
}

/// Match one CVE record against the installed packages.
fn match_record(
    record: &CveRecord,
    by_name: &HashMap<String, Vec<InstalledPackage>>,
    min_severity: CveSeverity,
    findings: &mut HashMap<InstanceId, HashMap<(String, String), Finding>>,
) {
    let Some((severity, score)) = record.severity() else {
        // Still awaiting analysis; it usually has no CPE data yet either.
        return;
    };
    if severity < min_severity {
        return;
    }

    for node in record.configurations.iter().flat_map(|c| c.nodes.iter()) {
        // AND/OR/negate semantics are ignored: treating every vulnerable CPE
        // as sufficient trades some false positives (e.g. "product X on
        // Windows") for never missing a listed product.
        for cpe in &node.cpe_match {
            if !cpe.vulnerable {
                continue;
            }
            let Some((part, product, cpe_version)) = cpe_fields(&cpe.criteria) else {
                continue;
            };
            if part != "a" {
                continue;
            }
            let Some(candidates) = by_name.get(&product) else {
                continue;
            };
            for package in candidates {
                if !version_matches(cpe, &cpe_version, normalize_version(&package.version)) {
                    continue;
                }
                findings
                    .entry(package.instance)
                    .or_default()
                    .entry((record.id.clone(), package.name.clone()))
                    .or_insert_with(|| Finding {
                        version: package.version.clone(),
                        severity,
                        score,
                        summary: record.summary(),
                        published: record.published.clone(),
                    });
            }
        }
    }
}

/// Reconcile one agent's stored findings with what this pass computed.
///
/// Only keys with no existing row are inserted — that edge is what makes a
/// finding alert exactly once. Rows whose key the pass no longer produces are
/// removed (package upgraded or gone, threshold raised), but only on a `clean`
/// pass that actually saw the whole feed.
fn store(
    realm: &RealmDatabase,
    instance: InstanceId,
    found: Option<&HashMap<(String, String), Finding>>,
    clean: bool,
) -> Result<Vec<Finding>> {
    let r = realm.r_transaction()?;
    let existing: Vec<VulnerabilityData> = r
        .scan()
        .primary::<VulnerabilityData>()?
        .all()?
        .collect::<std::result::Result<Vec<_>, _>>()?
        .into_iter()
        .filter(|v| v._instance_id == instance)
        .collect();
    drop(r);

    let empty = HashMap::new();
    let found = found.unwrap_or(&empty);
    let stale: Vec<&VulnerabilityData> = if clean {
        existing
            .iter()
            .filter(|v| !found.contains_key(&(v.cve_id.clone(), v.package.clone())))
            .collect()
    } else {
        Vec::new()
    };
    let existing_keys: HashSet<(&str, &str)> = existing
        .iter()
        .map(|v| (v.cve_id.as_str(), v.package.as_str()))
        .collect();
    let new: Vec<(&(String, String), &Finding)> = found
        .iter()
        .filter(|((cve, package), _)| !existing_keys.contains(&(cve.as_str(), package.as_str())))
        .collect();

    if new.is_empty() && stale.is_empty() {
        return Ok(Vec::new());
    }

    let rw = realm.write(DataScope::Instance(instance))?;
    let mut inserted = Vec::new();
    for ((cve_id, package), finding) in new {
        rw.insert(VulnerabilityData {
            _instance_id: instance,
            cve_id: cve_id.clone(),
            package: package.clone(),
            version: finding.version.clone(),
            severity: finding.severity,
            score: finding.score,
            summary: finding.summary.clone(),
            published: finding.published.clone(),
            _id: Default::default(),
            _revision: Default::default(),
            _creation: Default::default(),
        })?;
        inserted.push(finding.clone());
    }
    for row in stale {
        rw.remove(row.clone())?;
    }
    rw.commit()?;
    Ok(inserted)
}

/// Lowercase a package or CPE product name and fold `_` into `-`, the two
/// spellings that differ most often between the CPE dictionary and distros.
fn normalize_name(name: &str) -> String {
    name.to_ascii_lowercase().replace('_', "-")
}

/// The upstream part of a distro version string: `1:2.4.57+dfsg-2ubuntu1`
/// becomes `2.4.57`. CPE versions name upstream releases, so the epoch,
/// packaging revision, and repack suffix would only ever spoil the comparison.
fn normalize_version(version: &str) -> &str {
    let version = match version.split_once(':') {
        Some((epoch, rest)) if epoch.chars().all(|c| c.is_ascii_digit()) => rest,
        _ => version,
    };
    let version = version
        .rsplit_once('-')
        .map(|(upstream, _)| upstream)
        .unwrap_or(version);
    version
        .split_once('+')
        .map(|(base, _)| base)
        .unwrap_or(version)
}

/// The `(part, product, version)` fields of a CPE 2.3 criteria string, with
/// the product normalized. `cpe:2.3:a:vendor:product:version:update:...`
fn cpe_fields(criteria: &str) -> Option<(String, String, String)> {
    let mut fields = criteria.split(':');
    if fields.next()? != "cpe" || fields.next()? != "2.3" {
        return None;
    }
    let part = fields.next()?.to_string();
    let _vendor = fields.next()?;
    let product = normalize_name(fields.next()?);
    let version = fields.next()?.to_string();
    Some((part, product, version))
}

/// Whether an installed version satisfies a cpeMatch's version constraints.
///
/// An exact version in the criteria must compare equal. A wildcard version
/// must be narrowed by at least one range bound — an unbounded product-name
/// match would flag every version ever released, which is pure noise.
fn version_matches(cpe: &CpeMatch, cpe_version: &str, installed: &str) -> bool {
    if cpe_version != "*" && cpe_version != "-" {
        return vercmp(installed, cpe_version) == Ordering::Equal;
    }

    let mut bounded = false;
    for (bound, ok) in [
        (
            &cpe.version_start_including,
            [Ordering::Equal, Ordering::Greater],
        ),
        (
            &cpe.version_start_excluding,
            [Ordering::Greater, Ordering::Greater],
        ),
        (
            &cpe.version_end_including,
            [Ordering::Equal, Ordering::Less],
        ),
        (&cpe.version_end_excluding, [Ordering::Less, Ordering::Less]),
    ] {
        if let Some(bound) = bound {
            bounded = true;
            if !ok.contains(&vercmp(installed, bound)) {
                return false;
            }
        }
    }
    bounded
}

/// The slice of an NVD API 2.0 CVE record the matcher needs; everything else
/// in the feed is skipped at parse time.
#[derive(Deserialize, Default)]
#[serde(default)]
struct Feed {
    cve_items: Vec<CveRecord>,
}

#[derive(Deserialize, Default)]
#[serde(default)]
struct CveRecord {
    id: String,
    published: Option<String>,
    descriptions: Vec<Description>,
    metrics: Metrics,
    configurations: Vec<Configuration>,
}

impl CveRecord {
    /// The record's severity and base score, from the newest CVSS version it
    /// carries.
    fn severity(&self) -> Option<(CveSeverity, Option<f64>)> {
        let preferred = self
            .metrics
            .cvss_metric_v31
            .first()
            .or(self.metrics.cvss_metric_v30.first());
        let v2 = self.metrics.cvss_metric_v2.first();

        let severity = CveSeverity::from_nvd(
            preferred.and_then(|m| m.cvss_data.base_severity.as_deref()),
            None,
            v2.and_then(|m| m.cvss_data.base_score),
        )?;
        let score = preferred.or(v2).and_then(|m| m.cvss_data.base_score);
        Some((severity, score))
    }

    /// The English description, truncated to [`MAX_SUMMARY_CHARS`].
    fn summary(&self) -> Option<String> {
        let text = &self
            .descriptions
            .iter()
            .find(|d| d.lang == "en")
            .or(self.descriptions.first())?
            .value;
        Some(match text.char_indices().nth(MAX_SUMMARY_CHARS) {
            Some((cut, _)) => format!("{}…", &text[..cut]),
            None => text.clone(),
        })
    }
}

#[derive(Deserialize, Default)]
#[serde(default)]
struct Description {
    lang: String,
    value: String,
}

#[derive(Deserialize, Default)]
#[serde(default, rename_all = "camelCase")]
struct Metrics {
    cvss_metric_v31: Vec<CvssMetric>,
    cvss_metric_v30: Vec<CvssMetric>,
    cvss_metric_v2: Vec<CvssMetric>,
}

#[derive(Deserialize, Default)]
#[serde(default, rename_all = "camelCase")]
struct CvssMetric {
    cvss_data: CvssData,
}

#[derive(Deserialize, Default)]
#[serde(default, rename_all = "camelCase")]
struct CvssData {
    base_score: Option<f64>,
    base_severity: Option<String>,
}

#[derive(Deserialize, Default)]
#[serde(default)]
struct Configuration {
    nodes: Vec<Node>,
}

#[derive(Deserialize, Default)]
#[serde(default, rename_all = "camelCase")]
struct Node {
    cpe_match: Vec<CpeMatch>,
}

#[derive(Deserialize, Default)]
#[serde(default, rename_all = "camelCase")]
struct CpeMatch {
    vulnerable: bool,
    criteria: String,
    version_start_including: Option<String>,
    version_start_excluding: Option<String>,
    version_end_including: Option<String>,
    version_end_excluding: Option<String>,
}

#[cfg(test)]
mod tests {
    use super::*;
    use sandpolis_instance::InstanceType;
    use sandpolis_instance::database::DatabaseManager;
    use sandpolis_instance::realm::RealmName;
    use sandpolis_instance::test_db;
    use std::io::Write;

    #[test]
    fn normalize_version_strips_packaging() {
        assert_eq!(normalize_version("1:2.4.57+dfsg-2ubuntu1"), "2.4.57");
        assert_eq!(normalize_version("1.2.3-1"), "1.2.3");
        assert_eq!(normalize_version("1.2.3"), "1.2.3");
        assert_eq!(normalize_version("20230101-2"), "20230101");
        assert_eq!(normalize_version("2:1.0"), "1.0");
    }

    #[test]
    fn severity_mapping_prefers_v3_and_buckets_v2() {
        assert_eq!(
            CveSeverity::from_nvd(Some("CRITICAL"), None, None),
            Some(CveSeverity::Critical)
        );
        assert_eq!(
            CveSeverity::from_nvd(None, Some("Medium"), Some(9.0)),
            Some(CveSeverity::Medium)
        );
        assert_eq!(
            CveSeverity::from_nvd(None, None, Some(7.5)),
            Some(CveSeverity::High)
        );
        assert_eq!(
            CveSeverity::from_nvd(None, None, Some(5.0)),
            Some(CveSeverity::Medium)
        );
        assert_eq!(
            CveSeverity::from_nvd(None, None, Some(2.0)),
            Some(CveSeverity::Low)
        );
        assert_eq!(CveSeverity::from_nvd(None, None, None), None);
    }

    #[test]
    fn cpe_fields_parses_criteria() {
        assert_eq!(
            cpe_fields("cpe:2.3:a:openssl:OpenSSL:3.0.13:*:*:*:*:*:*:*"),
            Some(("a".into(), "openssl".into(), "3.0.13".into()))
        );
        assert_eq!(
            cpe_fields("cpe:2.3:o:linux:linux_kernel:*:*:*:*:*:*:*:*"),
            Some(("o".into(), "linux-kernel".into(), "*".into()))
        );
        assert_eq!(cpe_fields("cpe:/a:old:format"), None);
        assert_eq!(cpe_fields("nonsense"), None);
    }

    #[test]
    fn version_matches_requires_a_bound() {
        let range = CpeMatch {
            version_end_excluding: Some("3.0.14".into()),
            ..Default::default()
        };
        assert!(version_matches(&range, "*", "3.0.13"));
        assert!(!version_matches(&range, "*", "3.0.14"));

        let range = CpeMatch {
            version_start_including: Some("2.0".into()),
            version_end_including: Some("2.5".into()),
            ..Default::default()
        };
        assert!(version_matches(&range, "*", "2.0"));
        assert!(version_matches(&range, "*", "2.5"));
        assert!(!version_matches(&range, "*", "1.9"));
        assert!(!version_matches(&range, "*", "2.6"));

        // An exact version in the criteria must compare equal.
        assert!(version_matches(&CpeMatch::default(), "1.2.13", "1.2.13"));
        assert!(!version_matches(&CpeMatch::default(), "1.2.13", "1.2.14"));

        // A wildcard with no bounds matches every version ever released, which
        // is noise, not a finding.
        assert!(!version_matches(&CpeMatch::default(), "*", "1.2.13"));
        assert!(!version_matches(&CpeMatch::default(), "-", "1.2.13"));
    }

    /// A feed file with one HIGH range match, one LOW match (threshold fodder),
    /// one HIGH for a product that isn't installed, and one unbounded wildcard.
    const FEED: &str = r#"{
        "cve_count": 4,
        "cve_items": [
            {
                "id": "CVE-2024-0001",
                "published": "2024-01-01T00:00:00.000",
                "descriptions": [{"lang": "en", "value": "A buffer overflow in OpenSSL."}],
                "metrics": {"cvssMetricV31": [{"cvssData": {"baseScore": 8.1, "baseSeverity": "HIGH"}}]},
                "configurations": [{"nodes": [{"operator": "OR", "cpeMatch": [{
                    "vulnerable": true,
                    "criteria": "cpe:2.3:a:openssl:openssl:*:*:*:*:*:*:*:*",
                    "versionEndExcluding": "3.0.14"
                }]}]}]
            },
            {
                "id": "CVE-2024-0002",
                "descriptions": [{"lang": "en", "value": "A minor issue."}],
                "metrics": {"cvssMetricV31": [{"cvssData": {"baseScore": 2.0, "baseSeverity": "LOW"}}]},
                "configurations": [{"nodes": [{"cpeMatch": [{
                    "vulnerable": true,
                    "criteria": "cpe:2.3:a:openssl:openssl:*:*:*:*:*:*:*:*",
                    "versionEndExcluding": "9.9"
                }]}]}]
            },
            {
                "id": "CVE-2024-0003",
                "descriptions": [{"lang": "en", "value": "Not installed here."}],
                "metrics": {"cvssMetricV31": [{"cvssData": {"baseScore": 9.8, "baseSeverity": "CRITICAL"}}]},
                "configurations": [{"nodes": [{"cpeMatch": [{
                    "vulnerable": true,
                    "criteria": "cpe:2.3:a:f5:nginx:*:*:*:*:*:*:*:*",
                    "versionEndExcluding": "9.9"
                }]}]}]
            },
            {
                "id": "CVE-2024-0004",
                "descriptions": [{"lang": "en", "value": "Unbounded wildcard."}],
                "metrics": {"cvssMetricV31": [{"cvssData": {"baseScore": 9.8, "baseSeverity": "CRITICAL"}}]},
                "configurations": [{"nodes": [{"cpeMatch": [{
                    "vulnerable": true,
                    "criteria": "cpe:2.3:a:openssl:openssl:*:*:*:*:*:*:*:*"
                }]}]}]
            }
        ]
    }"#;

    fn write_feed(dir: &Path, name: &str, json: &str) -> Result<()> {
        let file = fs::File::create(dir.join(name))?;
        let mut encoder = liblzma::write::XzEncoder::new(file, 6);
        encoder.write_all(json.as_bytes())?;
        encoder.finish()?;
        Ok(())
    }

    fn agent_packages(
        instance: InstanceId,
        packages: &[(&str, &str)],
    ) -> HashMap<InstanceId, Vec<InstalledPackage>> {
        let mut map = HashMap::new();
        map.insert(
            instance,
            packages
                .iter()
                .map(|(name, version)| InstalledPackage {
                    instance,
                    name: name.to_string(),
                    version: version.to_string(),
                })
                .collect(),
        );
        map
    }

    fn stored(realm: &RealmDatabase) -> Result<Vec<VulnerabilityData>> {
        let r = realm.r_transaction()?;
        let mut rows: Vec<VulnerabilityData> = r
            .scan()
            .primary::<VulnerabilityData>()?
            .all()?
            .collect::<std::result::Result<Vec<_>, _>>()?;
        rows.sort_by(|a, b| a.cve_id.cmp(&b.cve_id));
        Ok(rows)
    }

    #[test_log::test(tokio::test)]
    async fn full_pass_matches_alerts_once_and_prunes() -> Result<()> {
        let db: DatabaseManager = test_db!(VulnerabilityData);
        let realm = db.realm(RealmName::default())?;
        let instance = InstanceId::new(InstanceType::Agent);
        let dir = tempfile::tempdir()?;
        write_feed(dir.path(), "CVE-2024.json.xz", FEED)?;

        let packages = agent_packages(instance, &[("openssl", "3.0.13-1"), ("zlib", "1.3-2")]);
        let cancel = CancellationToken::new();

        let outcome = match_feed(
            dir.path(),
            &index_by_name(&packages),
            CveSeverity::Medium,
            &cancel,
        )?;
        assert_eq!(outcome.parsed_files, 1);
        assert_eq!(outcome.failed, 0);
        assert_eq!(outcome.scanned, 4);

        // Only the HIGH range match survives: LOW is under the threshold,
        // nginx isn't installed, and the unbounded wildcard is skipped.
        let inserted = store(&realm, instance, outcome.findings.get(&instance), true)?;
        assert_eq!(inserted.len(), 1);
        assert_eq!(inserted[0].severity, CveSeverity::High);

        let rows = stored(&realm)?;
        assert_eq!(rows.len(), 1);
        assert_eq!(rows[0].cve_id, "CVE-2024-0001");
        assert_eq!(rows[0].package, "openssl");
        assert_eq!(rows[0].version, "3.0.13-1");
        assert_eq!(rows[0].score, Some(8.1));
        assert!(rows[0].summary.as_deref().unwrap().contains("OpenSSL"));

        // The same pass again inserts nothing — this is what makes the alert
        // fire at most once.
        let outcome = match_feed(
            dir.path(),
            &index_by_name(&packages),
            CveSeverity::Medium,
            &cancel,
        )?;
        assert!(store(&realm, instance, outcome.findings.get(&instance), true)?.is_empty());
        assert_eq!(stored(&realm)?.len(), 1);

        // Upgrading the package past the vulnerable range prunes the row, but
        // only on a clean pass.
        let upgraded = agent_packages(instance, &[("openssl", "3.0.14-1")]);
        let outcome = match_feed(
            dir.path(),
            &index_by_name(&upgraded),
            CveSeverity::Medium,
            &cancel,
        )?;
        assert!(store(&realm, instance, outcome.findings.get(&instance), false)?.is_empty());
        assert_eq!(stored(&realm)?.len(), 1, "a dirty pass must not prune");
        assert!(store(&realm, instance, outcome.findings.get(&instance), true)?.is_empty());
        assert!(stored(&realm)?.is_empty());
        Ok(())
    }

    #[test_log::test(tokio::test)]
    async fn overlay_masks_year_file_records() -> Result<()> {
        let instance = InstanceId::new(InstanceType::Agent);
        let dir = tempfile::tempdir()?;
        write_feed(dir.path(), "CVE-2024.json.xz", FEED)?;

        // The overlay rescored CVE-2024-0001 below the threshold; its year-file
        // copy must not resurrect it.
        write_feed(
            dir.path(),
            "CVE-Modified.json.xz",
            r#"{"cve_items": [{
                "id": "CVE-2024-0001",
                "descriptions": [{"lang": "en", "value": "Rescored."}],
                "metrics": {"cvssMetricV31": [{"cvssData": {"baseScore": 2.0, "baseSeverity": "LOW"}}]},
                "configurations": [{"nodes": [{"cpeMatch": [{
                    "vulnerable": true,
                    "criteria": "cpe:2.3:a:openssl:openssl:*:*:*:*:*:*:*:*",
                    "versionEndExcluding": "3.0.14"
                }]}]}]
            }]}"#,
        )?;

        let packages = agent_packages(instance, &[("openssl", "3.0.13-1")]);
        let outcome = match_feed(
            dir.path(),
            &index_by_name(&packages),
            CveSeverity::Medium,
            &CancellationToken::new(),
        )?;
        assert_eq!(outcome.parsed_files, 2);
        assert!(outcome.findings.is_empty());
        Ok(())
    }

    /// Downloads the real (small) recent feed and parses it, which is what
    /// validates the lean structs against the live schema. Run with
    /// `cargo test -p sandpolis-inventory --features server -- --ignored
    /// --nocapture parses_the_live_feed`.
    #[tokio::test]
    #[ignore = "requires network access"]
    async fn parses_the_live_feed() -> Result<()> {
        let config = crate::config::CveConfig::default();
        let url = format!("{}/CVE-Recent.json.xz", config.feed_url);
        let body = reqwest::get(&url)
            .await?
            .error_for_status()?
            .bytes()
            .await?;

        let dir = tempfile::tempdir()?;
        fs::write(dir.path().join("CVE-Recent.json.xz"), &body)?;
        let feed = parse_file(&dir.path().join("CVE-Recent.json.xz"))?;
        println!("{} records in the recent feed", feed.cve_items.len());
        assert!(!feed.cve_items.is_empty());
        assert!(
            feed.cve_items
                .iter()
                .all(|record| record.id.starts_with("CVE-"))
        );
        assert!(
            feed.cve_items
                .iter()
                .any(|record| record.severity().is_some())
        );
        assert!(
            feed.cve_items
                .iter()
                .any(|record| !record.configurations.is_empty())
        );
        Ok(())
    }
}
