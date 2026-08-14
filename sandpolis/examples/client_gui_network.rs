/// Network layer example demonstrating:
/// - Network topology edges between nodes
/// - Edge labels showing latency and throughput
/// - Color-coded nodes based on connection quality
/// - Network activity lines showing traffic flow
/// - Server/agent icon differentiation
use anyhow::Result;
use sandpolis::{InstanceState, MODELS, RuntimeOptions};
use sandpolis_instance::database::{DatabaseLayer, WriteAuthority, config::DatabaseConfig};
use sandpolis_instance::realm::Realms;
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
    let database = DatabaseLayer::new(db_config, &*MODELS, WriteAuthority::Full)?;

    // Create instance state
    let realms = Realms::for_client(Vec::new(), database.clone())?;
    let state = InstanceState::new(&options, database, realms, ServerStratum::Global).await?;

    // TODO: Populate test data with:
    // - Network topology (server connected to multiple agents)
    // - Latency and throughput stats for each connection
    // - Varying connection qualities (green/yellow/red nodes)

    // Run the GUI (will start on Desktop layer, press to cycle to Network layer)
    sandpolis::client::gui::main(options, state).await
}
