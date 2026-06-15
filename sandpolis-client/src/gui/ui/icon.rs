//! SVG icons for `bevy_ui` nodes.
//!
//! `bevy_svg` renders SVGs as 2D world sprites (`Svg2d`), which can't be used as
//! UI nodes. egui used `egui_extras`' image loader instead. For native UI we
//! rasterize SVGs to [`Image`] textures (via `resvg`/`usvg`/`tiny_skia`, already
//! in the dependency tree) and display them with [`ImageNode`]. Results are cached
//! by `(path, size)` so we only rasterize once.
//!
//! Icons are read from [`IconAssetRoot`] (the same asset directory the
//! `AssetPlugin` serves from). Tint per-state via [`ImageNode::color`] rather than
//! baking the tint into the cache key.

use bevy::asset::RenderAssetUsages;
use bevy::image::Image;
use bevy::prelude::*;
use bevy::render::render_resource::{Extent3d, TextureDimension, TextureFormat};
use resvg::{tiny_skia, usvg};
use std::collections::HashMap;
use std::path::{Path, PathBuf};

/// Installs the icon cache and asset-root resources.
pub struct IconPlugin;

impl Plugin for IconPlugin {
    fn build(&self, app: &mut App) {
        app.init_resource::<IconCache>()
            .init_resource::<IconAssetRoot>();
    }
}

/// Filesystem root from which SVG icons are read. Defaults to the same path the
/// `AssetPlugin` is configured with in the GUI bootstrap.
#[derive(Resource)]
pub struct IconAssetRoot(pub PathBuf);

impl Default for IconAssetRoot {
    fn default() -> Self {
        Self(PathBuf::from("../sandpolis-client/assets"))
    }
}

/// Cache of rasterized SVG icons keyed by `(path, size)`.
#[derive(Resource, Default)]
pub struct IconCache {
    cache: HashMap<(String, u32), Handle<Image>>,
}

impl IconCache {
    /// Get a cached icon texture, rasterizing it from `<root>/<path>` at the given
    /// square pixel size on first use. Returns a default (empty) handle if the SVG
    /// can't be read or parsed.
    pub fn get_or_rasterize(
        &mut self,
        images: &mut Assets<Image>,
        root: &Path,
        path: &str,
        size: u32,
    ) -> Handle<Image> {
        let key = (path.to_string(), size);
        if let Some(handle) = self.cache.get(&key) {
            return handle.clone();
        }
        let handle = rasterize_svg(root, path, size)
            .map(|image| images.add(image))
            .unwrap_or_default();
        self.cache.insert(key, handle.clone());
        handle
    }
}

/// Rasterize an SVG file into a square RGBA [`Image`], preserving aspect ratio.
fn rasterize_svg(root: &Path, path: &str, size: u32) -> Option<Image> {
    let bytes = std::fs::read(root.join(path))
        .map_err(|e| warn!("icon read failed for {path}: {e}"))
        .ok()?;
    let tree = usvg::Tree::from_data(&bytes, &usvg::Options::default())
        .map_err(|e| warn!("icon parse failed for {path}: {e}"))
        .ok()?;

    let mut pixmap = tiny_skia::Pixmap::new(size, size)?;
    let svg_size = tree.size();
    let scale = size as f32 / svg_size.width().max(svg_size.height());
    resvg::render(
        &tree,
        tiny_skia::Transform::from_scale(scale, scale),
        &mut pixmap.as_mut(),
    );

    // tiny_skia produces premultiplied alpha; convert to straight alpha so the
    // texture displays correctly when sampled by bevy_ui.
    let mut rgba = pixmap.data().to_vec();
    for px in rgba.chunks_exact_mut(4) {
        let alpha = px[3] as f32 / 255.0;
        if alpha > 0.0 {
            px[0] = ((px[0] as f32 / alpha).round() as u32).min(255) as u8;
            px[1] = ((px[1] as f32 / alpha).round() as u32).min(255) as u8;
            px[2] = ((px[2] as f32 / alpha).round() as u32).min(255) as u8;
        }
    }

    Some(Image::new(
        Extent3d {
            width: size,
            height: size,
            depth_or_array_layers: 1,
        },
        TextureDimension::D2,
        rgba,
        TextureFormat::Rgba8UnormSrgb,
        RenderAssetUsages::RENDER_WORLD | RenderAssetUsages::MAIN_WORLD,
    ))
}
