use super::{components::NodeEntity, edges::Edge};
use bevy::prelude::*;
use bevy_rapier2d::prelude::*;

/// Configuration parameters for force-directed layout algorithm
#[derive(Resource, Clone)]
pub struct LayoutConfig {
    /// Repulsion strength between nodes (Coulomb's law constant)
    pub repulsion_strength: f32,

    /// Spring attraction strength along edges (Hooke's law constant)
    pub spring_strength: f32,

    /// Desired rest length for springs (edges)
    pub rest_length: f32,

    /// Velocity damping factor (0.0 = no damping, 1.0 = instant stop)
    pub damping: f32,

    /// Maximum force magnitude to prevent instability
    pub max_force: f32,

    /// Velocity threshold for considering layout stable
    pub stability_threshold: f32,
}

impl Default for LayoutConfig {
    fn default() -> Self {
        Self {
            repulsion_strength: 50000.0,
            spring_strength: 0.1,
            rest_length: 200.0,
            damping: 0.85,
            max_force: 1000.0,
            stability_threshold: 0.5,
        }
    }
}

/// Track whether the layout has stabilized
#[derive(Resource)]
pub struct LayoutState {
    pub is_stable: bool,
    pub frames_stable: u32,
    pub required_stable_frames: u32,
}

impl Default for LayoutState {
    fn default() -> Self {
        Self {
            is_stable: false,
            frames_stable: 0,
            required_stable_frames: 30, // ~0.5 seconds at 60 FPS
        }
    }
}

/// Apply Coulomb's law repulsion forces between all node pairs
/// F_repel = k_repel / distance²
pub fn apply_repulsion_forces(
    mut nodes: Query<(&Transform, &mut ExternalForce), With<NodeEntity>>,
    config: Res<LayoutConfig>,
) {
    // Collect all node positions first
    let positions: Vec<Vec3> = nodes
        .iter()
        .map(|(transform, _)| transform.translation)
        .collect();

    // Apply repulsion between all pairs
    for (idx_a, (transform_a, mut force_a)) in nodes.iter_mut().enumerate() {
        let pos_a = transform_a.translation;

        for (idx_b, pos_b) in positions.iter().enumerate() {
            if idx_a == idx_b {
                continue; // Skip self
            }

            let delta = pos_a - *pos_b;
            let distance = delta.length().max(1.0); // Prevent division by zero

            // Coulomb's law: F = k / r²
            let force_magnitude = config.repulsion_strength / (distance * distance);
            let force_magnitude = force_magnitude.min(config.max_force);

            let force_direction = delta.normalize_or_zero();
            let repulsion_force = force_direction * force_magnitude;

            // Apply force to node A
            force_a.force += repulsion_force.truncate();
        }
    }
}

/// Apply Hooke's law spring attraction forces along edges
/// F_spring = k_spring * (distance - rest_length)
pub fn apply_spring_forces(
    edges: Query<&Edge>,
    mut nodes: Query<(Entity, &Transform, &mut ExternalForce, &NodeEntity)>,
    config: Res<LayoutConfig>,
) {
    // Build a map of InstanceId to Entity for quick lookup
    let mut instance_to_entity = std::collections::HashMap::new();
    for (entity, _, _, node) in nodes.iter() {
        instance_to_entity.insert(node.instance_id, entity);
    }

    // Apply spring forces for each edge
    for edge in edges.iter() {
        let Some(&entity_a) = instance_to_entity.get(&edge.from) else {
            continue;
        };
        let Some(&entity_b) = instance_to_entity.get(&edge.to) else {
            continue;
        };

        // Get positions
        let Ok([(_, transform_a, _, _), (_, transform_b, _, _)]) =
            nodes.get_many([entity_a, entity_b])
        else {
            continue;
        };

        let pos_a = transform_a.translation;
        let pos_b = transform_b.translation;
        let delta = pos_b - pos_a;
        let distance = delta.length().max(1.0);

        // Hooke's law: F = k * (distance - rest_length)
        let displacement = distance - config.rest_length;
        let force_magnitude = config.spring_strength * displacement;
        let force_magnitude = force_magnitude.clamp(-config.max_force, config.max_force);

        let force_direction = delta.normalize_or_zero();
        let spring_force = force_direction * force_magnitude;

        // Apply forces (equal and opposite)
        if let Ok((_, _, mut force_a, _)) = nodes.get_mut(entity_a) {
            force_a.force += spring_force.truncate();
        }
        if let Ok((_, _, mut force_b, _)) = nodes.get_mut(entity_b) {
            force_b.force -= spring_force.truncate();
        }
    }
}

/// Apply velocity damping to stabilize the layout
pub fn apply_damping(mut nodes: Query<&mut Velocity, With<NodeEntity>>, config: Res<LayoutConfig>) {
    for mut velocity in nodes.iter_mut() {
        velocity.linvel *= config.damping;
        velocity.angvel *= config.damping;
    }
}

/// Check if the layout has stabilized (average velocity below threshold)
pub fn check_stabilization(
    nodes: Query<&Velocity, With<NodeEntity>>,
    config: Res<LayoutConfig>,
    mut state: ResMut<LayoutState>,
) {
    if nodes.is_empty() {
        state.is_stable = true;
        return;
    }

    // Calculate average velocity magnitude
    let total_velocity: f32 = nodes.iter().map(|v| v.linvel.length()).sum();
    let avg_velocity = total_velocity / nodes.iter().count() as f32;

    // Check if below stability threshold
    if avg_velocity < config.stability_threshold {
        state.frames_stable += 1;
        if state.frames_stable >= state.required_stable_frames {
            state.is_stable = true;
        }
    } else {
        state.frames_stable = 0;
        state.is_stable = false;
    }
}

/// Reset layout stability when nodes are added/removed or layout is changed
pub fn reset_layout_stability(mut state: ResMut<LayoutState>) {
    state.is_stable = false;
    state.frames_stable = 0;
}
