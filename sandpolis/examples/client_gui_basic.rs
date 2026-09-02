/// Basic GUI example demonstrating:
/// - Node spawning from database with multiple nodes
/// - Minimap rendering
/// - Layer indicator
/// - Camera controls (pan, zoom)
/// - Layer switching (F/P/D keys)
/// - Force-directed graph layout
use anyhow::Result;
use sandpolis::{InstanceState, MODELS, RuntimeOptions};
use sandpolis_instance::database::{DatabaseManager, WriteAuthority, config::DatabaseConfig};
use sandpolis_instance::network::ConnectionData;
use sandpolis_instance::realm::RealmName;
use sandpolis_instance::realm::RealmManager;
use sandpolis_instance::{AgentId, ServerId};
use sandpolis_server::ServerStratum;

#[tokio::main]
async fn main() -> Result<()> {
    // Create minimal configuration for testing
    let options = RuntimeOptions::embedded();

    // Create in-memory database for testing
    let db_config = DatabaseConfig {
        storage: None,
        key: Default::default(),
    };
    let database = DatabaseManager::new(db_config, &*MODELS, WriteAuthority::Full)?;

    // Create instance state
    // The local instance will be spawned automatically
    let realms = RealmManager::for_endpoint(Vec::new(), database.clone())?;
    let state =
        InstanceState::new(&options, database.clone(), realms, ServerStratum::Global).await?;

    // Populate the database with test nodes to demonstrate the GUI
    // This creates several agent and server connections that will appear in the world view
    {
        let db = database.realm(RealmName::default())?;
        let rw = db.write(sandpolis_instance::database::DataScope::Instance(
            state.instance.instance_id,
        ))?;

        // Create several test agent connections
        for i in 1..=5 {
            rw.insert(ConnectionData {
                remote_instance: Some(AgentId::random().into()),
                read_bytes: (i * 1024) as u64,
                write_bytes: (i * 512) as u64,
                read_throughput: (i * 100) as u64,
                write_throughput: (i * 50) as u64,
                ..ConnectionData::scoped(state.instance.instance_id)
            })?;
        }

        // Create a couple of server connections
        for i in 1..=2 {
            rw.insert(ConnectionData {
                remote_instance: Some(ServerId::random().into()),
                read_bytes: (i * 2048) as u64,
                write_bytes: (i * 1024) as u64,
                read_throughput: (i * 200) as u64,
                write_throughput: (i * 100) as u64,
                ..ConnectionData::scoped(state.instance.instance_id)
            })?;
        }

        rw.commit()?;
    }

    // Run the GUI - you should see 8 nodes total:
    // - 1 local instance (automatically created)
    // - 5 agent instances
    // - 2 server instances
    sandpolis::client::gui::main(options, state).await
}
