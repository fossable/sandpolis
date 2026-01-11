use super::components::{MinimapViewport, NodeEntity, WorldView};
use bevy::prelude::*;
use bevy_egui::{EguiContexts, egui};

/// Render the minimap in the bottom-right corner using egui
pub fn render_minimap(
    mut contexts: EguiContexts,
    minimap_viewport: Res<MinimapViewport>,
    camera_query: Query<(&Transform, &Projection), (With<Camera2d>, With<WorldView>)>,
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

            // Get camera position and viewport size
            let (camera_pos, viewport_half_size) = if let Ok((camera_transform, projection)) = camera_query.single() {
                let cam_pos = camera_transform.translation.truncate();
                let half_size = if let Projection::Orthographic(ortho) = projection {
                    Vec2::new(
                        ortho.area.width() * ortho.scale / 2.0,
                        ortho.area.height() * ortho.scale / 2.0,
                    )
                } else {
                    Vec2::new(window_size.x / 2.0, window_size.y / 2.0)
                };
                (cam_pos, half_size)
            } else {
                (Vec2::ZERO, Vec2::new(window_size.x / 2.0, window_size.y / 2.0))
            };

            // Use a fixed world area for the minimap to prevent scale changes when panning
            // This ensures the viewport rectangle size only changes with zoom
            let fixed_world_size: f32 = 2000.0; // Fixed world units shown in minimap
            let min_x = -fixed_world_size;
            let max_x = fixed_world_size;
            let min_y = -fixed_world_size;
            let max_y = fixed_world_size;

            let world_width: f32 = max_x - min_x;
            let world_height: f32 = max_y - min_y;

            // Calculate scale to fit all content in minimap
            let scale_x = minimap_viewport.width / world_width.max(1.0_f32);
            let scale_y = minimap_viewport.height / world_height.max(1.0_f32);
            let scale = scale_x.min(scale_y) * 0.9_f32; // 0.9 for some margin

            // Helper to convert world coords to minimap coords
            // Note: Bevy's Y axis points up, but screen Y points down
            let world_to_minimap = |world_pos: Vec2| -> egui::Pos2 {
                let x = (world_pos.x - min_x) * scale;
                let y = (max_y - world_pos.y) * scale; // Flip Y axis
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
            // Use the calculated camera position and viewport size from earlier
            let top_left = world_to_minimap(Vec2::new(
                camera_pos.x - viewport_half_size.x,
                camera_pos.y + viewport_half_size.y, // Y is flipped
            ));
            let bottom_right = world_to_minimap(Vec2::new(
                camera_pos.x + viewport_half_size.x,
                camera_pos.y - viewport_half_size.y, // Y is flipped
            ));

            let viewport_rect = egui::Rect::from_min_max(top_left, bottom_right);

            ui.painter().rect_stroke(
                viewport_rect,
                egui::Rounding::ZERO,
                egui::Stroke::new(2.0, egui::Color32::from_rgb(255, 200, 0)),
                egui::epaint::StrokeKind::Middle,
            );
        });
}
