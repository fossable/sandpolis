//! Terrains: soft, colored regions drawn behind groups of nodes.
//!
//! A terrain is an organic ("blob") shaded area of the world view enclosing every
//! node that shares a value for some configurable instance attribute. Terrains are
//! derived automatically from [`TerrainConfig`]: each level in the config is an
//! attribute, and successive levels nest, so a terrain can contain sub-terrains.
//!
//! Each node maps to an ordered path of attribute values (e.g. `["Server",
//! "Linux"]`); every prefix of that path is a terrain, and a node belongs to its
//! leaf terrain and all ancestors. A terrain's shape encloses the positions of all
//! of its descendant nodes plus padding, so a parent always contains its children.
//!
//! Membership is carried by [`TerrainMember`], not derived from `NodeEntity`, so
//! anything positioned in the world can join a terrain — instance nodes get
//! theirs from [`TerrainConfig`] via [`sync_instance_terrain_members`], while
//! layers with their own non-instance nodes (accounts) insert their own.
//!
//! Bounds are stored as a centroid plus `RIM` angular radii and are lerped toward
//! their enclosing target every frame, so dragging a node to the edge makes the
//! region grow smoothly to keep the node inside.

use crate::gui::node::NodeEntity;
use crate::gui::queries::{InstanceMetadata, query_instance_metadata};
use bevy::asset::RenderAssetUsages;
use bevy::mesh::{Indices, PrimitiveTopology};
use bevy::prelude::*;
use sandpolis_instance::InstanceId;
use std::collections::hash_map::DefaultHasher;
use std::collections::{HashMap, HashSet};
use std::hash::{Hash, Hasher};

/// Number of rim vertices used to approximate each terrain blob.
const RIM: usize = 48;

/// Extra space (world units) between the outermost node and the leaf terrain's
/// edge. Roughly a node radius plus margin.
const BASE_PADDING: f32 = 90.0;

/// Additional padding granted to each nesting level above the leaf, so an outer
/// terrain sits visibly outside the terrains it contains.
const LEVEL_PADDING: f32 = 55.0;

/// Minimum radius so a terrain with a single node is still a pleasant circle.
const MIN_RADIUS: f32 = 100.0;

/// How many rim buckets on each side of a node's bearing it pushes outward.
/// Wider spread yields a rounder envelope.
const MEMBER_SPREAD: i32 = 6;

/// Number of [1,2,1]-kernel smoothing passes applied to the target rim.
const SMOOTH_PASSES: usize = 12;

/// Fraction of the top radius at which the label sits, keeping it inside the blob
/// near the upper edge.
const LABEL_TOP_FRACTION: f32 = 0.72;

/// How quickly current bounds approach their target (per second). Higher is
/// snappier; this drives the "smoothly adjust" behavior.
const LERP_RATE: f32 = 9.0;

/// Base Z for terrain fills. Well behind nodes (Z=0) and selection rings (Z≈-0.1).
const FILL_Z: f32 = -50.0;

/// Z for terrain labels: behind nodes, but in front of every fill.
const LABEL_Z: f32 = -1.0;

/// A groupable instance attribute. Each variant maps an instance to an optional
/// `(key, display)` pair; the key defines terrain identity and the display names
/// the terrain. Adding a new attribute (e.g. the future `domain`) is a variant
/// plus one match arm — rendering is unaffected.
#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash)]
pub enum TerrainAttribute {
    /// Group by instance role (Server / Agent / Client).
    InstanceType,
    /// Group by operating system.
    OperatingSystem,
    /// Group by the instance's configured domain.
    Domain,
}

impl TerrainAttribute {
    /// Resolve this attribute for an instance into a `(key, display)` pair, or
    /// `None` if the instance has no value for it.
    pub fn resolve(&self, id: InstanceId, meta: &InstanceMetadata) -> Option<(String, String)> {
        match self {
            TerrainAttribute::InstanceType => {
                let label = if id.is_server() {
                    "Servers"
                } else if id.is_agent() {
                    "Agents"
                } else if id.is_client() {
                    "Clients"
                } else {
                    return None;
                };
                Some((label.to_string(), label.to_string()))
            }
            TerrainAttribute::OperatingSystem => {
                let os = meta.os_type.to_string();
                Some((os.clone(), os))
            }
            TerrainAttribute::Domain => {
                // Empty until the domain has been configured/synced; contributes
                // no terrain rather than an unnamed one.
                if meta.domain.is_empty() {
                    None
                } else {
                    // Key on the normalized domain so an instance's "Example.com"
                    // and an account's "example.com" land in one region.
                    Some((meta.domain.to_lowercase(), meta.domain.clone()))
                }
            }
        }
    }
}

/// Ordered list of attributes that define the terrain hierarchy. This is the
/// configuration surface a future UI picker (or the `domain` attribute) plugs
/// into; changing it rebuilds all terrains.
#[derive(Resource, Clone)]
pub struct TerrainConfig {
    pub levels: Vec<TerrainAttribute>,
}

impl Default for TerrainConfig {
    fn default() -> Self {
        // Group nodes into one terrain per domain.
        Self {
            levels: vec![TerrainAttribute::Domain],
        }
    }
}

/// Places an entity in the terrain hierarchy as an ordered list of
/// `(key, display)` segments — the same shape [`TerrainAttribute::resolve`]
/// produces, but stored rather than recomputed.
///
/// An empty `segments` means "in no terrain"; the component is still inserted so
/// query filters can rely on every terrain-eligible entity having it.
#[derive(Component, Clone, Default)]
pub struct TerrainMember {
    pub segments: Vec<(String, String)>,
}

impl TerrainMember {
    /// Just the keys, which is what region paths are matched against.
    fn keys(&self) -> Vec<String> {
        self.segments.iter().map(|(key, _)| key.clone()).collect()
    }
}

/// Derive [`TerrainMember`] for instance nodes from [`TerrainConfig`].
///
/// Only newly added nodes are resolved unless the config changed. Resolving is
/// not free — it goes through `query_instance_metadata`, which calls
/// `os_info::get()` — so this must not become a per-frame scan.
pub fn sync_instance_terrain_members(
    mut commands: Commands,
    config: Res<TerrainConfig>,
    nodes: Query<(Entity, Ref<NodeEntity>)>,
) {
    let config_changed = config.is_changed();
    for (entity, node) in nodes.iter() {
        if !config_changed && !node.is_added() {
            continue;
        }
        commands.entity(entity).insert(TerrainMember {
            segments: node_segments(node.instance_id, &config),
        });
    }
}

/// A rendered terrain region. Owns its smoothed bounds; the mesh/material live on
/// the same entity via [`Mesh2d`]/[`MeshMaterial2d`].
#[derive(Component)]
pub struct TerrainRegion {
    /// Attribute-key path identifying this terrain (its ancestors are prefixes).
    pub path: Vec<String>,
    /// Nesting depth (1 = top level). Deeper terrains get less padding and a
    /// higher Z so they paint over their parents.
    pub depth: usize,
    /// Smoothed center of the blob in world space.
    pub centroid: Vec2,
    /// Smoothed rim radii, one per angular bucket.
    pub radii: [f32; RIM],
}

/// A world-space name label for a terrain, matched to its region by `path`.
#[derive(Component)]
pub struct TerrainLabel {
    pub path: Vec<String>,
}

/// Full ordered `(key, display)` segments for a node under the current config.
/// Stops at the first attribute that yields `None`.
fn node_segments(id: InstanceId, cfg: &TerrainConfig) -> Vec<(String, String)> {
    let Ok(meta) = query_instance_metadata(id) else {
        return Vec::new();
    };
    let mut segments = Vec::new();
    for attr in &cfg.levels {
        match attr.resolve(id, &meta) {
            Some(seg) => segments.push(seg),
            None => break,
        }
    }
    segments
}

/// Deterministic fill/label colors for a terrain, keyed on its path so a given
/// group always gets the same hue.
fn colors_for(path: &[String], depth: usize) -> (Color, Color) {
    let mut hasher = DefaultHasher::new();
    path.join("\u{1f}").hash(&mut hasher);
    let hue = (hasher.finish() % 360) as f32;
    // Nested fills are slightly more opaque so overlap reads as depth.
    let fill_alpha = 0.12 + (depth as f32 - 1.0) * 0.05;
    let fill = Color::hsla(hue, 0.55, 0.5, fill_alpha.min(0.35));
    let label = Color::hsla(hue, 0.7, 0.72, 0.95);
    (fill, label)
}

/// Build an empty mesh with the right topology/usage; geometry is filled in by
/// [`update_terrain_bounds`].
fn empty_blob_mesh() -> Mesh {
    Mesh::new(
        PrimitiveTopology::TriangleList,
        RenderAssetUsages::RENDER_WORLD | RenderAssetUsages::MAIN_WORLD,
    )
}

/// Rewrite `mesh` as a centroid-fan blob from the given rim radii (local space,
/// centered on the entity's transform).
fn write_blob_mesh(mesh: &mut Mesh, radii: &[f32; RIM]) {
    let mut positions: Vec<[f32; 3]> = Vec::with_capacity(RIM + 1);
    positions.push([0.0, 0.0, 0.0]);
    for (i, &r) in radii.iter().enumerate() {
        let theta = i as f32 / RIM as f32 * std::f32::consts::TAU;
        positions.push([r * theta.cos(), r * theta.sin(), 0.0]);
    }

    let mut indices: Vec<u32> = Vec::with_capacity(RIM * 3);
    for i in 0..RIM {
        let a = (i + 1) as u32;
        let b = ((i + 1) % RIM + 1) as u32;
        indices.extend_from_slice(&[0, a, b]);
    }

    mesh.insert_attribute(Mesh::ATTRIBUTE_POSITION, positions);
    mesh.insert_indices(Indices::U32(indices));
}

/// A stable hash of the current membership and config, used to skip terrain
/// rebuilds when nothing relevant changed. Hashing the paths rather than the
/// members means a node whose attributes changed also triggers a rebuild.
fn membership_signature(paths: &[Vec<String>], cfg: &TerrainConfig) -> u64 {
    let mut sorted = paths.to_vec();
    sorted.sort_unstable();
    let mut hasher = DefaultHasher::new();
    sorted.hash(&mut hasher);
    cfg.levels.hash(&mut hasher);
    hasher.finish()
}

/// Spawn/despawn terrain regions (and their labels) to match the current nodes
/// and [`TerrainConfig`]. Only runs when the node set or config changes.
pub fn rebuild_terrains(
    mut commands: Commands,
    mut meshes: ResMut<Assets<Mesh>>,
    mut materials: ResMut<Assets<ColorMaterial>>,
    config: Res<TerrainConfig>,
    members: Query<(&TerrainMember, &Transform)>,
    regions: Query<(Entity, &TerrainRegion)>,
    labels: Query<(Entity, &TerrainLabel)>,
    mut last_signature: Local<Option<u64>>,
) {
    let paths: Vec<Vec<String>> = members.iter().map(|(member, _)| member.keys()).collect();
    let signature = membership_signature(&paths, &config);
    if *last_signature == Some(signature) && !config.is_changed() {
        return;
    }
    *last_signature = Some(signature);

    // Desired terrains: every path prefix, with its depth and display name.
    // Visibility deliberately plays no part here — a hidden member's region is
    // hidden by `update_terrain_bounds` instead, so switching layers doesn't
    // despawn and respawn every region.
    let mut desired: HashMap<Vec<String>, (usize, String)> = HashMap::new();
    // Where a newly spawned region should start, so it doesn't visibly grow out
    // of the world origin on its first frames.
    let mut seeds: HashMap<Vec<String>, (Vec2, f32)> = HashMap::new();
    for (member, transform) in members.iter() {
        let mut prefix = Vec::new();
        for (key, display) in &member.segments {
            prefix.push(key.clone());
            let depth = prefix.len();
            desired
                .entry(prefix.clone())
                .or_insert((depth, display.clone()));
            let seed = seeds.entry(prefix.clone()).or_insert((Vec2::ZERO, 0.0));
            seed.0 += transform.translation.truncate();
            seed.1 += 1.0;
        }
    }

    // Despawn regions/labels that are no longer wanted.
    let existing: HashSet<Vec<String>> = regions.iter().map(|(_, r)| r.path.clone()).collect();
    for (entity, region) in regions.iter() {
        if !desired.contains_key(&region.path) {
            commands.entity(entity).despawn();
        }
    }
    for (entity, label) in labels.iter() {
        if !desired.contains_key(&label.path) {
            commands.entity(entity).despawn();
        }
    }

    // Spawn regions/labels that are newly wanted.
    for (path, (depth, name)) in &desired {
        if existing.contains(path) {
            continue;
        }
        let (fill, label_color) = colors_for(path, *depth);
        let mesh = meshes.add(empty_blob_mesh());
        let z = FILL_Z + *depth as f32;
        let centroid = seeds
            .get(path)
            .filter(|(_, count)| *count > 0.0)
            .map(|(sum, count)| *sum / *count)
            .unwrap_or(Vec2::ZERO);
        commands.spawn((
            TerrainRegion {
                path: path.clone(),
                depth: *depth,
                centroid,
                radii: [MIN_RADIUS; RIM],
            },
            Mesh2d(mesh),
            MeshMaterial2d(materials.add(ColorMaterial::from(fill))),
            Transform::from_xyz(centroid.x, centroid.y, z),
        ));
        commands.spawn((
            TerrainLabel { path: path.clone() },
            Text2d::new(name.clone()),
            TextFont::from_font_size(18.0),
            TextColor(label_color),
            Transform::from_xyz(centroid.x, centroid.y, LABEL_Z),
        ));
    }
}

/// Recompute each terrain's enclosing target from its descendant node positions,
/// lerp the smoothed bounds toward it, rewrite the blob mesh, and reposition the
/// label. This is what keeps every node inside its terrain and makes the bounds
/// grow smoothly when a node is dragged out.
pub fn update_terrain_bounds(
    time: Res<Time>,
    config: Res<TerrainConfig>,
    mut meshes: ResMut<Assets<Mesh>>,
    mut regions: Query<(&mut TerrainRegion, &Mesh2d, &mut Transform, &mut Visibility)>,
    // `Without<TerrainRegion>` here and `Without<TerrainMember>` on the labels
    // are what keep these three `Transform` accesses disjoint; dropping either
    // is a runtime B0001 panic, not a compile error.
    members: Query<(&TerrainMember, &Transform, Option<&Visibility>), Without<TerrainRegion>>,
    mut labels: Query<
        (&TerrainLabel, &mut Transform, &mut Visibility),
        (Without<TerrainRegion>, Without<TerrainMember>),
    >,
) {
    // Member positions keyed by their attribute path. Hidden members are left
    // out so a region whose layer isn't active stops tracking them.
    let member_paths: Vec<(Vec2, Vec<String>)> = members
        .iter()
        .filter(|(_, _, visibility)| *visibility != Some(&Visibility::Hidden))
        .map(|(member, transform, _)| (transform.translation.truncate(), member.keys()))
        .collect();

    let t = (LERP_RATE * time.delta_secs()).clamp(0.0, 1.0);

    // Where each terrain's label should sit (top of its blob), filled while we
    // update regions and consumed afterwards.
    let mut label_anchors: HashMap<Vec<String>, Vec2> = HashMap::new();
    // Regions with no visible members are hidden rather than despawned, so
    // their bounds are still correct when their layer comes back.
    let mut hidden_paths: HashSet<Vec<String>> = HashSet::new();

    for (mut region, mesh_handle, mut transform, mut visibility) in regions.iter_mut() {
        // Descendant member positions: those whose path starts with this terrain.
        let members: Vec<Vec2> = member_paths
            .iter()
            .filter(|(_, path)| path.len() >= region.path.len() && path[..region.path.len()] == region.path[..])
            .map(|(pos, _)| *pos)
            .collect();

        if members.is_empty() {
            if *visibility != Visibility::Hidden {
                *visibility = Visibility::Hidden;
            }
            hidden_paths.insert(region.path.clone());
            continue;
        }
        if *visibility != Visibility::Inherited {
            *visibility = Visibility::Inherited;
        }

        let target_centroid = members.iter().copied().sum::<Vec2>() / members.len() as f32;
        let padding = BASE_PADDING
            + (config.levels.len().saturating_sub(region.depth)) as f32 * LEVEL_PADDING;

        // Target rim radii: for each member, require the buckets around its
        // bearing to reach it (plus padding). Spreading the influence over a wide
        // arc of neighbors keeps the rim rounded rather than spiking toward each
        // node.
        let mut target = [MIN_RADIUS; RIM];
        for pos in &members {
            let delta = *pos - target_centroid;
            let dist = delta.length() + padding;
            let bucket = if delta.length() < 1e-3 {
                0
            } else {
                let angle = delta.y.atan2(delta.x).rem_euclid(std::f32::consts::TAU);
                (angle / std::f32::consts::TAU * RIM as f32).floor() as usize % RIM
            };
            for offset in -MEMBER_SPREAD..=MEMBER_SPREAD {
                let i = (bucket as i32 + offset).rem_euclid(RIM as i32) as usize;
                if dist > target[i] {
                    target[i] = dist;
                }
            }
        }

        // Round the target with several passes of a [1,2,1] smoothing kernel. The
        // generous padding keeps every node enclosed despite the peak flattening.
        let mut smoothed = target;
        for _ in 0..SMOOTH_PASSES {
            let previous = smoothed;
            for i in 0..RIM {
                let prev = previous[(i + RIM - 1) % RIM];
                let next = previous[(i + 1) % RIM];
                smoothed[i] = (prev + 2.0 * previous[i] + next) / 4.0;
            }
        }

        // Ease current bounds toward the (rounded) target.
        region.centroid = region.centroid.lerp(target_centroid, t);
        for i in 0..RIM {
            let current = region.radii[i];
            region.radii[i] = current + (smoothed[i] - current) * t;
        }

        transform.translation.x = region.centroid.x;
        transform.translation.y = region.centroid.y;

        if let Some(mut mesh) = meshes.get_mut(&mesh_handle.0) {
            write_blob_mesh(&mut mesh, &region.radii);
        }

        // Place the label inside the blob, near its top edge. `RIM / 4` is the
        // bucket pointing straight up (+Y), so its radius is the local top.
        let top_radius = region.radii[RIM / 4];
        label_anchors.insert(
            region.path.clone(),
            region.centroid + Vec2::new(0.0, top_radius * LABEL_TOP_FRACTION),
        );
    }

    for (label, mut transform, mut visibility) in labels.iter_mut() {
        if let Some(anchor) = label_anchors.get(&label.path) {
            transform.translation.x = anchor.x;
            transform.translation.y = anchor.y;
        }
        let hidden = hidden_paths.contains(&label.path);
        let wanted = if hidden {
            Visibility::Hidden
        } else {
            Visibility::Inherited
        };
        if *visibility != wanted {
            *visibility = wanted;
        }
    }
}
