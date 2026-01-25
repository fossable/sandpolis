//! GUI components for the Desktop layer.
//!
//! This module provides the desktop viewer controller and layer-specific GUI elements.

use bevy::prelude::*;
use bevy_egui::egui;
use sandpolis_core::{InstanceId, LayerName};

use sandpolis_client::gui::layer_ext::{ActivityTypeInfo, LayerGuiExtension};

/// Instance metadata.
#[derive(Clone, Debug, Default)]
pub struct InstanceMetadata {
    pub os_type: String,
    pub hostname: Option<String>,
}

/// Query instance metadata.
pub fn query_instance_metadata(_id: InstanceId) -> anyhow::Result<InstanceMetadata> {
    // TODO: Query from database
    Ok(InstanceMetadata::default())
}

/// Render desktop viewer controller.
pub fn render(ui: &mut egui::Ui, instance_id: InstanceId) {
    // Desktop stream placeholder
    ui.label(egui::RichText::new("Desktop Stream").size(16.0).strong());

    ui.separator();

    // Display area for desktop stream
    let stream_size = egui::Vec2::new(560.0, 315.0); // 16:9 aspect ratio
    let (rect, _response) = ui.allocate_exact_size(stream_size, egui::Sense::hover());

    ui.painter().rect_filled(
        rect,
        egui::CornerRadius::ZERO,
        egui::Color32::from_rgb(30, 30, 30),
    );

    ui.painter().text(
        rect.center(),
        egui::Align2::CENTER_CENTER,
        "Desktop stream not active",
        egui::FontId::proportional(14.0),
        egui::Color32::GRAY,
    );

    ui.separator();

    // Control buttons
    ui.horizontal(|ui| {
        if ui.button("Start Stream").clicked() {
            // TODO: Start desktop stream via database write
        }

        if ui.button("Stop Stream").clicked() {
            // TODO: Stop desktop stream
        }

        if ui.button("Screenshot").clicked() {
            // TODO: Request screenshot via database
        }
    });

    ui.separator();

    // Desktop information
    if let Ok(metadata) = query_instance_metadata(instance_id) {
        ui.label(format!("OS: {}", metadata.os_type));
        if let Some(hostname) = metadata.hostname {
            ui.label(format!("Hostname: {}", hostname));
        }
    }

    // Stream quality settings
    ui.separator();
    ui.label("Stream Quality:");
    ui.horizontal(|ui| {
        ui.radio_value(&mut 1, 1, "Low");
        ui.radio_value(&mut 2, 2, "Medium");
        ui.radio_value(&mut 3, 3, "High");
    });
}

/// Desktop layer GUI extension.
pub struct DesktopGuiExtension;

impl LayerGuiExtension for DesktopGuiExtension {
    fn layer(&self) -> &LayerName {
        static LAYER: std::sync::LazyLock<LayerName> =
            std::sync::LazyLock::new(|| LayerName::from("Desktop"));
        &LAYER
    }

    fn description(&self) -> &'static str {
        "Remote desktop viewing and control"
    }

    fn render_controller(&self, ui: &mut egui::Ui, instance_id: InstanceId) {
        render(ui, instance_id);
    }

    fn controller_name(&self) -> &'static str {
        "Desktop Viewer"
    }

    fn get_node_svg(&self, _instance_id: InstanceId) -> &'static str {
        "desktop/Screen.svg"
    }

    fn get_node_color(&self, _instance_id: InstanceId) -> Color {
        // Default color for desktop layer
        Color::WHITE
    }

    fn preview_icon(&self) -> &'static str {
        "Monitor"
    }

    fn preview_details(&self, instance_id: InstanceId) -> String {
        if let Ok(metadata) = query_instance_metadata(instance_id) {
            if let Some(hostname) = metadata.hostname {
                format!("{} - {}", hostname, metadata.os_type)
            } else {
                metadata.os_type
            }
        } else {
            "Desktop status unknown".to_string()
        }
    }

    fn edge_color(&self) -> Color {
        Color::srgb(0.8, 0.3, 0.8) // Purple
    }

    fn activity_types(&self) -> Vec<ActivityTypeInfo> {
        vec![ActivityTypeInfo {
            id: "desktop_stream",
            name: "Desktop Stream",
            color: Color::srgb(0.8, 0.3, 0.8),
            size: 10.0,
        }]
    }

    fn visible_instance_types(&self) -> &'static [sandpolis_core::InstanceType] {
        // Desktop layer shows servers and agents (not clients)
        &[sandpolis_core::InstanceType::Server, sandpolis_core::InstanceType::Agent]
    }
}

/// Static instance of the desktop GUI extension.
static DESKTOP_GUI_EXT: DesktopGuiExtension = DesktopGuiExtension;

// Register the extension with inventory
inventory::submit! {
    &DESKTOP_GUI_EXT as &dyn LayerGuiExtension
}
