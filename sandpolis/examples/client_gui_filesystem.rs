/// Filesystem layer example demonstrating:
/// - Node panels with a filesystem usage gauge
/// - Color-coded nodes based on disk usage
/// - Activity lines for file transfers
/// - OS-specific node icons
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
    // - Multiple instances with different disk usage levels
    // - Simulated file transfers between instances
    // - Different OS types for varied icons

    // Run the GUI (will start on Desktop layer, press F to switch to Filesystem)
    sandpolis::client::gui::main(options, state).await
}
