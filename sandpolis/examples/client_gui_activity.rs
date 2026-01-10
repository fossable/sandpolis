/// Activity lines example demonstrating:
/// - File transfer activity lines (green dots moving along edges)
/// - Network traffic activity lines (blue dots)
/// - Different speeds and colors for different activity types
/// - Automatic spawning and despawning based on database events
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
    // - Multiple instances connected in a network
    // - Active file transfers for Filesystem layer
    // - Network connections for Network layer activity
    // - Simulated ongoing transfers to show animations

    // Run the GUI
    // Switch to Filesystem layer (F key) to see file transfer activity lines
    // Switch to Network layer to see network traffic activity lines
    sandpolis::client::gui::main(config, state).await
}
