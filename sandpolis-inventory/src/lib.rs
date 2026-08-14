use anyhow::Result;
use native_db::ToKey;
use native_model::Model;
use sandpolis_instance::InstanceLayer;
use sandpolis_instance::database::{DatabaseLayer, Resident};
use sandpolis_instance::realm::RealmName;
use sandpolis_macros::data;
#[cfg(feature = "agent")]
use std::sync::Arc;

#[cfg(feature = "client")]
pub mod client;

pub mod applications;
pub mod hardware;
pub mod os;
pub mod package;

#[data]
#[derive(Default)]
pub struct InventoryLayerData {}

#[cfg(feature = "agent")]
use tokio::sync::Mutex;

#[derive(Clone)]
pub struct InventoryLayer {
    #[allow(dead_code)]
    data: Resident<InventoryLayerData>,
    #[cfg(feature = "agent")]
    pub memory: Arc<Mutex<os::memory::agent::MemoryMonitor>>,
    #[cfg(feature = "agent")]
    pub cpu: Arc<Mutex<hardware::cpu::agent::CpuCollector>>,
    #[cfg(feature = "agent")]
    pub mountpoints: Arc<Mutex<os::mountpoint::agent::MountpointCollector>>,
    #[cfg(feature = "agent")]
    pub users: Arc<Mutex<os::user::agent::UserCollector>>,
    #[cfg(feature = "agent")]
    pub packages: Arc<Mutex<package::agent::PackageCollector>>,
}

impl InventoryLayer {
    #[cfg_attr(not(feature = "agent"), allow(unused_variables))]
    pub async fn new(database: DatabaseLayer, instance: InstanceLayer) -> Result<Self> {
        Ok(Self {
            #[cfg(feature = "agent")]
            memory: Arc::new(Mutex::new(os::memory::agent::MemoryMonitor::new(
                database.realm(RealmName::default())?,
                instance.instance_id,
            )?)),
            #[cfg(feature = "agent")]
            cpu: Arc::new(Mutex::new(hardware::cpu::agent::CpuCollector::new(
                database.realm(RealmName::default())?,
                instance.instance_id,
            )?)),
            #[cfg(feature = "agent")]
            mountpoints: Arc::new(Mutex::new(
                os::mountpoint::agent::MountpointCollector::new(
                    database.realm(RealmName::default())?,
                    instance.instance_id,
                )?,
            )),
            #[cfg(feature = "agent")]
            users: Arc::new(Mutex::new(os::user::agent::UserCollector::new(
                database.realm(RealmName::default())?,
                instance.instance_id,
            )?)),
            #[cfg(feature = "agent")]
            packages: Arc::new(Mutex::new(package::agent::PackageCollector::new(
                database.realm(RealmName::default())?,
                instance.instance_id,
            )?)),
            data: database.realm(RealmName::default())?.resident(())?,
        })
    }

    /// Add the layer's background services to the agent's runner.
    ///
    /// Packages get their own longer interval because enumerating them is
    /// expensive and they change rarely, unlike memory and users. CPU is the
    /// other way around: utilization averaged over half a minute says nothing,
    /// so it polls fast enough to still be a live reading.
    #[cfg(feature = "agent")]
    pub fn register_services(&self, runner: &mut sandpolis_instance::service::ServiceRunner) {
        use sandpolis_agent::CollectorService;
        use std::time::Duration;

        runner.register(CollectorService::new(
            self.memory.clone(),
            "Inventory",
            "memory",
            "Collects the host's memory usage",
            Duration::from_secs(30),
        ));
        runner.register(CollectorService::new(
            self.cpu.clone(),
            "Inventory",
            "cpu",
            "Collects the host's per-core CPU utilization",
            Duration::from_secs(5),
        ));
        runner.register(CollectorService::new(
            self.mountpoints.clone(),
            "Inventory",
            "mountpoints",
            "Collects the host's mounted filesystems and their capacity",
            Duration::from_secs(60),
        ));
        runner.register(CollectorService::new(
            self.users.clone(),
            "Inventory",
            "users",
            "Collects the host's user accounts",
            Duration::from_secs(30),
        ));
        runner.register(CollectorService::new(
            self.packages.clone(),
            "Inventory",
            "packages",
            "Collects the host's installed packages",
            Duration::from_secs(300),
        ));
    }
}
