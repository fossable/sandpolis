/// Activity lines example demonstrating:
/// - File transfer activity lines (green dots moving along edges)
/// - Network traffic activity lines (blue dots)
/// - Different speeds and colors for different activity types
/// - Automatic spawning and despawning based on database events
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
    let realms = Realms::for_endpoint(Vec::new(), database.clone())?;
    let state = InstanceState::new(&options, database, realms, ServerStratum::Global).await?;

    // TODO: Populate test data with:
    // - Multiple instances connected in a network
    // - Active file transfers for Filesystem layer
    // - Network connections for Network layer activity
    // - Simulated ongoing transfers to show animations

    // Run the GUI
    // Switch to Filesystem layer (F key) to see file transfer activity lines
    // Switch to Network layer to see network traffic activity lines
    sandpolis::client::gui::main(options, state).await
}
