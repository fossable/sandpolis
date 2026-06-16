use crate::gui::input::CurrentLayer;
use crate::gui::node::{NodeEntity, WorldView};
use crate::gui::queries;
use bevy::prelude::*;
use sandpolis_instance::InstanceId;
use sandpolis_instance::LayerName;
use std::collections::HashMap;

/// Component representing an edge between two nodes
#[derive(Component)]
pub struct Edge {
    pub from: InstanceId,
    pub to: InstanceId,
    pub layer: LayerName,
}

/// Component for edge labels (network stats, etc.)
#[derive(Component)]
pub struct EdgeLabel {
    pub edge_entity: Entity,
    pub text: String,
}

/// Render edges as lines using Bevy's Gizmos
/// Queries database for edge data based on current layer
pub fn render_edges(
    mut gizmos: Gizmos,
    edge_query: Query<&Edge>,
    node_query: Query<(&Transform, &NodeEntity)>,
    current_layer: Res<CurrentLayer>,
) {
    // Build position lookup map once per frame - O(N) instead of O(N*M)
    let node_positions: HashMap<InstanceId, Vec2> = node_query
        .iter()
        .map(|(transform, node)| (node.instance_id, transform.translation.truncate()))
        .collect();

    for edge in edge_query.iter() {
        // Only render edges for the current layer
        if edge.layer != **current_layer {
            continue;
        }

        // O(1) lookup instead of O(N) iteration
        let Some(&from) = node_positions.get(&edge.from) else {
            continue;
        };
        let Some(&to) = node_positions.get(&edge.to) else {
            continue;
        };

        // Color based on layer
        let color = match edge.layer.name() {
            "Network" => Color::srgb(0.3, 0.8, 1.0),    // Cyan
            "Filesystem" => Color::srgb(0.3, 1.0, 0.3), // Green
            "Desktop" => Color::srgb(1.0, 0.5, 0.3),    // Orange
            _ => Color::srgb(0.6, 0.6, 0.6),            // Gray
        };

        gizmos.line_2d(from, to, color);
    }
}

/// Create/update edges based on current layer
/// Queries database for network topology and spawns Edge entities
pub fn update_edges_for_layer(
    mut commands: Commands,
    current_layer: Res<CurrentLayer>,
    edge_query: Query<(Entity, &Edge)>,
    network_layer: Res<sandpolis_instance::network::NetworkLayer>,
) {
    // Only update when layer changes or is first added
    if !current_layer.is_changed() && !current_layer.is_added() {
        return;
    }

    // Despawn all existing edges
    for (entity, _) in edge_query.iter() {
        commands.entity(entity).despawn();
    }

    // Query database for edges relevant to current layer
    match current_layer.name() {
        "Network" => {
            // Query network topology from database
            if let Ok(network_edges) = queries::query_network_topology(&network_layer) {
                for net_edge in network_edges {
                    commands.spawn(Edge {
                        from: net_edge.from,
                        to: net_edge.to,
                        layer: LayerName::from("Network"),
                    });
                }
            }
        }
        "Filesystem" => {
            // TODO: Query filesystem connections (file transfer paths)
            // For now, no edges
        }
        "Desktop" => {
            // TODO: Query desktop streaming connections
            // For now, no edges
        }
        _ => {
            // Other layers might not have edges
        }
    }
}

/// System to show/hide edges based on visibility
pub fn update_edge_visibility(
    mut edge_query: Query<(&mut Visibility, &Edge)>,
    current_layer: Res<CurrentLayer>,
) {
    for (mut visibility, edge) in edge_query.iter_mut() {
        *visibility = if edge.layer == **current_layer {
            Visibility::Visible
        } else {
            Visibility::Hidden
        };
    }
}

