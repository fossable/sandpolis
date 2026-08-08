//! Keeps non-nested terrains from overlapping.
//!
//! Terrains are derived from member positions (see [`crate::gui::terrain`]), so
//! the only way to separate two blobs is to move the nodes they enclose. Two
//! systems do that by writing `Transform` directly — the same technique
//! [`crate::gui::drag::update_node_drag`] uses to move a dynamic body — rather
//! than by accumulating `ExternalForce`:
//!
//! * [`relax_terrain_overlap`] pushes overlapping *sibling* regions apart.
//!   Nested regions are exempt, because nesting is exactly the case where
//!   overlap is meaningful.
//! * [`apply_terrain_cohesion`] reels in members that have drifted far from
//!   their own terrain, so the terrain-blind repulsion in [`crate::gui::layout`]
//!   can't interleave two groups faster than separation pulls them apart.
//!
//! Positional relaxation is deliberate. Rapier's `ExternalForce` is an absolute,
//! persistent body force that nothing in this codebase zeroes, and a node's mass
//! is its collider area (~7854 for a 50-unit ball), so force constants here
//! would be neither stable nor interpretable. Moving the transform is immune to
//! all of that and converges monotonically.

use crate::gui::drag::Dragging;
use crate::gui::terrain::{TerrainMember, TerrainRegion};
use bevy::prelude::*;
use std::collections::hash_map::DefaultHasher;
use std::collections::{HashMap, HashSet};
use std::hash::{Hash, Hasher};

/// Clearance (world units) required between two non-nested terrain rims, so the
/// gap reads as deliberate rather than as a near-miss.
const SEPARATION_MARGIN: f32 = 60.0;

/// Overlap below this is ignored, so terrains resting at the margin don't churn.
const SEPARATION_DEADZONE: f32 = 6.0;

/// Fraction of the remaining overlap closed per frame, before the speed cap.
const RELAX_FRACTION: f32 = 0.2;

/// Ceiling on separation speed (world units per second). Kept well below the
/// rim's own response rate (`LERP_RATE` = 9.0, ~110ms) so separation doesn't
/// outrun the geometry it measures and overshoot.
const MAX_RELAX_SPEED: f32 = 150.0;

/// How far from its terrain's centroid a member may drift before cohesion acts.
/// Roughly a comfortable cluster radius, so cohesion is inert in the common case
/// and only reels in strays instead of fighting node-node repulsion.
const COHESION_SLACK: f32 = 150.0;

/// Fraction of the excess distance closed per frame, before the speed cap.
const COHESION_FRACTION: f32 = 0.1;

/// Ceiling on cohesion speed. Below [`MAX_RELAX_SPEED`] so a member is never
/// held inside a foreign terrain by its own.
const MAX_COHESION_SPEED: f32 = 60.0;

/// Centroid distance below which the separation axis is ill-defined.
const DEGENERATE_EPSILON: f32 = 1e-3;

/// A stable arbitrary direction for two exactly co-located regions, so they
/// separate along a consistent axis instead of flipping frame to frame.
fn fallback_axis(a: &[String], b: &[String]) -> Vec2 {
    let mut hasher = DefaultHasher::new();
    a.hash(&mut hasher);
    b.hash(&mut hasher);
    let angle = (hasher.finish() % 3600) as f32 / 3600.0 * std::f32::consts::TAU;
    Vec2::new(angle.cos(), angle.sin())
}

/// How far to translate `a` (and `-1` times that, `b`) this frame to resolve
/// their rim overlap, or `None` if they already clear each other.
///
/// Split out from the system so the pair math is testable without an `App`.
fn separation_step(a: &TerrainRegion, b: &TerrainRegion, dt: f32) -> Option<Vec2> {
    let axis = b.centroid - a.centroid;
    let distance = axis.length();
    let direction = if distance < DEGENERATE_EPSILON {
        fallback_axis(&a.path, &b.path)
    } else {
        axis / distance
    };

    let reach = a.radius_toward(b.centroid) + b.radius_toward(a.centroid);
    let overlap = reach + SEPARATION_MARGIN - distance;
    if overlap < SEPARATION_DEADZONE {
        return None;
    }

    Some(direction * (overlap * RELAX_FRACTION).min(MAX_RELAX_SPEED * dt))
}

/// Translate overlapping sibling terrains apart until their rims clear.
///
/// Every member of a region receives the *same* offset, which translates the
/// blob rigidly: its rim shape, and therefore the overlap measurement that
/// produced the offset, is unchanged by the response. Scaling by member count or
/// pushing each member individually would deform the blob and make this chase
/// its own tail.
///
/// The region query is read-only and never touches `Transform`, so despite the
/// B0001 warning on `update_terrain_bounds` no `Without<TerrainRegion>` filter is
/// needed here — `TerrainRegion::centroid` is what's read, and
/// `update_terrain_bounds` keeps it identical to the region's transform.
pub fn relax_terrain_overlap(
    time: Res<Time>,
    regions: Query<(&TerrainRegion, &Visibility)>,
    dragged: Query<&TerrainMember, With<Dragging>>,
    mut members: Query<(Entity, &TerrainMember, &mut Transform), Without<Dragging>>,
) {
    // Hidden regions have frozen bounds (`update_terrain_bounds` stops tracking
    // a region with no visible members), so acting on them would push nodes
    // around based on stale geometry.
    let mut visible: Vec<&TerrainRegion> = regions
        .iter()
        .filter(|(_, visibility)| **visibility != Visibility::Hidden)
        .map(|(region, _)| region)
        .collect();
    if visible.len() < 2 {
        return;
    }
    // Archetype order isn't a contract; sorting keeps the degenerate-axis
    // tiebreak and the accumulation order reproducible frame to frame.
    visible.sort_unstable_by(|a, b| a.path.cmp(&b.path));

    // Bucket members per region once, so the pair loop below is O(regions²)
    // rather than O(regions² * members).
    let snapshot: Vec<(Entity, Vec<String>)> = members
        .iter()
        .map(|(entity, member, _)| (entity, member.keys()))
        .collect();
    let membership: Vec<Vec<Entity>> = visible
        .iter()
        .map(|region| {
            snapshot
                .iter()
                .filter(|(_, keys)| region.contains_path(keys))
                .map(|(entity, _)| *entity)
                .collect()
        })
        .collect();

    // A dragged node distends its terrain's rim arbitrarily; separating on that
    // would catapult every other member of the terrain. Don't fight the user.
    let held: HashSet<Vec<String>> = dragged.iter().map(TerrainMember::keys).collect();
    let holds_dragged: Vec<bool> = visible
        .iter()
        .map(|region| held.iter().any(|keys| region.contains_path(keys)))
        .collect();

    let dt = time.delta_secs();
    let mut offsets: HashMap<Entity, Vec2> = HashMap::new();
    for i in 0..visible.len() {
        for j in (i + 1)..visible.len() {
            if !visible[i].is_sibling_of(visible[j]) || holds_dragged[i] || holds_dragged[j] {
                continue;
            }
            let Some(step) = separation_step(visible[i], visible[j], dt) else {
                continue;
            };
            for entity in &membership[i] {
                *offsets.entry(*entity).or_default() -= step;
            }
            for entity in &membership[j] {
                *offsets.entry(*entity).or_default() += step;
            }
        }
    }

    let limit = MAX_RELAX_SPEED * dt;
    for (entity, offset) in offsets {
        let Ok((_, _, mut transform)) = members.get_mut(entity) else {
            continue;
        };
        let offset = offset.clamp_length_max(limit);
        transform.translation.x += offset.x;
        transform.translation.y += offset.y;
    }
}

/// Pull members that have strayed back toward their own terrain's centroid.
///
/// Neutral with respect to [`relax_terrain_overlap`]: separation translates a
/// blob rigidly, so the intra-terrain distances cohesion acts on are unchanged.
pub fn apply_terrain_cohesion(
    time: Res<Time>,
    regions: Query<(&TerrainRegion, &Visibility)>,
    mut members: Query<(&TerrainMember, &mut Transform), Without<Dragging>>,
) {
    let centroids: HashMap<&[String], Vec2> = regions
        .iter()
        .filter(|(_, visibility)| **visibility != Visibility::Hidden)
        .map(|(region, _)| (region.path.as_slice(), region.centroid))
        .collect();
    if centroids.is_empty() {
        return;
    }

    let limit = MAX_COHESION_SPEED * time.delta_secs();
    for (member, mut transform) in members.iter_mut() {
        let keys = member.keys();
        if keys.is_empty() {
            continue;
        }
        // Absent for the frame between a membership change and the next
        // `rebuild_terrains`.
        let Some(centroid) = centroids.get(keys.as_slice()) else {
            continue;
        };

        let delta = *centroid - transform.translation.truncate();
        let distance = delta.length();
        if distance <= COHESION_SLACK {
            continue;
        }
        let step = delta.normalize_or_zero()
            * ((distance - COHESION_SLACK) * COHESION_FRACTION).min(limit);
        transform.translation.x += step.x;
        transform.translation.y += step.y;
    }
}

#[cfg(test)]
mod test {
    use super::*;
    use crate::gui::terrain::RIM;

    fn region(path: &[&str], centroid: Vec2, radius: f32) -> TerrainRegion {
        TerrainRegion {
            path: path.iter().map(|key| key.to_string()).collect(),
            depth: path.len(),
            centroid,
            radii: [radius; RIM],
        }
    }

    #[test]
    fn clear_regions_are_left_alone() {
        let a = region(&["a"], Vec2::ZERO, 100.0);
        let b = region(&["b"], Vec2::new(1000.0, 0.0), 100.0);
        assert!(separation_step(&a, &b, 1.0 / 60.0).is_none());
    }

    #[test]
    fn overlapping_regions_push_along_the_centroid_axis() {
        let a = region(&["a"], Vec2::ZERO, 100.0);
        let b = region(&["b"], Vec2::new(150.0, 0.0), 100.0);
        let step = separation_step(&a, &b, 1.0).expect("overlapping regions separate");
        // 100 + 100 + 60 margin - 150 apart = 110 of overlap.
        assert!((step.x - 110.0 * RELAX_FRACTION).abs() < 1e-3);
        assert!(step.y.abs() < 1e-3);
    }

    #[test]
    fn separation_speed_is_capped_by_the_timestep() {
        let a = region(&["a"], Vec2::ZERO, 500.0);
        let b = region(&["b"], Vec2::new(10.0, 0.0), 500.0);
        let dt = 1.0 / 60.0;
        let step = separation_step(&a, &b, dt).expect("overlapping regions separate");
        assert!((step.length() - MAX_RELAX_SPEED * dt).abs() < 1e-3);
    }

    #[test]
    fn colocated_regions_separate_deterministically() {
        let a = region(&["a"], Vec2::ZERO, 100.0);
        let b = region(&["b"], Vec2::ZERO, 100.0);
        let first = separation_step(&a, &b, 1.0).expect("co-located regions separate");
        let second = separation_step(&a, &b, 1.0).expect("co-located regions separate");
        assert_eq!(first, second);
        assert!(first.length() > 0.0);
    }

    /// Drive the systems through a real `App`. Query conflicts (B0001) are a
    /// runtime panic rather than a compile error, so the geometry tests above
    /// can't catch them — only actually running the schedule can.
    fn app() -> App {
        let mut app = App::new();
        // No `TimePlugin`: with real time, consecutive `update()` calls are
        // microseconds apart and every step rounds to nothing. A fixed delta
        // makes the assertions below deterministic.
        let mut time = Time::<()>::default();
        time.advance_by(std::time::Duration::from_secs_f32(1.0 / 60.0));
        app.insert_resource(time);
        app.add_systems(Update, (relax_terrain_overlap, apply_terrain_cohesion).chain());
        app
    }

    fn spawn_region(app: &mut App, path: &[&str], centroid: Vec2, radius: f32) {
        app.world_mut()
            .spawn((region(path, centroid, radius), Visibility::Inherited));
    }

    fn spawn_member(app: &mut App, keys: &[&str], position: Vec2) -> Entity {
        app.world_mut()
            .spawn((
                TerrainMember {
                    segments: keys.iter().map(|k| (k.to_string(), k.to_string())).collect(),
                },
                Transform::from_xyz(position.x, position.y, 0.0),
            ))
            .id()
    }

    fn position(app: &App, entity: Entity) -> Vec2 {
        app.world()
            .entity(entity)
            .get::<Transform>()
            .expect("member keeps its transform")
            .translation
            .truncate()
    }

    #[test]
    fn overlapping_siblings_drift_apart() {
        let mut app = app();
        spawn_region(&mut app, &["a"], Vec2::ZERO, 100.0);
        spawn_region(&mut app, &["b"], Vec2::new(150.0, 0.0), 100.0);
        let left = spawn_member(&mut app, &["a"], Vec2::ZERO);
        let right = spawn_member(&mut app, &["b"], Vec2::new(150.0, 0.0));

        for _ in 0..10 {
            app.update();
        }

        // The regions' own bounds are frozen here (`update_terrain_bounds` isn't
        // in this schedule), so separation keeps pushing at a constant rate; all
        // that's asserted is direction and the per-frame speed cap.
        assert!(position(&app, left).x < -1.0);
        assert!(position(&app, right).x > 151.0);
        assert!(position(&app, left).y.abs() < 1e-3);
    }

    #[test]
    fn nested_terrains_are_left_alone() {
        let mut app = app();
        spawn_region(&mut app, &["a"], Vec2::ZERO, 400.0);
        spawn_region(&mut app, &["a", "b"], Vec2::ZERO, 100.0);
        let member = spawn_member(&mut app, &["a", "b"], Vec2::ZERO);

        for _ in 0..10 {
            app.update();
        }

        // A child sitting entirely inside its parent is the intended case.
        assert_eq!(position(&app, member), Vec2::ZERO);
    }

    #[test]
    fn hidden_regions_do_not_move_anything() {
        let mut app = app();
        app.world_mut()
            .spawn((region(&["a"], Vec2::ZERO, 100.0), Visibility::Hidden));
        app.world_mut().spawn((
            region(&["b"], Vec2::new(150.0, 0.0), 100.0),
            Visibility::Hidden,
        ));
        let member = spawn_member(&mut app, &["a"], Vec2::ZERO);

        for _ in 0..10 {
            app.update();
        }

        assert_eq!(position(&app, member), Vec2::ZERO);
    }

    #[test]
    fn a_dragged_terrain_is_not_pushed() {
        let mut app = app();
        spawn_region(&mut app, &["a"], Vec2::ZERO, 100.0);
        spawn_region(&mut app, &["b"], Vec2::new(150.0, 0.0), 100.0);
        let member = spawn_member(&mut app, &["a"], Vec2::ZERO);
        let other = spawn_member(&mut app, &["b"], Vec2::new(150.0, 0.0));
        app.world_mut().entity_mut(member).insert(Dragging);

        for _ in 0..10 {
            app.update();
        }

        assert_eq!(position(&app, other), Vec2::new(150.0, 0.0));
    }

    #[test]
    fn cohesion_reels_in_a_stray_member() {
        let mut app = app();
        spawn_region(&mut app, &["a"], Vec2::ZERO, 100.0);
        let stray = spawn_member(&mut app, &["a"], Vec2::new(1000.0, 0.0));
        let near = spawn_member(&mut app, &["a"], Vec2::new(50.0, 0.0));

        for _ in 0..10 {
            app.update();
        }

        assert!(position(&app, stray).x < 1000.0);
        // Inside the slack radius, cohesion is inert.
        assert_eq!(position(&app, near), Vec2::new(50.0, 0.0));
    }
}
