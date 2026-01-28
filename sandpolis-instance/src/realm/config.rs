use sandpolis_instance::realm::RealmName;
use sandpolis_instance::realm::cli::RealmCommandLine;
use sandpolis_instance::LayerConfig;
use serde::{Deserialize, Serialize};
use std::path::PathBuf;

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct RealmConfig {
    /// Path to realm certificate which will be installed into the
    /// database.
    #[cfg(feature = "agent")]
    pub agent_certs: Option<Vec<PathBuf>>,

    /// Path to realm certificate which will be installed into the
    /// database.
    #[cfg(feature = "client")]
    pub client_certs: Option<Vec<PathBuf>>,

    /// Force the following realms to exist
    pub static_realms: Option<Vec<RealmName>>,

    /// Whether new realms can be created
    pub lock_realms: Option<bool>,
}

impl LayerConfig<RealmCommandLine> for RealmConfig {
    fn override_cli(&mut self, args: &RealmCommandLine) {
        #[cfg(feature = "agent")]
        if let Some(agent_cert) = &args.agent_cert {
            let mut agent_certs = self.agent_certs.clone().unwrap_or_default();
            agent_certs.push(agent_cert.clone());
            self.agent_certs = Some(agent_certs);
        }

        #[cfg(feature = "client")]
        if let Some(client_cert) = &args.client_cert {
            let mut client_certs = self.client_certs.clone().unwrap_or_default();
            client_certs.push(client_cert.clone());
            self.client_certs = Some(client_certs);
        }
    }
}
