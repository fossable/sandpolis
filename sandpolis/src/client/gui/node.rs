use super::components::NodeEntity;
use bevy::prelude::*;
use bevy_rapier2d::{
    dynamics::{Damping, ExternalForce, RigidBody, Velocity},
    geometry::{Collider, Restitution},
};
use bevy_svg::prelude::Svg2d;
use sandpolis_core::InstanceId;

#[derive(Bundle)]
pub struct Node {
    pub id: InstanceId,
    pub node_entity: NodeEntity,
    pub collider: Collider,
    pub rigid_body: RigidBody,
    pub velocity: Velocity,
    pub external_force: ExternalForce,
    pub damping: Damping,
    pub svg: Svg2d,
    pub restitution: Restitution,
    pub transform: Transform,
}

pub fn spawn_node(
    asset_server: &AssetServer,
    commands: &mut Commands,
    instance_id: InstanceId,
    os_type: os_info::Type,
) {
    // Random initial position for new nodes
    let x = (rand::random::<f32>() - 0.5) * 500.0;
    let y = (rand::random::<f32>() - 0.5) * 500.0;

    commands.spawn(Node {
        id: instance_id,
        node_entity: NodeEntity { instance_id },
        collider: Collider::ball(50.0),
        rigid_body: RigidBody::Dynamic,
        velocity: Velocity::zero(),
        external_force: ExternalForce::default(),
        damping: Damping {
            linear_damping: 0.0,  // Layout system will handle damping
            angular_damping: 1.0, // Prevent rotation
        },
        restitution: Restitution::coefficient(0.7),
        svg: Svg2d(asset_server.load(get_os_image(os_type))),
        transform: Transform::from_xyz(x, y, 0.0),
    });
}

pub fn get_os_image(os_type: os_info::Type) -> String {
    match os_type {
        os_info::Type::Android => "os/Android.svg",
        os_info::Type::Macos => "os/macOS.svg",
        os_info::Type::Windows => "os/Windows.svg",
        os_info::Type::Arch => "os/Arch Linux.svg",
        os_info::Type::NixOS => "os/NixOS.svg",
        // Check if SUSE exists in the enum, otherwise fallback
        // Note: os_info::Type enum may not have SLES
        _ => {
            // Try to match based on string representation
            let os_str = os_type.to_string();
            if os_str.contains("SUSE") {
                "os/SUSE Linux Enterprise Server.svg"
            } else {
                "os/Unknown.svg"
            }
        }
    }
    .to_string()
}

/// A `WindowStack` is a set of collapsible Windows that are rendered below a
/// node.
#[derive(Component, Clone, Debug)]
pub struct WindowStack {}

pub fn handle_window_stacks(
    commands: Commands,
    // mut contexts: EguiContexts,
    nodes: Query<(&mut Transform, (&InstanceId, &WindowStack)), With<InstanceId>>,
    windows: Query<&mut Window>,
    cameras: Query<&Transform, (With<Camera2d>, Without<InstanceId>)>,
) {
    // let window_size = windows.single_mut().size();
    // let camera_transform = cameras.single();

    // for (transform, (id, window_stack)) in nodes.iter_mut() {
    //     egui::Window::new("Hello")
    //         .movable(false)
    //         .resizable(false)
    //         .pivot(egui::Align2::CENTER_TOP)
    //         .current_pos(egui::Pos2::new(
    //             window_size.x / 2.0 + transform.translation.x -
    // camera_transform.translation.x,             window_size.y / 2.0 +
    // transform.translation.y + camera_transform.translation.y,         ))
    //         .show(contexts.ctx_mut(), |ui| {
    //             ui.label("world");
    //         });
    // }
}
