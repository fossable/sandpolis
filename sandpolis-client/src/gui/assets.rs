//! Compile-time-embedded GUI assets.
//!
//! Both asset consumers in the GUI read from an embedded copy of the asset
//! directory so release builds are self-contained and don't depend on the
//! filesystem layout next to the binary:
//!
//! - bevy's [`AssetServer`] (the `Svg2d` / `Mesh3d` world entities), via an
//!   [`EmbeddedDirReader`] registered as the default asset source in the GUI
//!   bootstrap.
//! - the icon rasterizer ([`crate::gui::ui::icon`]), via [`asset_bytes`].

use bevy::asset::io::{AssetReader, AssetReaderError, PathStream, Reader, VecReader};
use include_dir::{Dir, File, include_dir};
use std::path::Path;

/// Compile-time snapshot of `sandpolis-client/assets`.
static ASSETS: Dir<'static> = include_dir!("$CARGO_MANIFEST_DIR/assets");

/// The embedded asset directory, for callers that merge multiple sources (e.g.
/// the GUI bootstrap, which overlays per-layer asset crates on top of this one).
pub fn dir() -> &'static Dir<'static> {
    &ASSETS
}

/// Raw bytes of an embedded asset by its asset-relative path (e.g.
/// `"layer/Network.svg"`), or `None` if no such asset is embedded.
pub fn asset_bytes(path: &str) -> Option<&'static [u8]> {
    ASSETS.get_file(path).map(File::contents)
}

/// A bevy [`AssetReader`] backed by one or more compile-time-embedded [`Dir`]s.
/// Sources are searched in order, so earlier dirs shadow later ones.
pub struct EmbeddedDirReader {
    dirs: Vec<&'static Dir<'static>>,
}

impl EmbeddedDirReader {
    pub fn new(dirs: Vec<&'static Dir<'static>>) -> Self {
        Self { dirs }
    }

    fn lookup(&self, path: &Path) -> Option<&'static File<'static>> {
        self.dirs.iter().find_map(|dir| dir.get_file(path))
    }

    fn is_dir(&self, path: &Path) -> bool {
        self.dirs.iter().any(|dir| dir.get_dir(path).is_some())
    }
}

impl AssetReader for EmbeddedDirReader {
    async fn read<'a>(&'a self, path: &'a Path) -> Result<impl Reader + 'a, AssetReaderError> {
        match self.lookup(path) {
            Some(file) => Ok(VecReader::new(file.contents().to_vec())),
            None => Err(AssetReaderError::NotFound(path.to_path_buf())),
        }
    }

    async fn read_meta<'a>(&'a self, path: &'a Path) -> Result<impl Reader + 'a, AssetReaderError> {
        // No embedded `.meta` sidecars; let bevy fall back to default settings.
        Err::<VecReader, _>(AssetReaderError::NotFound(path.to_path_buf()))
    }

    async fn read_directory<'a>(
        &'a self,
        path: &'a Path,
    ) -> Result<Box<PathStream>, AssetReaderError> {
        Err(AssetReaderError::NotFound(path.to_path_buf()))
    }

    async fn is_directory<'a>(&'a self, path: &'a Path) -> Result<bool, AssetReaderError> {
        Ok(self.is_dir(path))
    }
}
