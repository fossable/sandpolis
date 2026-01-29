/// Basic GUI example demonstrating:
/// - Node spawning from database with multiple nodes
/// - Minimap rendering
/// - Layer indicator
/// - Camera controls (pan, zoom)
/// - Layer switching (F/P/D keys)
/// - Force-directed graph layout
use anyhow::Result;
use sandpolis::{InstanceState, MODELS, config::Configuration};
use sandpolis_instance::database::{DatabaseLayer, config::DatabaseConfig};
use sandpolis_instance::network::ConnectionData;
use sandpolis_instance::realm::RealmName;
use sandpolis_instance::{InstanceId, InstanceType};

#[tokio::main]
async fn main() -> Result<()> {
    // Create minimal configuration for testing
    let config = Configuration::default();

    // Create in-memory database for testing
    let db_config = DatabaseConfig {
        storage: None,
        ephemeral: true,
    };
    let database = DatabaseLayer::new(db_config, &*MODELS)?;

    // Create instance state
    // The local instance will be spawned automatically
    let state = InstanceState::new(config.clone(), database.clone()).await?;

    // Populate the database with test nodes to demonstrate the GUI
    // This creates several agent and server connections that will appear in the world view
    {
        let db = database.realm(RealmName::default())?;
        let rw = db.rw_transaction()?;

        // Create several test agent connections
        for i in 1..=5 {
            rw.insert(ConnectionData {
                _instance_id: state.instance.instance_id,
                remote_instance: InstanceId::new(&[InstanceType::Agent]),
                read_bytes: (i * 1024) as u64,
                write_bytes: (i * 512) as u64,
                read_throughput: (i * 100) as u64,
                write_throughput: (i * 50) as u64,
                ..Default::default()
            })?;
        }

        // Create a couple of server connections
        for i in 1..=2 {
            rw.insert(ConnectionData {
                _instance_id: state.instance.instance_id,
                remote_instance: InstanceId::new(&[InstanceType::Server]),
                read_bytes: (i * 2048) as u64,
                write_bytes: (i * 1024) as u64,
                read_throughput: (i * 200) as u64,
                write_throughput: (i * 100) as u64,
                ..Default::default()
            })?;
        }

        rw.commit()?;
    }

    // Run the GUI - you should see 8 nodes total:
    // - 1 local instance (automatically created)
    // - 5 agent instances
    // - 2 server instances
    sandpolis::client::gui::main(config, state).await
}
