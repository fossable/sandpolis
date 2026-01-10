use super::{
    CurrentLayer,
    components::{NodeEntity, WorldView},
    queries,
};
use crate::{InstanceState, Layer};
use bevy::prelude::*;
use bevy_egui::{EguiContexts, egui};
use sandpolis_core::InstanceId;

/// Component representing an edge between two nodes
#[derive(Component)]
pub struct Edge {
    pub from: InstanceId,
    pub to: InstanceId,
    pub layer: Layer,
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
    for edge in edge_query.iter() {
        // Only render edges for the current layer
        if edge.layer != **current_layer {
            continue;
        }

        // Find the positions of the from and to nodes
        let mut from_pos: Option<Vec2> = None;
        let mut to_pos: Option<Vec2> = None;

        for (transform, node_entity) in node_query.iter() {
            if node_entity.instance_id == edge.from {
                from_pos = Some(transform.translation.truncate());
            } else if node_entity.instance_id == edge.to {
                to_pos = Some(transform.translation.truncate());
            }

            // Early exit if we found both
            if from_pos.is_some() && to_pos.is_some() {
                break;
            }
        }

        // Draw line if we found both nodes
        if let (Some(from), Some(to)) = (from_pos, to_pos) {
            // Color based on layer
            let color = match edge.layer {
                Layer::Network => Color::srgb(0.3, 0.8, 1.0),    // Cyan
                Layer::Filesystem => Color::srgb(0.3, 1.0, 0.3), // Green
                Layer::Desktop => Color::srgb(1.0, 0.5, 0.3),    // Orange
                _ => Color::srgb(0.6, 0.6, 0.6),                 // Gray
            };

            gizmos.line_2d(from, to, color);
        }
    }
}

/// Create/update edges based on current layer
/// Queries database for network topology and spawns Edge entities
pub fn update_edges_for_layer(
    mut commands: Commands,
    current_layer: Res<CurrentLayer>,
    edge_query: Query<(Entity, &Edge)>,
    state: Res<InstanceState>,
) {
    // Only update when layer changes
    if !current_layer.is_changed() || current_layer.is_added() {
        return;
    }

    // Despawn all existing edges
    for (entity, _) in edge_query.iter() {
        commands.entity(entity).despawn();
    }

    // Query database for edges relevant to current layer
    match **current_layer {
        Layer::Network => {
            // Query network topology from database
            if let Ok(network_edges) = queries::query_network_topology(&state) {
                for net_edge in network_edges {
                    commands.spawn(Edge {
                        from: net_edge.from,
                        to: net_edge.to,
                        layer: Layer::Network,
                    });
                }
            }
        }
        Layer::Filesystem => {
            // TODO: Query filesystem connections (file transfer paths)
            // For now, no edges
        }
        Layer::Desktop => {
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
    state: Res<InstanceState>,
    edge_query: Query<&Edge>,
    node_query: Query<(&Transform, &NodeEntity)>,
    camera_query: Query<(&Camera, &GlobalTransform), With<WorldView>>,
) {
    // Only show labels on Network layer
    if **current_layer != Layer::Network {
        return;
    }

    let Ok((camera, camera_transform)) = camera_query.single() else {
        return;
    };

    let Ok(ctx) = contexts.ctx_mut() else {
        return;
    };

    // Render label for each edge
    for edge in edge_query.iter() {
        if edge.layer != Layer::Network {
            continue;
        }

        // Find node positions
        let mut from_pos: Option<Vec3> = None;
        let mut to_pos: Option<Vec3> = None;

        for (transform, node_entity) in node_query.iter() {
            if node_entity.instance_id == edge.from {
                from_pos = Some(transform.translation);
            } else if node_entity.instance_id == edge.to {
                to_pos = Some(transform.translation);
            }

            if from_pos.is_some() && to_pos.is_some() {
                break;
            }
        }

        let Some(from) = from_pos else {
            continue;
        };
        let Some(to) = to_pos else {
            continue;
        };

        // Calculate midpoint in world space
        let midpoint_world = (from + to) / 2.0;

        // Convert to screen space
        let Ok(screen_pos) = camera.world_to_viewport(camera_transform, midpoint_world) else {
            continue;
        };

        // Query network stats for this edge
        let label_text = if let Ok(stats) = queries::query_network_stats(&state, edge.to) {
            let mut parts = Vec::new();

            if let Some(latency) = stats.latency_ms {
                parts.push(format!("{}ms", latency));
            }

            if let Some(throughput) = stats.throughput_bps {
                let mbps = throughput as f64 / 1_000_000.0;
                parts.push(format!("{:.1}Mbps", mbps));
            }

            if parts.is_empty() {
                continue;
            }

            parts.join(" | ")
        } else {
            continue;
        };

        // Render label at midpoint
        let label_pos = egui::Pos2::new(screen_pos.x, screen_pos.y);

        egui::Area::new(egui::Id::new(format!(
            "edge_label_{}_{}",
            edge.from, edge.to
        )))
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
