/// NodeController example demonstrating:
/// - Double-click on nodes to open layer-specific controllers
/// - File browser for Filesystem layer
/// - Terminal for Shell layer
/// - System info for Inventory layer
/// - Package manager
/// - Desktop viewer for Desktop layer
/// - Probe manager for Probe layer (NEW)
/// - Controller windows centered on screen
/// - Close controllers when switching layers
use anyhow::Result;
use sandpolis::{InstanceState, MODELS, config::Configuration};
use sandpolis_instance::database::{DatabaseLayer, config::DatabaseConfig};

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
    let database = DatabaseLayer::new(db_config, &*MODELS)?;

    // Create instance state
    let state = InstanceState::new(config.clone(), database).await?;

    // TODO: Populate test data with:
    // - Multiple instances to demonstrate controllers
    // - Filesystem data (directories, files) for file browser
    // - Shell sessions for terminal
    // - Hardware info for system info
    // - Installed packages for package manager
    // - Registered probes for probe manager

    // Run the GUI
    // Instructions:
    // - Double-click any node to open its controller
    // - Switch layers to see different controllers (F key to cycle)
    // - Controller opens based on current layer:
    //   * Filesystem layer -> File Browser
    //   * Shell layer -> Terminal
    //   * Inventory layer -> System Info
    //   * Desktop layer -> Desktop Viewer
    //   * Probe layer -> Probe Manager (register SSH, RDP, IPMI, Docker, etc.)
    // - Click Close button to dismiss controller
    // - Probe nodes appear as smaller nodes attached to their gateway
    sandpolis::client::gui::main(config, state).await
}
