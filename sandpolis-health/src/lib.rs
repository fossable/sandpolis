use anyhow::Result;
use native_db::ToKey;
use native_model::Model;
use sandpolis_instance::InstanceId;
use sandpolis_instance::InstanceLayer;
use sandpolis_instance::database::{DatabaseLayer, Resident};
use sandpolis_instance::realm::RealmName;
use sandpolis_macros::data;

#[cfg(feature = "agent")]
use std::sync::Arc;

#[cfg(feature = "client")]
pub mod client;

pub mod systemd;

#[data]
#[derive(Default)]
pub struct HealthLayerData {}

/// The health layer tracks the operational status of services and the host's
/// overall well-being (currently: systemd units).
#[derive(Clone)]
pub struct HealthLayer {
    #[allow(dead_code)]
    data: Resident<HealthLayerData>,
    #[allow(dead_code)]
    pub instance_id: InstanceId,

    /// Agent-side systemd collector.
    #[cfg(feature = "agent")]
    pub systemd: Arc<tokio::sync::Mutex<systemd::agent::SystemdCollector>>,
}

impl HealthLayer {
    pub async fn new(database: DatabaseLayer, instance: InstanceLayer) -> Result<Self> {
        Ok(Self {
            #[cfg(feature = "agent")]
            systemd: Arc::new(tokio::sync::Mutex::new(
                systemd::agent::SystemdCollector::new(
                    database.realm(RealmName::default())?,
                    instance.instance_id,
                )?,
            )),
            instance_id: instance.instance_id,
            data: database.realm(RealmName::default())?.resident(())?,
        })
    }
}
