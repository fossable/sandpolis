use serde::Deserialize;
use serde::Serialize;

#[derive(Serialize, Deserialize, Debug, Clone, Default)]
#[serde(default)]
pub struct AccountLayerConfig {
    /// Accounts declared on disk. The server imports any that aren't in the
    /// database yet at startup, and rewrites this list whenever the account set
    /// changes.
    pub accounts: Vec<AccountConfig>,

    /// Periodic tasks that scrape data from the Internet.
    pub scrape: ScrapeConfig,
}

/// An account as it appears in the config file.
///
/// The database holds more than this (an account id, a value, an associated
/// instance); what's here is the identity a person would actually write down.
#[derive(Serialize, Deserialize, Debug, Clone, Default, PartialEq, Eq)]
#[serde(default)]
pub struct AccountConfig {
    /// The service domain, for example "github.com".
    pub domain: String,

    pub username: Option<String>,

    pub email: Option<String>,
}

/// Settings shared by every scraping task.
#[derive(Serialize, Deserialize, Debug, Clone)]
#[serde(default)]
pub struct ScrapeConfig {
    /// Whether any scraping runs at all. Scraping reaches out to third-party
    /// sites, so it's worth being able to switch off wholesale.
    pub enabled: bool,

    /// Sent as `User-Agent` on every request so site operators can identify
    /// (and if they wish, block) the traffic.
    pub user_agent: String,

    /// Per-request timeout in seconds.
    pub request_timeout: u64,

    /// Responses larger than this are rejected. Guards against a hostile or
    /// broken site filling the database.
    pub max_response_bytes: usize,

    /// Ceiling on outbound requests in flight across all tasks.
    pub max_concurrent_requests: usize,

    pub favicon: FaviconConfig,
}

impl Default for ScrapeConfig {
    fn default() -> Self {
        Self {
            enabled: true,
            user_agent: concat!(
                "sandpolis/",
                env!("CARGO_PKG_VERSION"),
                " (+https://github.com/fossable/sandpolis)"
            )
            .to_string(),
            request_timeout: 10,
            max_response_bytes: 512 * 1024,
            max_concurrent_requests: 4,
            favicon: FaviconConfig::default(),
        }
    }
}

/// Settings for the favicon task.
#[derive(Serialize, Deserialize, Debug, Clone)]
#[serde(default)]
pub struct FaviconConfig {
    pub enabled: bool,

    /// Seconds between sweeps over the account domains. A sweep only fetches
    /// domains whose stored icon has gone stale, so this can be frequent
    /// without generating much traffic.
    pub interval: u64,

    /// Seconds a stored favicon stays fresh before it's fetched again.
    pub refresh_after: u64,
}

impl Default for FaviconConfig {
    fn default() -> Self {
        Self {
            enabled: true,
            interval: 60 * 60,
            refresh_after: 7 * 24 * 60 * 60,
        }
    }
}
