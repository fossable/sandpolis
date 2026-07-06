//! Tiny persistent key-value store for capture state.
//!
//! Replaces rustdesk's `LocalConfig`, which the Wayland portal code uses to
//! remember the ScreenCast restore token and display offsets between runs so
//! the user isn't re-prompted by the permission dialog on every session.

use std::collections::HashMap;
use std::path::PathBuf;
use std::sync::Mutex;

pub struct LocalConfig;

static CACHE: Mutex<Option<HashMap<String, String>>> = Mutex::new(None);

fn store_path() -> PathBuf {
    dirs::state_dir()
        .or_else(dirs::data_dir)
        .unwrap_or_else(|| PathBuf::from("."))
        .join("sandpolis")
        .join("desktop-capture.json")
}

fn load() -> HashMap<String, String> {
    std::fs::read_to_string(store_path())
        .ok()
        .and_then(|s| serde_json::from_str(&s).ok())
        .unwrap_or_default()
}

impl LocalConfig {
    pub fn get_option(key: &str) -> String {
        let mut cache = CACHE.lock().unwrap();
        let map = cache.get_or_insert_with(load);
        map.get(key).cloned().unwrap_or_default()
    }

    pub fn set_option(key: String, value: String) {
        let mut cache = CACHE.lock().unwrap();
        let map = cache.get_or_insert_with(load);
        if value.is_empty() {
            map.remove(&key);
        } else {
            map.insert(key, value);
        }

        let path = store_path();
        if let Some(parent) = path.parent() {
            std::fs::create_dir_all(parent).ok();
        }
        if let Ok(json) = serde_json::to_string_pretty(&*map) {
            if let Err(err) = std::fs::write(&path, json) {
                tracing::warn!(?path, %err, "failed to persist capture state");
            }
        }
    }
}
