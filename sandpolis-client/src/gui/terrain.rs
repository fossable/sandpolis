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
    /// Reserved for the forthcoming per-instance `domain` attribute. Returns
    /// `None` until that attribute exists, so it contributes no terrains yet.
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
            TerrainAttribute::Domain => None,
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
        // Instance type is the only attribute that meaningfully varies today.
        Self {
            levels: vec![TerrainAttribute::InstanceType],
        }
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

/// A stable hash of the current node set and config, used to skip terrain
/// rebuilds when nothing relevant changed.
fn membership_signature(ids: &[InstanceId], cfg: &TerrainConfig) -> u64 {
    let mut sorted = ids.to_vec();
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
    nodes: Query<&NodeEntity>,
    regions: Query<(Entity, &TerrainRegion)>,
    labels: Query<(Entity, &TerrainLabel)>,
    mut last_signature: Local<Option<u64>>,
) {
    let ids: Vec<InstanceId> = nodes.iter().map(|n| n.instance_id).collect();
    let signature = membership_signature(&ids, &config);
    if *last_signature == Some(signature) && !config.is_changed() {
        return;
    }
    *last_signature = Some(signature);

    // Desired terrains: every path prefix, with its depth and display name.
    let mut desired: HashMap<Vec<String>, (usize, String)> = HashMap::new();
    for id in &ids {
        let segments = node_segments(*id, &config);
        let mut prefix = Vec::new();
        for (key, display) in segments {
            prefix.push(key);
            let depth = prefix.len();
            desired
                .entry(prefix.clone())
                .or_insert((depth, display));
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
        commands.spawn((
            TerrainRegion {
                path: path.clone(),
                depth: *depth,
                centroid: Vec2::ZERO,
                radii: [MIN_RADIUS; RIM],
            },
            Mesh2d(mesh),
            MeshMaterial2d(materials.add(ColorMaterial::from(fill))),
            Transform::from_xyz(0.0, 0.0, z),
        ));
        commands.spawn((
            TerrainLabel { path: path.clone() },
            Text2d::new(name.clone()),
            TextFont::from_font_size(18.0),
            TextColor(label_color),
            Transform::from_xyz(0.0, 0.0, LABEL_Z),
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
    mut regions: Query<(&mut TerrainRegion, &Mesh2d, &mut Transform)>,
    nodes: Query<(&NodeEntity, &Transform), Without<TerrainRegion>>,
    mut labels: Query<(&TerrainLabel, &mut Transform), (Without<TerrainRegion>, Without<NodeEntity>)>,
) {
    // Node positions keyed by their attribute path (computed once per frame).
    let node_paths: Vec<(Vec2, Vec<String>)> = nodes
        .iter()
        .map(|(node, tf)| {
            let path = node_segments(node.instance_id, &config)
                .into_iter()
                .map(|(k, _)| k)
                .collect();
            (tf.translation.truncate(), path)
        })
        .collect();

    let t = (LERP_RATE * time.delta_secs()).clamp(0.0, 1.0);

    // Where each terrain's label should sit (top of its blob), filled while we
    // update regions and consumed afterwards.
    let mut label_anchors: HashMap<Vec<String>, Vec2> = HashMap::new();

    for (mut region, mesh_handle, mut transform) in regions.iter_mut() {
        // Descendant node positions: those whose path starts with this terrain.
        let members: Vec<Vec2> = node_paths
            .iter()
            .filter(|(_, path)| path.len() >= region.path.len() && path[..region.path.len()] == region.path[..])
            .map(|(pos, _)| *pos)
            .collect();

        if members.is_empty() {
            continue;
        }

        let target_centroid = members.iter().copied().sum::<Vec2>() / members.len() as f32;
        let padding = BASE_PADDING
            + (config.levels.len().saturating_sub(region.depth)) as f32 * LEVEL_PADDING;

        // Target rim radii: for each member, require the buckets around its
        // bearing to reach it (plus padding). Spreading over neighbors keeps the
        // rim from pinching between vertices.
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
            for offset in -2i32..=2 {
                let i = (bucket as i32 + offset).rem_euclid(RIM as i32) as usize;
                if dist > target[i] {
                    target[i] = dist;
                }
            }
        }

        // Round the target by averaging each bucket with its neighbors.
        let mut smoothed = target;
        for i in 0..RIM {
            let prev = target[(i + RIM - 1) % RIM];
            let next = target[(i + 1) % RIM];
            smoothed[i] = (prev + 2.0 * target[i] + next) / 4.0;
        }

        // Ease current bounds toward the (rounded) target.
        region.centroid = region.centroid.lerp(target_centroid, t);
        let mut max_r = 0.0f32;
        for i in 0..RIM {
            let current = region.radii[i];
            region.radii[i] = current + (smoothed[i] - current) * t;
            max_r = max_r.max(region.radii[i]);
        }

        transform.translation.x = region.centroid.x;
        transform.translation.y = region.centroid.y;

        if let Some(mut mesh) = meshes.get_mut(&mesh_handle.0) {
            write_blob_mesh(&mut mesh, &region.radii);
        }

        label_anchors.insert(region.path.clone(), region.centroid + Vec2::new(0.0, max_r + 24.0));
    }

    for (label, mut transform) in labels.iter_mut() {
        if let Some(anchor) = label_anchors.get(&label.path) {
            transform.translation.x = anchor.x;
            transform.translation.y = anchor.y;
        }
    }
}
