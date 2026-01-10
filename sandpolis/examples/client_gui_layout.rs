/// Layout tuning example demonstrating:
/// - Force-directed layout with physics
/// - Interactive parameter tuning with egui sliders
/// - Node dragging
/// - Layout stabilization detection
/// - Multiple nodes with network edges
use anyhow::Result;
use sandpolis::{InstanceState, Layer, MODELS, config::Configuration};
use sandpolis_core::InstanceId;
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

    // Populate test data - create multiple instances
    populate_test_instances(&state).await?;

    // Run the GUI
    sandpolis::client::gui::main(config, state).await
}

/// Populate the database with test instances for layout demonstration
async fn populate_test_instances(state: &InstanceState) -> Result<()> {
    // TODO: Once database query implementation is complete, create test instances here
    // For now, the local instance will be the only node
    //
    // Example of what this should do:
    // - Create 15-20 test instances with different OS types
    // - Create network connections between some nodes (for spring forces)
    // - Store in database so query_all_instances() returns them

    Ok(())
}
