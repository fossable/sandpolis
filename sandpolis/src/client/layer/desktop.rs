use bevy::prelude::*;
use bevy_egui::EguiContexts;

use crate::{
    client::CurrentLayer,
    core::{layer::Layer, InstanceId},
};

pub fn check_layer_active(current_layer: Res<CurrentLayer>) -> bool {
    return **current_layer == Layer::Desktop;
}

pub fn handle_layer(
    commands: Commands,
    mut contexts: EguiContexts,
    mut nodes: Query<(&mut Transform, &InstanceId), With<InstanceId>>,
    mut windows: Query<&mut Window>,
    cameras: Query<&Transform, (With<Camera2d>, Without<InstanceId>)>,
) {
    let window_size = windows.single_mut().size();
    let camera_transform = cameras.single();

    for (transform, id) in nodes.iter_mut() {
        egui::Window::new("Hello")
            .movable(false)
            .resizable(false)
            .pivot(egui::Align2::CENTER_TOP)
            .current_pos(egui::Pos2::new(
                window_size.x / 2.0 + transform.translation.x - camera_transform.translation.x,
                window_size.y / 2.0 + transform.translation.y + camera_transform.translation.y,
            ))
            .show(contexts.ctx_mut(), |ui| {
                ui.label("world");
            });
    }
}
