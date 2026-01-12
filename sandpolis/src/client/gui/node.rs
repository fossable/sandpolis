use super::components::NodeEntity;
use bevy::prelude::*;
use bevy_rapier2d::{
    dynamics::{Damping, ExternalForce, RigidBody, Velocity},
    geometry::{Collider, Restitution},
};
use bevy_svg::prelude::{Origin, Svg, Svg2d};
use sandpolis_core::InstanceId;

/// The desired visual diameter for all nodes
const NODE_VISUAL_DIAMETER: f32 = 100.0;

/// Marker component to indicate this node's SVG needs scaling
#[derive(Component)]
pub struct NeedsScaling;

#[derive(Bundle)]
pub struct Node {
    pub id: InstanceId,
    pub node_entity: NodeEntity,
    pub collider: Collider,
    pub rigid_body: RigidBody,
    pub velocity: Velocity,
    pub external_force: ExternalForce,
    pub damping: Damping,
    pub restitution: Restitution,
    pub transform: Transform,
}

/// Marker component for the SVG child entity
#[derive(Component)]
pub struct NodeSvg;

pub fn spawn_node(
    asset_server: &AssetServer,
    commands: &mut Commands,
    instance_id: InstanceId,
    os_type: os_info::Type,
    is_server: bool,
    position: Option<Vec3>,
) {
    // Use provided position or generate random position for new nodes
    let (x, y) = if let Some(pos) = position {
        (pos.x, pos.y)
    } else {
        (
            (rand::random::<f32>() - 0.5) * 500.0,
            (rand::random::<f32>() - 0.5) * 500.0,
        )
    };

    // Start with a placeholder/default SVG that will be replaced by layer system
    // This ensures the correct SVG is loaded based on the current layer
    let svg_path = "network/agent.svg".to_string();

    // Spawn parent node with physics components
    let node_entity = commands.spawn((
        Node {
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
            transform: Transform::from_xyz(x, y, 0.0),
        },
    )).id();

    // Spawn SVG as a child entity
    commands.entity(node_entity).with_children(|parent| {
        parent.spawn((
            Svg2d(asset_server.load(svg_path)),
            Origin::Center,
            Transform::default(),
            NodeSvg,
            NeedsScaling,
        ));
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

/// System to scale SVGs to a uniform size once they're loaded
pub fn scale_node_svgs(
    mut commands: Commands,
    svg_assets: Res<Assets<Svg>>,
    mut nodes_needing_scale: Query<(Entity, &Svg2d, &mut Transform), (With<NeedsScaling>, With<NodeSvg>)>,
) {
    for (entity, svg_handle, mut transform) in nodes_needing_scale.iter_mut() {
        // Check if the SVG asset is loaded
        if let Some(svg) = svg_assets.get(&svg_handle.0) {
            // Get the SVG's natural dimensions
            let svg_size = svg.size;

            // Calculate scale for both dimensions to fit within NODE_VISUAL_DIAMETER
            // while maintaining aspect ratio
            let max_dimension = svg_size.x.max(svg_size.y);

            if max_dimension > 0.0 {
                // Scale to fit the largest dimension within NODE_VISUAL_DIAMETER
                // This ensures the entire SVG (including non-square ones) stays within bounds
                let scale = NODE_VISUAL_DIAMETER / max_dimension;

                // Apply uniform scale to maintain aspect ratio
                transform.scale = Vec3::splat(scale);

                // Calculate the scaled size
                let scaled_size = svg_size * scale;

                // Since Origin::Center doesn't seem to work, manually offset the child transform
                // SVGs render from top-left, so to center it we need to shift it:
                // - Left by half the width (negative x)
                // - Up by half the height (positive y in Bevy's coordinate system)
                transform.translation.x = -scaled_size.x / 2.0;
                transform.translation.y = scaled_size.y / 2.0;

                // Remove the NeedsScaling marker since we're done
                commands.entity(entity).remove::<NeedsScaling>();
            }
        }
    }
}

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
