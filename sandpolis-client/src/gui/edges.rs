use crate::gui::input::CurrentLayer;
use crate::gui::node::{NodeEntity, WorldView};
use crate::gui::queries;
use bevy::prelude::*;
use bevy_egui::{EguiContexts, egui};
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

/// Render edge labels for network layer (latency, throughput)
pub fn render_edge_labels(
    mut contexts: EguiContexts,
    current_layer: Res<CurrentLayer>,
    network_layer: Res<sandpolis_instance::network::NetworkLayer>,
    edge_query: Query<&Edge>,
    node_query: Query<(&Transform, &NodeEntity)>,
    camera_query: Query<(&Camera, &GlobalTransform), With<WorldView>>,
) {
    // Only show labels on Network layer
    if **current_layer != "Network" {
        return;
    }

    let Ok((camera, camera_transform)) = camera_query.single() else {
        return;
    };

    let Ok(ctx) = contexts.ctx_mut() else {
        return;
    };

    // Build position lookup map once - O(N) instead of O(N*M)
    let node_positions: HashMap<InstanceId, Vec3> = node_query
        .iter()
        .map(|(transform, node)| (node.instance_id, transform.translation))
        .collect();

    // Render label for each edge
    for edge in edge_query.iter() {
        if edge.layer != "Network" {
            continue;
        }

        // O(1) lookup instead of O(N) iteration
        let Some(&from) = node_positions.get(&edge.from) else {
            continue;
        };
        let Some(&to) = node_positions.get(&edge.to) else {
            continue;
        };

        // Calculate midpoint in world space
        let midpoint_world = (from + to) / 2.0;

        // Convert to screen space
        let Ok(screen_pos) = camera.world_to_viewport(camera_transform, midpoint_world) else {
            continue;
        };

        // Query network stats for this edge
        let Ok(stats) = queries::query_network_stats(&network_layer, edge.to) else {
            continue;
        };

        // Build label text
        let label_text = match (stats.latency_ms, stats.throughput_bps) {
            (Some(latency), Some(throughput)) => {
                let mbps = throughput as f64 / 1_000_000.0;
                format!("{}ms | {:.1}Mbps", latency, mbps)
            }
            (Some(latency), None) => format!("{}ms", latency),
            (None, Some(throughput)) => {
                let mbps = throughput as f64 / 1_000_000.0;
                format!("{:.1}Mbps", mbps)
            }
            (None, None) => continue,
        };

        // Render label at midpoint - use Hash-based ID instead of format string
        let label_pos = egui::Pos2::new(screen_pos.x, screen_pos.y);

        egui::Area::new(egui::Id::new("edge_label").with(edge.from).with(edge.to))
            .fixed_pos(label_pos)
            .show(ctx, |ui| {
                ui.label(
                    egui::RichText::new(label_text)
                        .size(10.0)
                        .background_color(egui::Color32::from_rgba_unmultiplied(0, 0, 0, 150))
                        .color(egui::Color32::WHITE),
                );
            });
    }
}
