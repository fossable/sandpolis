use super::components::NodeEntity;
use super::node::{NeedsScaling, NodeSvg};
use super::queries;
use crate::{InstanceState, Layer};
use bevy::prelude::*;
use bevy_svg::prelude::{Origin, Svg2d};

/// Update node SVG images when layer changes or for newly spawned nodes
pub fn update_node_svgs_for_layer(
    mut commands: Commands,
    current_layer: Res<super::CurrentLayer>,
    asset_server: Res<AssetServer>,
    state: Res<InstanceState>,
    node_query: Query<(Entity, &NodeEntity)>,
    children_query: Query<&Children>,
    svg_query: Query<Entity, With<NodeSvg>>,
    mut has_run: Local<bool>,
) {
    // Run when layer changes OR on first execution (to initialize newly spawned nodes)
    let should_run = current_layer.is_changed() || !*has_run;

    if !should_run {
        return;
    }

    // Mark that we've run at least once
    *has_run = true;

    // Update each node's SVG based on current layer
    for (entity, node_entity) in node_query.iter() {
        let svg_path = get_layer_svg_path(&current_layer, &state, node_entity.instance_id);

        // Find the SVG child entity
        if let Ok(children) = children_query.get(entity) {
            for &child in children {
                if svg_query.contains(child) {
                    // Update the SVG child
                    commands.entity(child).insert((
                        Svg2d(asset_server.load(svg_path.clone())),
                        Origin::Center,
                        NeedsScaling,
                    ));
                    break;
                }
            }
        }
    }
}

/// Get the appropriate SVG path for a node based on the current layer
fn get_layer_svg_path(
    layer: &Layer,
    state: &InstanceState,
    instance_id: sandpolis_core::InstanceId,
) -> String {
    match layer {
        #[cfg(feature = "layer-filesystem")]
        Layer::Filesystem => {
            // Show OS-specific icons for filesystem layer
            if let Ok(metadata) = queries::query_instance_metadata(state, instance_id) {
                super::node::get_os_image(metadata.os_type)
            } else {
                "os/Unknown.svg".to_string()
            }
        }

        Layer::Network => {
            // Distinguish between servers and agents
            // TODO: Query instance type from database
            // For now, use the local instance as reference
            if instance_id == state.instance.instance_id {
                "network/agent.svg".to_string()
            } else {
                "network/server.svg".to_string()
            }
        }

        #[cfg(feature = "layer-desktop")]
        Layer::Desktop => {
            // Show desktop environment icons
            // TODO: Query desktop environment from database
            // For now, default to generic desktop icon
            "desktop/generic.svg".to_string()
        }

        #[cfg(feature = "layer-inventory")]
        Layer::Inventory => {
            // Show hardware type icons (server, desktop, laptop, mobile)
            // TODO: Query hardware type from database
            if let Ok(metadata) = queries::query_instance_metadata(state, instance_id) {
                // Use OS type as proxy for device type
                match metadata.os_type {
                    os_info::Type::Android => "inventory/mobile.svg".to_string(),
                    os_info::Type::Windows | os_info::Type::Macos => {
                        "inventory/desktop.svg".to_string()
                    }
                    _ => "inventory/server.svg".to_string(),
                }
            } else {
                "inventory/server.svg".to_string()
            }
        }

        #[cfg(feature = "layer-shell")]
        Layer::Shell => {
            // Show terminal icon
            "shell/terminal.svg".to_string()
        }

        _ => {
            // Default to OS icons for other layers
            if let Ok(metadata) = queries::query_instance_metadata(state, instance_id) {
                super::node::get_os_image(metadata.os_type)
            } else {
                "os/Unknown.svg".to_string()
            }
        }
    }
}

/// Update node colors based on layer-specific states
pub fn update_node_colors_for_layer(
    current_layer: Res<super::CurrentLayer>,
    state: Res<InstanceState>,
    mut node_query: Query<(&NodeEntity, &mut Sprite)>,
) {
    // Only update when layer changes
    if !current_layer.is_changed() {
        return;
    }

    for (node_entity, mut sprite) in node_query.iter_mut() {
        // Get layer-specific color tint
        let color = get_layer_color_tint(&current_layer, &state, node_entity.instance_id);
        sprite.color = color;
    }
}

/// Get color tint for a node based on layer and state
fn get_layer_color_tint(
    layer: &Layer,
    state: &InstanceState,
    instance_id: sandpolis_core::InstanceId,
) -> Color {
    match layer {
        Layer::Network => {
            // Color based on connection latency
            if let Ok(stats) = queries::query_network_stats(state, instance_id) {
                if let Some(latency) = stats.latency_ms {
                    if latency < 50 {
                        Color::srgb(0.7, 1.0, 0.7) // Green - good connection
                    } else if latency < 150 {
                        Color::srgb(1.0, 1.0, 0.7) // Yellow - moderate
                    } else {
                        Color::srgb(1.0, 0.7, 0.7) // Red - poor connection
                    }
                } else {
                    Color::srgb(0.7, 0.7, 0.7) // Gray - no connection
                }
            } else {
                Color::WHITE
            }
        }

        #[cfg(feature = "layer-filesystem")]
        Layer::Filesystem => {
            // Color based on disk usage
            if let Ok(usage) = queries::query_filesystem_usage(state, instance_id) {
                let percent = if usage.total > 0 {
                    (usage.used as f64 / usage.total as f64) * 100.0
                } else {
                    0.0
                };

                if percent < 70.0 {
                    Color::srgb(0.7, 1.0, 0.7) // Green
                } else if percent < 90.0 {
                    Color::srgb(1.0, 1.0, 0.7) // Yellow
                } else {
                    Color::srgb(1.0, 0.7, 0.7) // Red
                }
            } else {
                Color::WHITE
            }
        }

        #[cfg(feature = "layer-inventory")]
        Layer::Inventory => {
            // Color based on memory usage
            if let Ok(mem) = queries::query_memory_stats(state, instance_id) {
                let percent = if mem.total > 0 {
                    (mem.used as f64 / mem.total as f64) * 100.0
                } else {
                    0.0
                };

                if percent < 70.0 {
                    Color::srgb(0.7, 1.0, 0.7) // Green
                } else if percent < 90.0 {
                    Color::srgb(1.0, 1.0, 0.7) // Yellow
                } else {
                    Color::srgb(1.0, 0.7, 0.7) // Red
                }
            } else {
                Color::WHITE
            }
        }

        _ => Color::WHITE, // No tint for other layers
    }
}
