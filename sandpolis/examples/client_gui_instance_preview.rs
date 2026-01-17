/// Isolated test harness for the node preview window egui component.
/// This example tests the preview rendering without starting Bevy.
///
/// Environment variables (all optional):
/// - LAYER: Layer to display (Network, Filesystem, Inventory, Shell, Desktop) [default: Network]
///
/// Usage: cargo run --example client_gui_instance_preview --features client-gui
///
/// Examples:
///   LAYER=Filesystem cargo run --example client_gui_instance_preview --features client-gui
///   LAYER=Shell cargo run --example client_gui_instance_preview --features client-gui
///   LAYER=Inventory cargo run --example client_gui_instance_preview --features client-gui
use eframe::egui;
use sandpolis::{Layer, InstanceState, config::Configuration, MODELS};
use sandpolis::client::gui::preview::render_preview_content;
use sandpolis::client::gui::controller::NodeControllerState;
use sandpolis_core::InstanceId;
use sandpolis_database::{DatabaseLayer, config::DatabaseConfig};
use std::env;

#[tokio::main]
async fn main() -> eframe::Result<()> {
    // Set up tracing
    tracing_subscriber::fmt::init();

    // Read configuration from environment
    let layer = env::var("LAYER")
        .ok()
        .and_then(|s| parse_layer(&s))
        .unwrap_or(Layer::Network);

    // Create minimal configuration
    let config = Configuration::default();

    // Create in-memory database
    let db_config = DatabaseConfig {
        storage: None,
        ephemeral: true,
    };
    let database = DatabaseLayer::new(db_config, &*MODELS).unwrap();

    // Create instance state
    let state = InstanceState::new(config.clone(), database).await.unwrap();
    let instance_id = state.instance.instance_id;
    let network_layer = state.network.clone();

    println!("Testing node preview with:");
    println!("  Layer: {:?}", layer);
    println!("  Instance ID: {}", instance_id);

    let options = eframe::NativeOptions {
        viewport: egui::ViewportBuilder::default()
            .with_inner_size([320.0, 150.0])
            .with_title("Node Preview Test Harness"),
        ..Default::default()
    };

    eframe::run_native(
        "Node Preview Test",
        options,
        Box::new(move |_cc| Ok(Box::new(NodePreviewTestApp::new(layer, instance_id, network_layer)))),
    )
}

fn parse_layer(s: &str) -> Option<Layer> {
    match s {
        "Network" => Some(Layer::Network),
        #[cfg(feature = "layer-filesystem")]
        "Filesystem" => Some(Layer::Filesystem),
        #[cfg(feature = "layer-inventory")]
        "Inventory" => Some(Layer::Inventory),
        #[cfg(feature = "layer-shell")]
        "Shell" => Some(Layer::Shell),
        #[cfg(feature = "layer-desktop")]
        "Desktop" => Some(Layer::Desktop),
        _ => None,
    }
}

struct NodePreviewTestApp {
    layer: Layer,
    instance_id: InstanceId,
    network_layer: sandpolis_network::NetworkLayer,
    controller_state: NodeControllerState,
}

impl NodePreviewTestApp {
    fn new(layer: Layer, instance_id: InstanceId, network_layer: sandpolis_network::NetworkLayer) -> Self {
        Self {
            layer,
            instance_id,
            network_layer,
            controller_state: NodeControllerState::default(),
        }
    }
}

impl eframe::App for NodePreviewTestApp {
    fn update(&mut self, ctx: &egui::Context, _frame: &mut eframe::Frame) {
        egui::CentralPanel::default().show(ctx, |ui| {
            ui.heading("Node Preview Component Test");
            ui.separator();

            ui.label(format!("Layer: {:?}", self.layer));

            ui.add_space(20.0);

            // Render the actual preview component
            egui::Frame::new()
                .fill(ui.visuals().window_fill())
                .stroke(ui.visuals().window_stroke())
                .corner_radius(ui.visuals().window_corner_radius)
                .inner_margin(8.0)
                .show(ui, |ui| {
                    ui.set_width(280.0);
                    ui.set_height(80.0);

                    // Call the actual render function with real state
                    render_preview_content(
                        ui,
                        &self.layer,
                        &self.network_layer,
                        &mut self.controller_state,
                        self.instance_id,
                    );
                });
        });
    }
}
