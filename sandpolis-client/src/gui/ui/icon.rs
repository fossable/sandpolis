//! SVG icons for `bevy_ui` nodes.
//!
//! `bevy_svg` renders SVGs as 2D world sprites (`Svg2d`), which can't be used as
//! UI nodes. egui used `egui_extras`' image loader instead. For native UI we
//! rasterize SVGs to [`Image`] textures (via `resvg`/`usvg`/`tiny_skia`, already
//! in the dependency tree) and display them with [`ImageNode`]. Results are cached
//! by `(path, size)` so we only rasterize once.
//!
//! Icons are read from the compile-time-embedded asset bundle
//! ([`crate::gui::assets`]), so rasterization doesn't depend on the process
//! working directory or the on-disk asset layout. Tint per-state via
//! [`ImageNode::color`] rather than baking the tint into the cache key.
//!
//! [`decode_icon_bytes`] handles icons that don't come from the bundle at all —
//! scraped favicons, which arrive as whatever bytes the site served.

use crate::gui::assets;
use bevy::asset::RenderAssetUsages;
use bevy::image::Image;
use bevy::prelude::*;
use bevy::render::render_resource::{Extent3d, TextureDimension, TextureFormat};
use resvg::{tiny_skia, usvg};
use std::collections::HashMap;
use std::io::Cursor;
use std::sync::LazyLock;

/// Installs the icon cache.
pub struct IconPlugin;

impl Plugin for IconPlugin {
    fn build(&self, app: &mut App) {
        app.init_resource::<IconCache>();
    }
}

/// Cache of rasterized SVG icons keyed by `(path, size)`.
#[derive(Resource, Default)]
pub struct IconCache {
    cache: HashMap<(String, u32), Handle<Image>>,
}

impl IconCache {
    /// Get a cached icon texture, rasterizing the embedded SVG at `path` to the
    /// given square pixel size on first use. Returns a default (empty) handle if
    /// the SVG isn't embedded or can't be parsed.
    pub fn get_or_rasterize(
        &mut self,
        images: &mut Assets<Image>,
        path: &str,
        size: u32,
    ) -> Handle<Image> {
        let key = (path.to_string(), size);
        if let Some(handle) = self.cache.get(&key) {
            return handle.clone();
        }
        let handle = rasterize_svg(path, size)
            .map(|image| images.add(image))
            .unwrap_or_default();
        self.cache.insert(key, handle.clone());
        handle
    }
}

/// Rasterize an embedded SVG into a square RGBA [`Image`], preserving aspect ratio.
fn rasterize_svg(path: &str, size: u32) -> Option<Image> {
    let bytes = assets::asset_bytes(path).or_else(|| {
        warn!("icon not found in embedded assets: {path}");
        None
    })?;
    rasterize_svg_bytes(bytes, size)
}

/// Rendering options shared by every rasterization, carrying a font database.
///
/// Several icons letter their glyph — the Shell layer's `$`, the SSH probe's
/// `$_`, the IPMI probe's `BMC`. `usvg::Options::default()` ships an *empty*
/// font database, so those all resolved to nothing and warned once per
/// rasterization. The system fonts are loaded once, lazily, because the scan
/// isn't free and a build with no lettered icon on screen shouldn't pay for it.
static SVG_OPTIONS: LazyLock<usvg::Options<'static>> = LazyLock::new(|| {
    let mut options = usvg::Options::default();
    options.fontdb_mut().load_system_fonts();
    options
});

/// Rasterize SVG source into a square RGBA [`Image`], preserving aspect ratio.
fn rasterize_svg_bytes(bytes: &[u8], size: u32) -> Option<Image> {
    let tree = usvg::Tree::from_data(bytes, &SVG_OPTIONS)
        .map_err(|e| warn!("svg icon parse failed: {e}"))
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

    Some(square_image(size, rgba))
}

/// Largest icon payload we'll even look at. Favicons are small; anything past
/// this is either not an icon or not worth decoding.
const MAX_ICON_BYTES: usize = 2 * 1024 * 1024;

/// Largest source dimension accepted from a raster icon, so a decompression bomb
/// can't allocate its way through the render thread.
const MAX_ICON_DIMENSION: u32 = 4096;

/// Decode icon bytes of unknown provenance into a square RGBA [`Image`].
///
/// Unlike [`IconCache`] this doesn't go through the embedded asset bundle: the
/// bytes come from an arbitrary third-party web server (see
/// `sandpolis_account::favicon`), so both the format and the trustworthiness of
/// the input are unknown. `content_type` is the server's claim about the format
/// and is only used to pick the SVG path; raster formats are identified by their
/// magic bytes instead, since a `.ico` URL frequently serves a PNG.
///
/// The result is always exactly `size` × `size`, with the source letterboxed
/// into the middle of a transparent square.
pub fn decode_icon_bytes(bytes: &[u8], content_type: Option<&str>, size: u32) -> Option<Image> {
    if bytes.is_empty() || size == 0 {
        return None;
    }
    if bytes.len() > MAX_ICON_BYTES {
        warn!("icon rejected: {} bytes exceeds the cap", bytes.len());
        return None;
    }

    if looks_like_svg(bytes, content_type) {
        return rasterize_svg_bytes(bytes, size);
    }

    let mut reader = image::ImageReader::new(Cursor::new(bytes))
        .with_guessed_format()
        .map_err(|e| warn!("icon format detection failed: {e}"))
        .ok()?;
    let mut limits = image::Limits::default();
    limits.max_image_width = Some(MAX_ICON_DIMENSION);
    limits.max_image_height = Some(MAX_ICON_DIMENSION);
    reader.limits(limits);

    let decoded = reader
        .decode()
        .map_err(|e| warn!("icon decode failed: {e}"))
        .ok()?;

    // `resize` fits inside the box while preserving aspect ratio, so the result
    // is at most `size` on each axis but rarely square.
    let scaled = decoded
        .resize(size, size, image::imageops::FilterType::Lanczos3)
        .to_rgba8();

    // `Image::new` panics unless the buffer is exactly `size * size * 4`, so
    // center the scaled icon on a transparent canvas of the requested size.
    // `replace` copies rather than blends, keeping the straight alpha that
    // `to_rgba8` produced.
    let mut canvas = image::RgbaImage::new(size, size);
    let x = (size.saturating_sub(scaled.width()) / 2) as i64;
    let y = (size.saturating_sub(scaled.height()) / 2) as i64;
    image::imageops::replace(&mut canvas, &scaled, x, y);

    Some(square_image(size, canvas.into_raw()))
}

/// Whether these bytes should be handed to the SVG rasterizer.
fn looks_like_svg(bytes: &[u8], content_type: Option<&str>) -> bool {
    if content_type.is_some_and(|t| t.contains("svg")) {
        return true;
    }
    let head = bytes
        .iter()
        .position(|b| !b.is_ascii_whitespace())
        .map(|start| &bytes[start..])
        .unwrap_or_default();
    head.starts_with(b"<svg") || head.starts_with(b"<?xml") || head.starts_with(b"<!DOCTYPE svg")
}

/// Wrap a `size` × `size` straight-alpha RGBA buffer as an [`Image`].
fn square_image(size: u32, rgba: Vec<u8>) -> Image {
    Image::new(
        Extent3d {
            width: size,
            height: size,
            depth_or_array_layers: 1,
        },
        TextureDimension::D2,
        rgba,
        TextureFormat::Rgba8UnormSrgb,
        RenderAssetUsages::RENDER_WORLD | RenderAssetUsages::MAIN_WORLD,
    )
}
