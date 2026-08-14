//! Favicons for account domains.
//!
//! The account layer's first scraping service: for every distinct domain across
//! all accounts, fetch the site's favicon and store it so clients can show
//! accounts with the branding of the service they belong to.

use native_db::*;
use native_model::Model;
use sandpolis_macros::data;

/// A domain's favicon, or the reason it couldn't be fetched.
///
/// A failed fetch still writes a row (with empty `bytes` and an `error`) so the
/// staleness check doesn't retry an unreachable domain on every sweep.
#[data]
#[derive(Default)]
pub struct FaviconData {
    #[secondary_key(unique)]
    pub domain: String,

    /// The raw image, empty when the last attempt failed.
    pub bytes: Vec<u8>,

    /// The `Content-Type` the server reported.
    pub content_type: Option<String>,

    /// Where the icon was found, which is often not `/favicon.ico`.
    pub source_url: Option<String>,

    /// When the last attempt happened, in milliseconds since the Unix epoch.
    pub fetched_at: i64,

    /// Why the last attempt failed. `None` means `bytes` is good.
    pub error: Option<String>,
}

inventory::submit! {
    sandpolis_instance::database::sync::SyncRegistration(|r| r.register::<FaviconData>())
}

#[cfg(feature = "server")]
mod server {
    use super::*;
    use crate::AccountData;
    #[cfg(test)]
    use crate::config::FaviconConfig;
    use crate::config::ScrapeConfig;
    use crate::scrape::{Fetched, HttpFetcher};
    use anyhow::{Result, bail};
    use chrono::Utc;
    use regex::Regex;
    use sandpolis_instance::LayerName;
    use sandpolis_instance::database::{DataScope, RealmDatabase};
    use sandpolis_instance::service::{Service, ServiceReport, ServiceSchedule};
    use std::collections::BTreeSet;
    use std::sync::LazyLock;
    use std::time::Duration;
    use tokio_util::sync::CancellationToken;
    use tracing::debug;
    use url::Url;

    /// Fetches each account domain's favicon.
    pub struct FaviconService {
        realm: RealmDatabase,
        http: HttpFetcher,
        interval: Duration,
        refresh_after: Duration,
    }

    impl FaviconService {
        pub fn new(realm: RealmDatabase, scrape: &ScrapeConfig) -> Result<Self> {
            Ok(Self {
                realm,
                http: HttpFetcher::new(scrape)?,
                interval: Duration::from_secs(scrape.favicon.interval.max(1)),
                refresh_after: Duration::from_secs(scrape.favicon.refresh_after),
            })
        }

        /// The refresh window this service was configured with.
        #[cfg(test)]
        fn for_test(realm: RealmDatabase, config: &FaviconConfig) -> Result<Self> {
            let scrape = ScrapeConfig {
                favicon: config.clone(),
                ..Default::default()
            };
            Self::new(realm, &scrape)
        }
    }

    impl Service for FaviconService {
        fn name(&self) -> &'static str {
            "favicon"
        }

        fn layer(&self) -> LayerName {
            LayerName::from("Account")
        }

        fn description(&self) -> &'static str {
            "Fetches favicons for every account domain"
        }

        fn schedule(&self) -> ServiceSchedule {
            ServiceSchedule::every(self.interval)
        }

        async fn run(&self, cancel: CancellationToken) -> Result<ServiceReport> {
            let stale_before =
                Utc::now().timestamp_millis() - self.refresh_after.as_millis() as i64;

            let mut report = ServiceReport::default();
            for domain in stale_domains(&self.realm, stale_before)? {
                // A sweep can be long and hits third parties the whole way; stop
                // at the next domain rather than the next pass. The staleness
                // check means the next sweep picks up where this one left off.
                if cancel.is_cancelled() {
                    break;
                }
                report.scanned += 1;

                let outcome = fetch_favicon(&self.http, &domain).await;
                if let Err(e) = &outcome {
                    debug!(domain = %domain, error = %e, "Failed to fetch favicon");
                    report.failed += 1;
                } else {
                    report.updated += 1;
                }

                // Store either way: recording the failure is what stops the next
                // sweep from immediately retrying a domain that's simply down.
                store(&self.realm, &domain, outcome)?;
            }
            Ok(report)
        }
    }

    /// Every account domain whose stored favicon is missing or older than
    /// `stale_before`.
    fn stale_domains(realm: &RealmDatabase, stale_before: i64) -> Result<Vec<String>> {
        let r = realm.r_transaction()?;

        let accounts: Vec<AccountData> = r
            .scan()
            .primary::<AccountData>()?
            .all()?
            .collect::<std::result::Result<Vec<_>, _>>()?;
        let favicons: Vec<FaviconData> = r
            .scan()
            .primary::<FaviconData>()?
            .all()?
            .collect::<std::result::Result<Vec<_>, _>>()?;
        drop(r);

        // Accounts on the same service share a favicon, so collapse to the
        // distinct set. `BTreeSet` also makes the sweep order deterministic.
        let domains: BTreeSet<String> = accounts
            .into_iter()
            .map(|a| a.domain.trim().to_lowercase())
            .filter(|d| !d.is_empty())
            .collect();

        Ok(domains
            .into_iter()
            .filter(|domain| {
                favicons
                    .iter()
                    .find(|f| &f.domain == domain)
                    .is_none_or(|f| f.fetched_at < stale_before)
            })
            .collect())
    }

    /// A favicon that was successfully fetched.
    struct Favicon {
        bytes: Vec<u8>,
        content_type: Option<String>,
        source_url: String,
    }

    /// Write the outcome of one domain's fetch, replacing any previous row.
    fn store(realm: &RealmDatabase, domain: &str, outcome: Result<Favicon>) -> Result<()> {
        let rw = realm.write(DataScope::Global)?;

        let existing: Vec<FaviconData> = rw
            .scan()
            .primary::<FaviconData>()?
            .all()?
            .collect::<std::result::Result<Vec<_>, _>>()?;
        let previous = existing.into_iter().find(|f| f.domain == domain);

        let mut row = FaviconData {
            domain: domain.to_string(),
            fetched_at: Utc::now().timestamp_millis(),
            ..Default::default()
        };
        match outcome {
            Ok(favicon) => {
                row.bytes = favicon.bytes;
                row.content_type = favicon.content_type;
                row.source_url = Some(favicon.source_url);
            }
            Err(e) => row.error = Some(e.to_string()),
        }

        match previous {
            Some(previous) => {
                rw.upsert(FaviconData {
                    _id: previous._id,
                    ..row
                })?;
            }
            None => {
                rw.insert(row)?;
            }
        }

        rw.commit()?;
        Ok(())
    }

    /// How many `<link rel="icon">` candidates to try before falling back.
    const MAX_CANDIDATES: usize = 3;

    /// Fetch a domain's favicon, preferring what the homepage declares.
    ///
    /// `/favicon.ico` is only the fallback: most sites now point at a PNG (or
    /// several sizes) from a `<link>` tag, and plenty serve an HTML error page
    /// with a 200 status at the well-known path.
    async fn fetch_favicon(http: &HttpFetcher, domain: &str) -> Result<Favicon> {
        let base = Url::parse(&format!("https://{domain}/"))?;

        if let Ok(page) = http.get(&base).await {
            for href in icon_hrefs(&page.text()).into_iter().take(MAX_CANDIDATES) {
                let Ok(url) = page.url.join(&href) else {
                    continue;
                };
                match http.get(&url).await {
                    Ok(response) => match into_favicon(response) {
                        Ok(favicon) => return Ok(favicon),
                        Err(e) => debug!(url = %url, error = %e, "Declared icon unusable"),
                    },
                    Err(e) => debug!(url = %url, error = %e, "Declared icon unreachable"),
                }
            }
        }

        into_favicon(http.get(&base.join("/favicon.ico")?).await?)
    }

    /// Accept a response as an icon, or explain why it isn't one.
    fn into_favicon(response: Fetched) -> Result<Favicon> {
        if response.body.is_empty() {
            bail!("empty response");
        }

        // Sites routinely answer 200 with an HTML error page. Trust an explicit
        // image content type; otherwise fall back to sniffing, since some
        // servers send `application/octet-stream` for .ico files.
        let is_image = match response.content_type.as_deref() {
            Some(content_type) => {
                let content_type = content_type.to_ascii_lowercase();
                content_type.starts_with("image/")
                    || (content_type.starts_with("application/octet-stream")
                        && !looks_like_markup(&response.body))
            }
            None => !looks_like_markup(&response.body),
        };
        if !is_image {
            bail!(
                "not an image (content type {})",
                response.content_type.as_deref().unwrap_or("unset")
            );
        }

        Ok(Favicon {
            bytes: response.body,
            content_type: response.content_type,
            source_url: response.url.to_string(),
        })
    }

    /// Whether a body looks like HTML/XML rather than binary image data.
    fn looks_like_markup(body: &[u8]) -> bool {
        body.iter()
            .find(|b| !b.is_ascii_whitespace())
            .is_some_and(|b| *b == b'<')
    }

    static LINK_TAG: LazyLock<Regex> =
        LazyLock::new(|| Regex::new(r"(?is)<link\b[^>]*>").expect("valid regex"));

    static ATTRIBUTE: LazyLock<Regex> = LazyLock::new(|| {
        Regex::new(r#"(?is)([a-z][a-z0-9-]*)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'>]+))"#)
            .expect("valid regex")
    });

    /// Extract icon `href`s from a page's `<link>` tags, best candidate first.
    ///
    /// A regex rather than a real parser: we're pulling attributes off one
    /// self-closing tag type out of markup we won't otherwise interpret, which
    /// isn't worth an HTML-parsing dependency.
    pub(super) fn icon_hrefs(html: &str) -> Vec<String> {
        let mut exact = Vec::new();
        let mut other = Vec::new();

        for tag in LINK_TAG.find_iter(html) {
            let mut rel = None;
            let mut href = None;
            for attribute in ATTRIBUTE.captures_iter(tag.as_str()) {
                let name = attribute[1].to_ascii_lowercase();
                let value = attribute
                    .get(2)
                    .or_else(|| attribute.get(3))
                    .or_else(|| attribute.get(4))
                    .map(|m| m.as_str())
                    .unwrap_or_default();
                match name.as_str() {
                    "rel" => rel = Some(value.to_ascii_lowercase()),
                    "href" => href = Some(value.trim().to_string()),
                    _ => {}
                }
            }

            let (Some(rel), Some(href)) = (rel, href) else {
                continue;
            };
            if href.is_empty() {
                continue;
            }

            // `rel` is a space-separated token list: "shortcut icon", "icon",
            // "apple-touch-icon", ...
            let tokens: Vec<&str> = rel.split_whitespace().collect();
            if tokens.contains(&"icon") {
                exact.push(href);
            } else if tokens.iter().any(|t| t.ends_with("icon")) {
                other.push(href);
            }
        }

        // Plain `rel="icon"` beats `apple-touch-icon` and friends, which are
        // platform-specific and often much larger.
        exact.extend(other);
        exact
    }

    #[cfg(test)]
    mod tests {
        use super::*;
        use crate::AccountId;
        use sandpolis_instance::database::DatabaseLayer;
        use sandpolis_instance::realm::RealmName;
        use sandpolis_instance::test_db;

        fn realm() -> Result<RealmDatabase> {
            let db: DatabaseLayer = test_db!(AccountData, FaviconData);
            db.realm(RealmName::default())
        }

        fn add_account(realm: &RealmDatabase, domain: &str) -> Result<()> {
            let rw = realm.write(DataScope::Global)?;
            rw.insert(AccountData {
                account_id: AccountId::default(),
                domain: domain.into(),
                username: Some("someone".into()),
                ..Default::default()
            })?;
            rw.commit()?;
            Ok(())
        }

        fn fetched(content_type: Option<&str>, body: &[u8]) -> Fetched {
            Fetched {
                url: Url::parse("https://example.com/favicon.ico").unwrap(),
                content_type: content_type.map(Into::into),
                body: body.to_vec(),
            }
        }

        #[test]
        fn finds_icon_links_in_preference_order() {
            let html = r#"
                <html><head>
                  <link rel="stylesheet" href="/style.css">
                  <link rel="apple-touch-icon" sizes="180x180" href="/apple.png">
                  <link rel='shortcut icon' href='/favicon-32.png'>
                  <link rel="icon" type="image/svg+xml" href=/icon.svg>
                  <link rel="icon" href="">
                </head></html>
            "#;

            // Both `icon` and `shortcut icon` land in the plain-icon bucket and
            // come before `apple-touch-icon`; the empty href is dropped.
            assert_eq!(
                icon_hrefs(html),
                vec!["/favicon-32.png", "/icon.svg", "/apple.png"]
            );
        }

        #[test]
        fn ignores_pages_without_icons() {
            assert!(
                icon_hrefs("<html><head><link rel=stylesheet href=/a.css></head></html>")
                    .is_empty()
            );
            assert!(icon_hrefs("").is_empty());
        }

        #[test]
        fn accepts_images_and_rejects_error_pages() {
            assert!(into_favicon(fetched(Some("image/png"), b"\x89PNG\r\n")).is_ok());
            // Case and parameters in the content type shouldn't matter.
            assert!(
                into_favicon(fetched(Some("Image/X-Icon; charset=binary"), b"\x00\x00")).is_ok()
            );
            // Some servers serve .ico as a generic binary blob.
            assert!(
                into_favicon(fetched(Some("application/octet-stream"), b"\x00\x00\x01")).is_ok()
            );
            // No content type at all: fall back to sniffing.
            assert!(into_favicon(fetched(None, b"\x00\x00\x01")).is_ok());

            // A 200 response carrying an HTML error page is the common trap.
            assert!(into_favicon(fetched(Some("text/html"), b"<!doctype html>")).is_err());
            assert!(into_favicon(fetched(None, b"  \n<html>")).is_err());
            assert!(into_favicon(fetched(Some("application/octet-stream"), b"<html>")).is_err());
            assert!(into_favicon(fetched(Some("image/png"), b"")).is_err());
        }

        #[test]
        fn sweeps_distinct_domains_that_have_gone_stale() -> Result<()> {
            let realm = realm()?;

            // Two accounts on one service produce one domain to fetch.
            add_account(&realm, "github.com")?;
            add_account(&realm, "GitHub.com")?;
            add_account(&realm, "gitlab.com")?;

            let now = Utc::now().timestamp_millis();
            assert_eq!(
                stale_domains(&realm, now)?,
                vec!["github.com".to_string(), "gitlab.com".to_string()]
            );

            // A fresh icon takes its domain out of the sweep.
            store(
                &realm,
                "github.com",
                Ok(Favicon {
                    bytes: vec![1, 2, 3],
                    content_type: Some("image/png".into()),
                    source_url: "https://github.com/favicon.ico".into(),
                }),
            )?;
            assert_eq!(stale_domains(&realm, now)?, vec!["gitlab.com".to_string()]);

            // A recorded failure also counts as fetched, so an unreachable
            // domain isn't retried on every sweep.
            store(&realm, "gitlab.com", Err(anyhow::anyhow!("unreachable")))?;
            assert!(stale_domains(&realm, now)?.is_empty());

            // Once the stored rows age past the refresh window they come back.
            let later = Utc::now().timestamp_millis() + 1;
            assert_eq!(
                stale_domains(&realm, later)?,
                vec!["github.com".to_string(), "gitlab.com".to_string()]
            );
            Ok(())
        }

        /// Exercises the real fetch path — HTTP, redirects, homepage parsing,
        /// and image acceptance — against live sites. Ignored by default since
        /// it needs network access and depends on third parties staying up.
        ///
        /// Run with `cargo test -p sandpolis-account --features server -- \
        /// --ignored --nocapture fetches_real_favicons`.
        #[tokio::test]
        #[ignore = "requires network access"]
        async fn fetches_real_favicons() -> Result<()> {
            let http = HttpFetcher::new(&ScrapeConfig::default())?;

            for domain in ["github.com", "rust-lang.org", "wikipedia.org"] {
                let favicon = fetch_favicon(&http, domain)
                    .await
                    .unwrap_or_else(|e| panic!("{domain}: {e}"));
                println!(
                    "{domain}: {} bytes, {} from {}",
                    favicon.bytes.len(),
                    favicon.content_type.as_deref().unwrap_or("no content type"),
                    favicon.source_url,
                );
                assert!(!favicon.bytes.is_empty());
                assert!(!looks_like_markup(&favicon.bytes));
            }
            Ok(())
        }

        /// The whole pass: pick stale domains, fetch, store, and skip what's
        /// already fresh. Ignored by default alongside the other network test.
        #[tokio::test]
        #[ignore = "requires network access"]
        async fn full_pass_stores_favicons_and_then_skips_them() -> Result<()> {
            let realm = realm()?;
            add_account(&realm, "github.com")?;
            add_account(&realm, "rust-lang.org")?;

            let service = FaviconService::for_test(realm.clone(), &FaviconConfig::default())?;
            let cancel = CancellationToken::new();

            let first = service.run(cancel.clone()).await?;
            assert_eq!(
                first,
                ServiceReport {
                    scanned: 2,
                    updated: 2,
                    failed: 0
                }
            );

            let r = realm.r_transaction()?;
            let rows: Vec<FaviconData> = r
                .scan()
                .primary::<FaviconData>()?
                .all()?
                .collect::<std::result::Result<Vec<_>, _>>()?;
            drop(r);
            assert_eq!(rows.len(), 2);
            for row in &rows {
                assert!(row.error.is_none(), "{row:#?}");
                assert!(!row.bytes.is_empty(), "{row:#?}");
                assert!(row.source_url.is_some(), "{row:#?}");
            }

            // Everything is fresh now, so the next pass makes no requests.
            let second = service.run(cancel).await?;
            assert_eq!(second, ServiceReport::default());
            Ok(())
        }

        #[test]
        fn storing_a_domain_twice_replaces_its_row() -> Result<()> {
            let realm = realm()?;
            add_account(&realm, "github.com")?;

            store(
                &realm,
                "github.com",
                Ok(Favicon {
                    bytes: vec![1],
                    content_type: Some("image/png".into()),
                    source_url: "https://github.com/a.png".into(),
                }),
            )?;
            store(&realm, "github.com", Err(anyhow::anyhow!("gone")))?;

            let r = realm.r_transaction()?;
            let rows: Vec<FaviconData> = r
                .scan()
                .primary::<FaviconData>()?
                .all()?
                .collect::<std::result::Result<Vec<_>, _>>()?;
            assert_eq!(rows.len(), 1, "{rows:#?}");
            assert_eq!(rows[0].error.as_deref(), Some("gone"));
            assert!(rows[0].bytes.is_empty());
            Ok(())
        }
    }
}

#[cfg(feature = "server")]
pub use server::FaviconService;
