use bevy_egui::egui;
use sandpolis_core::InstanceId;
use crate::InstanceState;
use crate::client::gui::queries;

/// Render desktop viewer controller
pub fn render(ui: &mut egui::Ui, state: &InstanceState, instance_id: InstanceId) {
    // Desktop stream placeholder
    ui.label(egui::RichText::new("Desktop Stream").size(16.0).strong());

    ui.separator();

    // Display area for desktop stream
    let stream_size = egui::Vec2::new(560.0, 315.0); // 16:9 aspect ratio
    let (rect, _response) = ui.allocate_exact_size(stream_size, egui::Sense::hover());

    ui.painter().rect_filled(
        rect,
        egui::Rounding::ZERO,
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
        if ui.button("▶ Start Stream").clicked() {
            // TODO: Start desktop stream via database write
        }

        if ui.button("⏸ Stop Stream").clicked() {
            // TODO: Stop desktop stream
        }

        if ui.button("📷 Screenshot").clicked() {
            // TODO: Request screenshot via database
        }
    });

    ui.separator();

    // Desktop information
    if let Ok(metadata) = queries::query_instance_metadata(state, instance_id) {
        ui.label(format!("OS: {:?}", metadata.os_type));
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
