/// Filesystem layer example demonstrating:
/// - NodePreview windows with filesystem usage stats
/// - Color-coded nodes based on disk usage
/// - Activity lines for file transfers
/// - OS-specific node icons
use anyhow::Result;
use sandpolis::{InstanceState, MODELS, config::Configuration};
use sandpolis_instance::database::{DatabaseAccess, DatabaseLayer, config::DatabaseConfig};
use sandpolis_server::ServerStratum;

#[tokio::main]
async fn main() -> Result<()> {
    // Create minimal configuration for testing
    let config = Configuration::default();

    // Create in-memory database for testing
    let db_config = DatabaseConfig {
        storage: None,
        ephemeral: true,
        key: Default::default(),
    };
    let database = DatabaseLayer::new(db_config, &*MODELS, DatabaseAccess::ReadWrite)?;

    // Create instance state
    let state = InstanceState::new(config.clone(), database, ServerStratum::Global).await?;

    // TODO: Populate test data with:
    // - Multiple instances with different disk usage levels
    // - Simulated file transfers between instances
    // - Different OS types for varied icons

    // Run the GUI (will start on Desktop layer, press F to switch to Filesystem)
    sandpolis::client::gui::main(config, state).await
}
