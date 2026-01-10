use super::components::{MinimapViewport, NodeEntity, WorldView};
use bevy::prelude::*;
use bevy_egui::{EguiContexts, egui};

/// Render the minimap in the bottom-right corner using egui
pub fn render_minimap(
    mut contexts: EguiContexts,
    minimap_viewport: Res<MinimapViewport>,
    camera_query: Query<&Transform, (With<Camera2d>, With<WorldView>)>,
    node_query: Query<&Transform, (With<NodeEntity>, Without<Camera2d>)>,
    windows: Query<&Window>,
) {
    let Ok(window) = windows.single() else {
        return;
    };
    let window_size = Vec2::new(window.width(), window.height());

    // Position minimap at bottom-right
    let minimap_pos = egui::Pos2::new(
        window_size.x - minimap_viewport.width - minimap_viewport.bottom_right_offset.x,
        window_size.y - minimap_viewport.height - minimap_viewport.bottom_right_offset.y,
    );

    let Ok(ctx) = contexts.ctx_mut() else {
        return;
    };

    egui::Window::new("Minimap")
        .title_bar(false)
        .resizable(false)
        .movable(false)
        .fixed_pos(minimap_pos)
        .fixed_size([minimap_viewport.width, minimap_viewport.height])
        .show(ctx, |ui| {
            // Draw background
            let rect = ui.available_rect_before_wrap();
            ui.painter().rect_filled(
                rect,
                egui::Rounding::ZERO,
                egui::Color32::from_rgba_unmultiplied(20, 20, 20, 200),
            );

            // Calculate bounds of all nodes to determine minimap scale
            let mut min_x = f32::MAX;
            let mut max_x = f32::MIN;
            let mut min_y = f32::MAX;
            let mut max_y = f32::MIN;

            for transform in node_query.iter() {
                let pos = transform.translation;
                min_x = min_x.min(pos.x);
                max_x = max_x.max(pos.x);
                min_y = min_y.min(pos.y);
                max_y = max_y.max(pos.y);
            }

            // Add padding
            let padding = 100.0;
            min_x -= padding;
            max_x += padding;
            min_y -= padding;
            max_y += padding;

            let world_width = max_x - min_x;
            let world_height = max_y - min_y;

            // Calculate scale to fit all nodes in minimap
            let scale_x = minimap_viewport.width / world_width.max(1.0);
            let scale_y = minimap_viewport.height / world_height.max(1.0);
            let scale = scale_x.min(scale_y) * 0.9; // 0.9 for some margin

            // Helper to convert world coords to minimap coords
            let world_to_minimap = |world_pos: Vec2| -> egui::Pos2 {
                let x = (world_pos.x - min_x) * scale;
                let y = (world_pos.y - min_y) * scale;
                egui::Pos2::new(rect.min.x + x, rect.min.y + y)
            };

            // Draw nodes as small circles
            for transform in node_query.iter() {
                let world_pos = transform.translation.truncate();
                let minimap_pos = world_to_minimap(world_pos);

                ui.painter().circle_filled(
                    minimap_pos,
                    3.0, // Small circle
                    egui::Color32::from_rgb(100, 150, 255),
                );
            }

            // Draw camera viewport rectangle
            if let Ok(camera_transform) = camera_query.single() {
                let camera_pos = camera_transform.translation.truncate();

                // Approximate viewport size (this is simplified - actual viewport depends on zoom)
                let viewport_half_size = Vec2::new(400.0, 300.0);

                let top_left = world_to_minimap(camera_pos - viewport_half_size);
                let bottom_right = world_to_minimap(camera_pos + viewport_half_size);

                let viewport_rect = egui::Rect::from_min_max(top_left, bottom_right);

                ui.painter().rect_stroke(
                    viewport_rect,
                    egui::Rounding::ZERO,
                    egui::Stroke::new(1.0, egui::Color32::from_rgb(255, 200, 0)),
                    egui::epaint::StrokeKind::Middle,
                );
            }
        });
}
