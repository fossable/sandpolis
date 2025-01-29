use bevy::prelude::*;
use bevy_egui::EguiContexts;
use bevy_rapier2d::{
    dynamics::RigidBody,
    geometry::{Collider, Restitution},
};
use bevy_svg::prelude::Svg2d;

use crate::core::InstanceId;

#[derive(Bundle)]
pub struct Node {
    pub id: InstanceId,
    pub collider: Collider,
    pub rigid_body: RigidBody,
    pub svg: Svg2d,
    pub restitution: Restitution,
}

pub fn spawn_node(
    asset_server: &AssetServer,
    commands: &mut Commands,
    instance_id: InstanceId,
    os_type: os_info::Type,
) {
    commands.spawn(Node {
        id: instance_id,
        collider: Collider::ball(50.0),
        rigid_body: RigidBody::Dynamic,
        restitution: Restitution::coefficient(0.7),
        svg: Svg2d(asset_server.load(get_os_image(os_type))),
    });
}

pub fn get_os_image(os_type: os_info::Type) -> String {
    match os_type {
        // Additional versions
        os_info::Type::Android => todo!(),
        // Additional versions
        os_info::Type::Macos => todo!(),
        // Additional versions
        os_info::Type::Windows => todo!(),
        _ => format!("os/{}.svg", os_type.to_string()),
    }
    .to_string()
}

/// A `WindowStack` is a set of collapsible Windows that are rendered below a node.
#[derive(Component, Clone, Debug)]
pub struct WindowStack {}

pub fn handle_window_stacks(
    commands: Commands,
    mut contexts: EguiContexts,
    mut nodes: Query<(&mut Transform, (&InstanceId, &WindowStack)), With<InstanceId>>,
    mut windows: Query<&mut Window>,
    cameras: Query<&Transform, (With<Camera2d>, Without<InstanceId>)>,
) {
    let window_size = windows.single_mut().size();
    let camera_transform = cameras.single();

    for (transform, (id, window_stack)) in nodes.iter_mut() {
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
