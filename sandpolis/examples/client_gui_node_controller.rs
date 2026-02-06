/// Isolated test harness for the node controller window egui components.
/// This example tests the controller rendering without starting Bevy.
///
/// Environment variables (all optional):
/// - CONTROLLER: Controller to display (FileBrowser, Terminal, SystemInfo, PackageManager, DesktopViewer) [default: SystemInfo]
///
/// Usage: cargo run --example client_gui_node_controller --features client-gui
///
/// Examples:
///   CONTROLLER=FileBrowser cargo run --example client_gui_node_controller --features client-gui
///   CONTROLLER=Terminal cargo run --example client_gui_node_controller --features client-gui
///   CONTROLLER=SystemInfo cargo run --example client_gui_node_controller --features client-gui
use eframe::egui;
use sandpolis::client::gui::controller::ControllerType;
use sandpolis::client::gui::layer_ext::get_extension_for_layer;
use sandpolis::{InstanceState, MODELS, config::Configuration};
use sandpolis_instance::database::{DatabaseLayer, config::DatabaseConfig};
use sandpolis_instance::{InstanceId, LayerName};
use std::env;

#[tokio::main]
async fn main() -> eframe::Result<()> {
    // Set up tracing
    tracing_subscriber::fmt::init();

    // Read configuration from environment
    let controller = env::var("CONTROLLER")
        .ok()
        .and_then(|s| parse_controller(&s))
        .unwrap_or(ControllerType::SystemInfo);

    // Create minimal configuration
    let config = Configuration::default();

    // Create in-memory database
    let db_config = DatabaseConfig {
        storage: None,
        ephemeral: true,
        key: Default::default(),
    };
    let database = DatabaseLayer::new(db_config, &*MODELS).unwrap();

    // Create instance state
    let state = InstanceState::new(config.clone(), database).await.unwrap();
    let instance_id = state.instance.instance_id;

    println!("Testing node controller with:");
    println!("  Controller: {:?}", controller);
    println!("  Instance ID: {}", instance_id);

    let options = eframe::NativeOptions {
        viewport: egui::ViewportBuilder::default()
            .with_inner_size([650.0, 500.0])
            .with_title("Node Controller Test Harness"),
        ..Default::default()
    };

    eframe::run_native(
        "Node Controller Test",
        options,
        Box::new(move |_cc| {
            Ok(Box::new(NodeControllerTestApp::new(
                controller,
                instance_id,
            )))
        }),
    )
}

fn parse_controller(s: &str) -> Option<ControllerType> {
    match s {
        "FileBrowser" => Some(ControllerType::FileBrowser),
        "Terminal" => Some(ControllerType::Terminal),
        "SystemInfo" => Some(ControllerType::SystemInfo),
        "PackageManager" => Some(ControllerType::PackageManager),
        "DesktopViewer" => Some(ControllerType::DesktopViewer),
        _ => None,
    }
}

struct NodeControllerTestApp {
    controller_type: ControllerType,
    instance_id: InstanceId,
}

impl NodeControllerTestApp {
    fn new(controller_type: ControllerType, instance_id: InstanceId) -> Self {
        Self {
            controller_type,
            instance_id,
        }
    }

    fn get_layer_for_controller(&self) -> LayerName {
        match self.controller_type {
            ControllerType::FileBrowser => LayerName::from("Filesystem"),
            ControllerType::Terminal => LayerName::from("Shell"),
            ControllerType::SystemInfo => LayerName::from("Inventory"),
            ControllerType::PackageManager => LayerName::from("Inventory"),
            ControllerType::DesktopViewer => LayerName::from("Desktop"),
            ControllerType::None => LayerName::from("Network"),
        }
    }
}

impl eframe::App for NodeControllerTestApp {
    fn update(&mut self, ctx: &egui::Context, _frame: &mut eframe::Frame) {
        egui::CentralPanel::default().show(ctx, |ui| {
            ui.heading("Node Controller Component Test");
            ui.separator();

            ui.label(format!("Controller: {}", self.controller_type.display_name()));
            ui.label(format!("Instance: {}", self.instance_id));

            ui.add_space(20.0);

            // Render the actual controller component in a frame
            egui::Frame::new()
                .fill(ui.visuals().window_fill())
                .stroke(ui.visuals().window_stroke())
                .corner_radius(ui.visuals().window_corner_radius)
                .inner_margin(8.0)
                .show(ui, |ui| {
                    ui.set_width(600.0);
                    ui.set_height(400.0);

                    // Use trait-based dispatching
                    let layer = self.get_layer_for_controller();
                    if let Some(extension) = get_extension_for_layer(&layer) {
                        extension.render_controller(ui, self.instance_id);
                    } else {
                        ui.label(format!("No GUI extension registered for {} layer", layer.name()));
                        ui.label("Make sure the layer crate is linked with the appropriate feature flag.");
                    }
                });
        });
    }
}
