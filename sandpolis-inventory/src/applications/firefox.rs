use native_db::ToKey;
use native_model::Model;
use sandpolis_macros::data;

/// A Firefox addon installed for a user.
#[data(instance)]
pub struct FirefoxAddonData {
    /// The local user that owns the addon
    pub uid: String,
    /// Addon display name
    pub name: String,
    /// Addon identifier
    pub identifier: String,
    /// Addon-supported creator string
    pub creator: String,
    /// Extension, addon, webapp
    pub r#type: String,
    /// Addon-supplied version string
    pub version: String,
    /// Addon-supplied description string
    pub description: String,
    /// URL that installed the addon
    pub source_url: String,
    /// Whether the addon is shown in the browser
    pub visible: bool,
    /// Whether the addon is active
    pub active: bool,
    /// Whether the addon is disabled
    pub disabled: bool,
    /// Whether the addon applies background updates
    pub autoupdate: bool,
    /// Whether the addon includes binary components
    pub native_addon: bool,
    /// Global, profile location
    pub location: String,
    /// Path to plugin bundle
    pub path: String,
}
