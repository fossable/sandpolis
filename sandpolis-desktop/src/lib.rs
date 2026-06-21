use anyhow::Result;
use native_db::*;
use native_model::Model;
use sandpolis_instance::InstanceId;
use sandpolis_instance::database::DatabaseLayer;
use sandpolis_macros::data;

pub mod screenshot;
pub mod session;

#[cfg(feature = "client")]
pub mod client;

#[cfg(all(feature = "client", not(target_os = "android")))]
pub mod cli;

#[cfg(feature = "agent")]
mod agent;

/// A capturable desktop (display) discovered on an agent.
#[data]
#[derive(Default)]
pub struct DesktopData {
    #[secondary_key]
    pub _instance_id: InstanceId,

    /// Stable name used to identify the display when streaming
    #[secondary_key]
    pub name: String,

    /// Display width in pixels
    pub width: u32,

    /// Display height in pixels
    pub height: u32,

    /// Whether this is the primary display
    pub primary: bool,

    /// Display scale factor
    pub scale_factor: f64,
}

#[derive(Clone)]
pub struct DesktopLayer {
    #[allow(dead_code)]
    database: DatabaseLayer,
    #[cfg(feature = "agent")]
    pub displays: std::sync::Arc<agent::DesktopDisplayCollector>,
}

impl DesktopLayer {
    pub async fn new(database: DatabaseLayer) -> Result<Self> {
        #[cfg(feature = "agent")]
        let displays = {
            use sandpolis_agent::Collector;
            use sandpolis_instance::realm::RealmName;

            let mut collector =
                agent::DesktopDisplayCollector::new(database.realm(RealmName::default())?)?;
            // Enumerate displays once at startup; periodic refresh follows the
            // broader (incomplete) collector scheduling convention.
            if let Err(e) = collector.refresh().await {
                tracing::warn!(error = %e, "Failed to enumerate desktops");
            }
            std::sync::Arc::new(collector)
        };

        Ok(Self {
            database,
            #[cfg(feature = "agent")]
            displays,
        })
    }
}

/// Static handler for registering desktop stream responders.
#[cfg(feature = "agent")]
pub struct DesktopResponderRegistration;

#[cfg(feature = "agent")]
impl sandpolis_instance::network::RegisterResponders for DesktopResponderRegistration {
    fn register_responders(&self, registry: &sandpolis_instance::network::StreamRegistry) {
        registry.register_responder(session::DesktopStreamResponder::default);
        registry.register_responder(screenshot::DesktopScreenshotResponder::default);
    }
}

#[cfg(feature = "agent")]
inventory::submit!(sandpolis_instance::network::ResponderRegistration(
    &DesktopResponderRegistration
));
