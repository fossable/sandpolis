use bevy::prelude::*;
use sandpolis_core::InstanceId;
use tokio::sync::mpsc;

/// Marker component for the main world view camera
#[derive(Component)]
pub struct WorldView;

/// Component that marks an entity as representing an instance node
#[derive(Component)]
pub struct NodeEntity {
    pub instance_id: InstanceId,
}

/// Minimap configuration
#[derive(Component)]
pub struct Minimap {
    pub zoom_factor: f32, // How much smaller the minimap view is (e.g., 0.1 for 10x zoom out)
}

/// Marker component for the minimap camera
#[derive(Component)]
pub struct MinimapCamera;

/// Marker component for layer indicator UI
#[derive(Component)]
pub struct LayerIndicator;

/// Resource controlling minimap viewport settings
#[derive(Resource)]
pub struct MinimapViewport {
    pub width: f32,
    pub height: f32,
    pub bottom_right_offset: Vec2, // Offset from bottom-right corner
}

impl MinimapViewport {
    /// Create responsive minimap viewport based on window size
    pub fn from_window_size(window_width: f32, window_height: f32) -> Self {
        // For mobile screens (< 800px width), use smaller minimap
        let is_mobile = window_width < 800.0;

        if is_mobile {
            Self {
                width: (window_width * 0.25).max(120.0),
                height: (window_height * 0.15).max(90.0),
                bottom_right_offset: Vec2::new(5.0, 5.0),
            }
        } else {
            Self {
                width: 200.0,
                height: 150.0,
                bottom_right_offset: Vec2::new(10.0, 10.0),
            }
        }
    }
}

impl Default for MinimapViewport {
    fn default() -> Self {
        Self {
            width: 200.0,
            height: 150.0,
            bottom_right_offset: Vec2::new(10.0, 10.0),
        }
    }
}

/// Resource for layer indicator state
#[derive(Resource)]
pub struct LayerIndicatorState {
    pub show_timer: Timer, // Display for N seconds after layer change
}

impl Default for LayerIndicatorState {
    fn default() -> Self {
        Self {
            show_timer: Timer::from_seconds(3.0, TimerMode::Once),
        }
    }
}

/// Database update events from resident listeners
#[derive(Clone, Debug)]
pub enum DatabaseUpdate {
    InstanceAdded(InstanceId),
    InstanceRemoved(InstanceId),
    FilesystemChanged(InstanceId, std::path::PathBuf),
    NetworkTopologyChanged,
    InventoryUpdated(InstanceId),
    ShellOutput(String, Vec<u8>), // session_id, output
    PackagesChanged(InstanceId),
    DesktopEvent(InstanceId),
    TransferStarted(InstanceId, InstanceId, String), // from, to, filename
    TransferProgress(InstanceId, InstanceId, f32),
    TransferCompleted(InstanceId, InstanceId),
}

/// Resource containing channel receiver for database updates
#[derive(Resource)]
pub struct DatabaseUpdateChannel {
    pub receiver: mpsc::UnboundedReceiver<DatabaseUpdate>,
}
