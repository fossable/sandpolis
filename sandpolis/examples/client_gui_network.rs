/// Network layer example demonstrating:
/// - Network topology edges between nodes
/// - Edge labels showing latency and throughput
/// - Color-coded nodes based on connection quality
/// - Network activity lines showing traffic flow
/// - Server/agent icon differentiation
use anyhow::Result;
use sandpolis::{InstanceState, MODELS, config::Configuration};
use sandpolis_database::{DatabaseLayer, config::DatabaseConfig};

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
    let state = InstanceState::new(config.clone(), database).await?;

    // TODO: Populate test data with:
    // - Network topology (server connected to multiple agents)
    // - Latency and throughput stats for each connection
    // - Varying connection qualities (green/yellow/red nodes)

    // Run the GUI (will start on Desktop layer, press to cycle to Network layer)
    sandpolis::client::gui::main(config, state).await
}
