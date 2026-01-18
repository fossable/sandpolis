use crate::gui::MinimapViewport;
use bevy::prelude::*;

/// Update responsive UI elements when window is resized
pub fn update_responsive_ui(
    mut minimap_viewport: ResMut<MinimapViewport>,
    windows: Query<&Window, Changed<Window>>,
) {
    for window in windows.iter() {
        let window_size = Vec2::new(window.width(), window.height());

        // Update minimap viewport for new window size
        *minimap_viewport =
            MinimapViewport::from_window_size(window_size.x, window_size.y);
    }
}
