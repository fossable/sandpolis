//! Known vulnerabilities in installed packages.
//!
//! The server periodically downloads a public CVE feed and matches it against
//! the packages agents have reported. Matches become [`VulnerabilityData`] rows
//! scoped to the affected agent, so they replicate to clients exactly like the
//! rest of that agent's inventory.

use native_db::ToKey;
use native_model::Model;
use sandpolis_instance::InstanceId;
use sandpolis_macros::data;
use serde::{Deserialize, Serialize};
use strum::Display;

#[cfg(feature = "server")]
pub mod server;

#[cfg(feature = "server")]
pub use server::CveService;

/// CVSS base severity, ordered so a threshold can be a simple comparison.
#[derive(
    Serialize, Deserialize, Clone, Copy, Debug, Default, PartialEq, Eq, PartialOrd, Ord, Display,
)]
pub enum CveSeverity {
    #[default]
    Low,
    Medium,
    High,
    Critical,
}

impl CveSeverity {
    /// The severity of an NVD record, preferring CVSS v3.1 over v3.0 over v2.
    ///
    /// CVSS v2 has no severity strings, only a score, so it's bucketed by the
    /// conventional ranges. `None` when the record carries no metrics at all
    /// (typically one still awaiting analysis).
    pub fn from_nvd(
        v31: Option<&str>,
        v30: Option<&str>,
        v2_score: Option<f64>,
    ) -> Option<CveSeverity> {
        if let Some(severity) = v31.or(v30) {
            return match severity.to_ascii_uppercase().as_str() {
                "CRITICAL" => Some(Self::Critical),
                "HIGH" => Some(Self::High),
                "MEDIUM" => Some(Self::Medium),
                _ => Some(Self::Low),
            };
        }
        v2_score.map(|score| {
            if score >= 7.0 {
                Self::High
            } else if score >= 4.0 {
                Self::Medium
            } else {
                Self::Low
            }
        })
    }
}

/// A CVE that matched a package installed on an agent.
///
/// Authored by the server that owns the agent, never by the agent itself; the
/// agent's `_instance_id` scopes the row so it reaches clients through the same
/// subscription as the rest of the agent's inventory. The `(instance, cve_id,
/// package)` key is what makes alerts fire at most once: a scan only counts a
/// finding as new when no row with its key exists.
#[data]
#[derive(Default)]
pub struct VulnerabilityData {
    #[secondary_key]
    pub _instance_id: InstanceId,

    /// The CVE identifier, for example "CVE-2024-3094".
    #[secondary_key]
    pub cve_id: String,

    /// The installed package that matched, as the agent reported its name.
    pub package: String,

    /// The installed version that matched.
    pub version: String,

    pub severity: CveSeverity,

    /// CVSS base score, when the record carried one.
    pub score: Option<f64>,

    /// The record's English description, truncated.
    pub summary: Option<String>,

    /// When the CVE was published, as the feed's RFC 3339 timestamp.
    pub published: Option<String>,
}

inventory::submit! {
    sandpolis_instance::database::sync::SyncRegistration(|r| {
        r.register_scoped::<VulnerabilityData>(|d| d._instance_id)
    })
}

/// The sync `model_id` for [`VulnerabilityData`], used by clients to subscribe.
pub fn vulnerability_model_id() -> u32 {
    <VulnerabilityData as Model>::native_model_id()
}
