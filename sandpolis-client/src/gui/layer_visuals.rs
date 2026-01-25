use crate::gui::input::CurrentLayer;
use crate::gui::layer_ext::get_extension_for_layer;
use crate::gui::node::{NeedsScaling, NodeEntity, NodeSvg};
use crate::gui::queries;
use bevy::prelude::*;
use bevy_svg::prelude::{Origin, Svg2d};
use sandpolis_core::{InstanceType, LayerName};

/// Update node SVG images when layer changes or for newly spawned nodes
pub fn update_node_svgs_for_layer(
    mut commands: Commands,
    current_layer: Res<CurrentLayer>,
    asset_server: Res<AssetServer>,
    instance_layer: Res<sandpolis_instance::InstanceLayer>,
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
        let svg_path =
            get_layer_svg_path(&current_layer, &*instance_layer, node_entity.instance_id);

        // Find the SVG child entity
        if let Ok(children) = children_query.get(entity) {
            for &child in children {
                if svg_query.contains(child) {
                    // Update the SVG child
                    commands.entity(child).insert((
                        Svg2d(asset_server.load(svg_path)),
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
    layer: &LayerName,
    instance_layer: &sandpolis_instance::InstanceLayer,
    instance_id: sandpolis_core::InstanceId,
) -> &'static str {
    match layer.name() {
        #[cfg(feature = "layer-filesystem")]
        "Filesystem" => {
            // Show OS-specific icons for filesystem layer
            if let Ok(metadata) = queries::query_instance_metadata(instance_id) {
                super::node::get_os_image(metadata.os_type)
            } else {
                "os/Unknown.svg"
            }
        }

        "Network" => {
            // Distinguish between servers and agents
            // TODO: Query instance type from database
            // For now, use the local instance as reference
            if instance_id == instance_layer.instance_id {
                "network/agent.svg"
            } else {
                "network/server.svg"
            }
        }

        #[cfg(feature = "layer-desktop")]
        "Desktop" => {
            // Show desktop environment icons
            // TODO: Query desktop environment from database
            // For now, default to generic desktop icon
            "desktop/generic.svg"
        }

        #[cfg(feature = "layer-inventory")]
        "Inventory" => {
            // Show hardware type icons (server, desktop, laptop, mobile)
            // TODO: Query hardware type from database
            if let Ok(metadata) = queries::query_instance_metadata(instance_id) {
                // Use OS type as proxy for device type
                match metadata.os_type {
                    os_info::Type::Android => "inventory/mobile.svg",
                    os_info::Type::Windows | os_info::Type::Macos => "inventory/desktop.svg",
                    _ => "inventory/server.svg",
                }
            } else {
                "inventory/server.svg"
            }
        }

        #[cfg(feature = "layer-shell")]
        "Shell" => {
            // Show terminal icon
            "shell/terminal.svg"
        }

        _ => {
            // Default to OS icons for other layers
            if let Ok(metadata) = queries::query_instance_metadata(instance_id) {
                super::node::get_os_image(metadata.os_type)
            } else {
                "os/Unknown.svg"
            }
        }
    }
}

/// Update node colors based on layer-specific states
pub fn update_node_colors_for_layer(
    current_layer: Res<CurrentLayer>,
    network_layer: Res<sandpolis_network::NetworkLayer>,
    mut node_query: Query<(&NodeEntity, &mut Sprite)>,
) {
    // Only update when layer changes
    if !current_layer.is_changed() {
        return;
    }

    for (node_entity, mut sprite) in node_query.iter_mut() {
        // Get layer-specific color tint
        let color = get_layer_color_tint(&current_layer, &network_layer, node_entity.instance_id);
        sprite.color = color;
    }
}

/// Get color tint for a node based on layer and state
fn get_layer_color_tint(
    layer: &LayerName,
    network_layer: &sandpolis_network::NetworkLayer,
    instance_id: sandpolis_core::InstanceId,
) -> Color {
    match layer.name() {
        "Network" => {
            // Color based on connection latency
            if let Ok(stats) = queries::query_network_stats(network_layer, instance_id) {
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
        "Filesystem" => {
            // Color based on disk usage
            if let Ok(usage) = queries::query_filesystem_usage(instance_id) {
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
        "Inventory" => {
            // Color based on memory usage
            if let Ok(mem) = queries::query_memory_stats(instance_id) {
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

/// Update node visibility based on the current layer's visible instance types.
///
/// Each layer can specify which instance types (Server, Agent, Client) should be
/// visible when that layer is active. This system hides nodes that don't match
/// the current layer's filter criteria.
pub fn update_node_visibility_for_layer(
    current_layer: Res<CurrentLayer>,
    mut node_query: Query<(&NodeEntity, &mut Visibility)>,
) {
    // Only update when layer changes
    if !current_layer.is_changed() {
        return;
    }

    // Get the visible instance types for the current layer
    let visible_types = get_visible_instance_types_for_layer(&current_layer);

    for (node_entity, mut visibility) in node_query.iter_mut() {
        let instance_id = node_entity.instance_id;

        // Check if this node's instance type matches any of the visible types
        let should_be_visible = visible_types.iter().any(|instance_type| {
            instance_id.is_type(*instance_type)
        });

        *visibility = if should_be_visible {
            Visibility::Inherited
        } else {
            Visibility::Hidden
        };
    }
}

/// Get the visible instance types for a layer.
///
/// This checks if there's a LayerGuiExtension for the layer and uses its
/// visible_instance_types() method. If no extension exists, falls back to
/// default behavior based on layer name.
fn get_visible_instance_types_for_layer(layer: &LayerName) -> &'static [InstanceType] {
    // First, check if there's a registered extension for this layer
    if let Some(ext) = get_extension_for_layer(layer) {
        return ext.visible_instance_types();
    }

    // Fall back to default behavior based on layer name
    match layer.name() {
        // Network layer shows all instance types
        "Network" => &[InstanceType::Server, InstanceType::Agent, InstanceType::Client],

        // Agent-focused layers only show servers and agents
        "Inventory" | "Filesystem" | "Shell" | "Desktop" | "Probe" => {
            &[InstanceType::Server, InstanceType::Agent]
        }

        // Client layer only shows servers and clients
        "Client" => &[InstanceType::Server, InstanceType::Client],

        // Server layer only shows servers
        "Server" => &[InstanceType::Server],

        // Agent layer only shows servers and agents
        "Agent" => &[InstanceType::Server, InstanceType::Agent],

        // Default: show all
        _ => &[InstanceType::Server, InstanceType::Agent, InstanceType::Client],
    }
}
