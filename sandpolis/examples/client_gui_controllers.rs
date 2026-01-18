/// NodeController example demonstrating:
/// - Double-click on nodes to open layer-specific controllers
/// - File browser for Filesystem layer
/// - Terminal for Shell layer
/// - System info for Inventory layer
/// - Package manager
/// - Desktop viewer for Desktop layer
/// - Controller windows centered on screen
/// - Close controllers when switching layers
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
    // - Multiple instances to demonstrate controllers
    // - Filesystem data (directories, files) for file browser
    // - Shell sessions for terminal
    // - Hardware info for system info
    // - Installed packages for package manager

    // Run the GUI
    // Instructions:
    // - Double-click any node to open its controller
    // - Switch layers to see different controllers (F key to cycle)
    // - Controller opens based on current layer:
    //   * Filesystem layer -> File Browser
    //   * Shell layer -> Terminal
    //   * Inventory layer -> System Info
    //   * Desktop layer -> Desktop Viewer
    // - Click Close button to dismiss controller
    sandpolis::client::gui::main(config, state).await
}
