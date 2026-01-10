/// Basic GUI example demonstrating:
/// - Node spawning from database
/// - Minimap rendering
/// - Layer indicator
/// - Camera controls (pan, zoom)
/// - Layer switching (F/P/D keys)
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
    // The local instance will be spawned automatically
    let state = InstanceState::new(config.clone(), database).await?;

    // Run the GUI
    sandpolis::client::gui::main(config, state).await
}
