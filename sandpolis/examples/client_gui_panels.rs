/// Node panel example demonstrating:
/// - A collapsed panel under every node, showing the node's identity plus the
///   active layer's summary
/// - Zoom-driven verbosity: the collapsed panel says more the further in you are
/// - Selecting a single node expands its panel in place to the layer's full
///   detail view, starting any stream that view exists to show
/// - Pinning an expanded panel so it survives deselection
/// - Per-layer panels: file browser, terminal, system info, desktop viewer,
///   probe devices
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
    // - Multiple instances to demonstrate panels
    // - Filesystem data (directories, files) for file browser
    // - Shell sessions for terminal
    // - Hardware info for system info
    // - Installed packages for package manager
    // - Registered probes for probe manager

    // Run the GUI
    // Instructions:
    // - Every visible node carries a collapsed panel beneath it
    // - Scroll to zoom; the collapsed panel moves through three verbosity levels
    // - Click a node to expand its panel; the content depends on the active layer:
    //   * Filesystem layer -> File Browser
    //   * Shell layer -> Terminal, opened immediately
    //   * Inventory layer -> CPU / memory / swap / storage gauges
    //   * Desktop layer -> Desktop stream, started immediately
    //   * Probe layer -> the device's protocol tabs (register SSH, RDP, IPMI, ...)
    // - Ctrl-click a second node to collapse both again
    // - "Pin" keeps a panel expanded after its node is deselected; ✕ closes it
    // - Press P to hide every unpinned panel
    sandpolis::client::gui::main(options, state).await
}
