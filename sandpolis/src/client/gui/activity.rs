use super::{components::NodeEntity, edges::Edge};
use bevy::prelude::*;
use sandpolis_core::InstanceId;

/// Component for animated activity lines moving along edges
#[derive(Component)]
pub struct ActivityLine {
    /// Source instance ID
    pub from: InstanceId,
    /// Destination instance ID
    pub to: InstanceId,
    /// Progress along the edge (0.0 to 1.0)
    pub progress: f32,
    /// Speed of movement (units per second)
    pub speed: f32,
    /// Activity type identifier (for different colors/sprites)
    pub activity_type: ActivityType,
}

/// Types of activities that can be visualized
#[derive(Clone, Copy, Debug, PartialEq)]
pub enum ActivityType {
    FileTransfer,
    NetworkTraffic,
    ShellCommand,
    DesktopStream,
}

impl ActivityType {
    /// Get color for this activity type
    pub fn color(&self) -> Color {
        match self {
            ActivityType::FileTransfer => Color::srgb(0.3, 0.8, 0.3), // Green
            ActivityType::NetworkTraffic => Color::srgb(0.3, 0.6, 1.0), // Blue
            ActivityType::ShellCommand => Color::srgb(1.0, 0.8, 0.2), // Yellow
            ActivityType::DesktopStream => Color::srgb(0.9, 0.3, 0.9), // Purple
        }
    }

    /// Get size for this activity type
    pub fn size(&self) -> f32 {
        match self {
            ActivityType::FileTransfer => 8.0,
            ActivityType::NetworkTraffic => 5.0,
            ActivityType::ShellCommand => 6.0,
            ActivityType::DesktopStream => 7.0,
        }
    }
}

/// Bundle for spawning activity line entities
#[derive(Bundle)]
pub struct ActivityLineBundle {
    pub activity_line: ActivityLine,
    pub sprite: Sprite,
    pub transform: Transform,
}

/// Spawn activity lines for active file transfers
pub fn spawn_transfer_activity_lines(
    mut commands: Commands,
    existing_activities: Query<&ActivityLine>,
) {
    // Query database for active transfers
    let Ok(transfers) = super::queries::query_active_transfers() else {
        return;
    };

    // Check which transfers already have activity lines
    for transfer in transfers {
        // Skip if activity line already exists for this transfer
        let exists = existing_activities.iter().any(|activity| {
            activity.from == transfer.from
                && activity.to == transfer.to
                && activity.activity_type == ActivityType::FileTransfer
        });

        if exists {
            continue;
        }

        // Spawn new activity line
        let activity_type = ActivityType::FileTransfer;
        commands.spawn(ActivityLineBundle {
            activity_line: ActivityLine {
                from: transfer.from,
                to: transfer.to,
                progress: 0.0,
                speed: 0.3, // 30% of edge per second
                activity_type,
            },
            sprite: Sprite {
                color: activity_type.color(),
                custom_size: Some(Vec2::splat(activity_type.size())),
                ..default()
            },
            transform: Transform::default(),
        });
    }
}

/// Update activity line positions along their edges
pub fn update_activity_line_positions(
    mut activity_query: Query<(&ActivityLine, &mut Transform)>,
    node_query: Query<(&Transform, &NodeEntity), Without<ActivityLine>>,
    _time: Res<Time>,
) {
    // Build map of instance IDs to positions
    let mut positions = std::collections::HashMap::new();
    for (transform, node) in node_query.iter() {
        positions.insert(node.instance_id, transform.translation.truncate());
    }

    // Update each activity line
    for (activity, mut transform) in activity_query.iter_mut() {
        // Get source and destination positions
        let Some(&from_pos) = positions.get(&activity.from) else {
            continue;
        };
        let Some(&to_pos) = positions.get(&activity.to) else {
            continue;
        };

        // Calculate position along the edge based on progress
        let position = from_pos.lerp(to_pos, activity.progress);
        transform.translation = position.extend(1.0); // Z=1 to render above edges
    }
}

/// Animate activity lines along their paths
pub fn animate_activity_lines(mut activity_query: Query<&mut ActivityLine>, time: Res<Time>) {
    for mut activity in activity_query.iter_mut() {
        // Update progress
        activity.progress += activity.speed * time.delta_secs();

        // Loop back to start when reaching the end
        if activity.progress >= 1.0 {
            activity.progress = 0.0;
        }
    }
}

/// Despawn activity lines for completed transfers
pub fn despawn_completed_activity_lines(
    mut commands: Commands,
    activity_query: Query<(Entity, &ActivityLine)>,
) {
    // Get list of active transfers
    let Ok(active_transfers) = super::queries::query_active_transfers() else {
        return;
    };

    // Check each activity line
    for (entity, activity) in activity_query.iter() {
        if activity.activity_type != ActivityType::FileTransfer {
            continue;
        }

        // Check if transfer still exists
        let transfer_exists = active_transfers
            .iter()
            .any(|transfer| transfer.from == activity.from && transfer.to == activity.to);

        // Despawn if transfer is complete
        if !transfer_exists {
            commands.entity(entity).despawn();
        }
    }
}

/// Spawn network traffic activity lines (for demonstration)
pub fn spawn_network_activity_lines(
    mut commands: Commands,
    edges: Query<&Edge>,
    existing_activities: Query<&ActivityLine>,
    current_layer: Res<super::CurrentLayer>,
) {
    use crate::Layer;

    // Only spawn on Network layer
    if **current_layer != Layer::Network {
        return;
    }

    // Spawn activity line for each edge (if not already exists)
    for edge in edges.iter() {
        // Skip if activity line already exists
        let exists = existing_activities.iter().any(|activity| {
            activity.from == edge.from
                && activity.to == edge.to
                && activity.activity_type == ActivityType::NetworkTraffic
        });

        if exists {
            continue;
        }

        // Spawn network traffic indicator
        let activity_type = ActivityType::NetworkTraffic;
        commands.spawn(ActivityLineBundle {
            activity_line: ActivityLine {
                from: edge.from,
                to: edge.to,
                progress: 0.0,
                speed: 0.5, // Faster than file transfers
                activity_type,
            },
            sprite: Sprite {
                color: activity_type.color(),
                custom_size: Some(Vec2::splat(activity_type.size())),
                ..default()
            },
            transform: Transform::default(),
        });
    }
}

/// Despawn activity lines when switching away from their layer
pub fn cleanup_layer_activity_lines(
    mut commands: Commands,
    activity_query: Query<(Entity, &ActivityLine)>,
    current_layer: Res<super::CurrentLayer>,
) {
    use crate::Layer;

    // Only keep activity lines relevant to current layer
    for (entity, activity) in activity_query.iter() {
        let should_keep = match activity.activity_type {
            ActivityType::FileTransfer => {
                #[cfg(feature = "layer-filesystem")]
                {
                    **current_layer == Layer::Filesystem
                }
                #[cfg(not(feature = "layer-filesystem"))]
                {
                    false
                }
            }
            ActivityType::NetworkTraffic => **current_layer == Layer::Network,
            ActivityType::ShellCommand => {
                #[cfg(feature = "layer-shell")]
                {
                    **current_layer == Layer::Shell
                }
                #[cfg(not(feature = "layer-shell"))]
                {
                    false
                }
            }
            ActivityType::DesktopStream => {
                #[cfg(feature = "layer-desktop")]
                {
                    **current_layer == Layer::Desktop
                }
                #[cfg(not(feature = "layer-desktop"))]
                {
                    false
                }
            }
        };

        if !should_keep {
            commands.entity(entity).despawn();
        }
    }
}
