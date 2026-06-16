#[cfg(target_os = "android")]
use bevy::input::touch::TouchPhase;
use bevy::{
    input::mouse::{MouseButtonInput, MouseMotion, MouseWheel},
    prelude::*,
};
use crate::gui::ui::gating::UiPointerState;
use crate::gui::ui::panel::modal_scrim;
use crate::gui::ui::theme::{Role, Theme, ThemedBg, ThemedBorder};
use crate::gui::ui::widgets::{button, heading, muted};
use bevy_ui_widgets::Activate;
use sandpolis_instance::LayerName;
use std::ops::Range;

/// Only one layer can be selected at a time.
#[derive(Resource, Deref, DerefMut, Debug)]
pub struct CurrentLayer(pub LayerName);

impl Default for CurrentLayer {
    fn default() -> Self {
        Self(LayerName::from("Network"))
    }
}

/// Current zoom level for the camera.
#[derive(Resource, Deref, DerefMut)]
pub struct ZoomLevel(pub f32);

impl Default for ZoomLevel {
    fn default() -> Self {
        Self(1.0)
    }
}

#[derive(Resource)]
pub struct MousePressed(pub bool);

#[derive(Resource, Default)]
pub struct PanningState {
    /// Whether we're actively panning (started panning and haven't released yet)
    pub is_panning: bool,
}

#[derive(Resource, Deref, DerefMut)]
pub struct LayerChangeTimer(pub Timer);

#[derive(Resource, Default)]
pub struct HelpScreenState {
    pub show: bool,
}

#[derive(Resource, Default)]
pub struct LoginDialogState {
    pub show: bool,
    pub phase: LoginPhase,
    pub server_address: String,
    pub username: String,
    pub password: String,
    pub otp: String,
    pub error_message: Option<String>,
    pub loading: bool,
}

#[derive(Default, PartialEq)]
pub enum LoginPhase {
    #[default]
    ServerAddress,
    Credentials {
        banner: sandpolis_server::ServerBanner,
    },
}

/// Handle touch input for panning on mobile devices
#[cfg(target_os = "android")]
pub fn handle_touch_camera(
    ui_pointer: Res<UiPointerState>,
    windows: Query<&Window>,
    mut touches: MessageReader<TouchInput>,
    mut camera: Query<&mut Transform, With<Camera2d>>,
    mut last_position: Local<Option<Vec2>>,
) {
    // Don't handle touch if the pointer is over blocking UI
    if ui_pointer.over_ui_blocking {
        touches.clear();
        *last_position = None;
        return;
    }

    let Ok(window) = windows.single() else {
        return;
    };

    for touch in touches.read() {
        if touch.phase == TouchPhase::Started {
            *last_position = None;
        }
        if let Some(last_pos) = *last_position {
            if let Ok(mut transform) = camera.single_mut() {
                // Calculate displacement in screen space
                let displacement = touch.position - last_pos;
                // Apply panning (negative Y because screen coords are flipped)
                transform.translation -= Vec3::new(displacement.x, -displacement.y, 0.0);
            }
        }
        *last_position = Some(touch.position);
    }
}

/// Handle pinch-to-zoom gestures on mobile devices using two-finger touch
#[cfg(target_os = "android")]
pub fn handle_touch_zoom(
    ui_pointer: Res<UiPointerState>,
    mut touches: MessageReader<TouchInput>,
    mut zoom_level: ResMut<ZoomLevel>,
    mut camera_query: Query<&mut Projection, With<Camera2d>>,
    mut touch_positions: Local<std::collections::HashMap<u64, Vec2>>,
    mut last_distance: Local<Option<f32>>,
) {
    // Don't handle zoom if the pointer is over blocking UI
    if ui_pointer.over_ui_blocking {
        touches.clear();
        touch_positions.clear();
        *last_distance = None;
        return;
    }

    // Update touch positions
    for touch in touches.read() {
        match touch.phase {
            TouchPhase::Started | TouchPhase::Moved => {
                touch_positions.insert(touch.id, touch.position);
            }
            TouchPhase::Ended | TouchPhase::Canceled => {
                touch_positions.remove(&touch.id);
                if touch_positions.len() < 2 {
                    *last_distance = None;
                }
            }
        }
    }

    // Only process zoom if we have exactly 2 touches
    if touch_positions.len() == 2 {
        let positions: Vec<Vec2> = touch_positions.values().copied().collect();
        let current_distance = positions[0].distance(positions[1]);

        if let Some(prev_distance) = *last_distance {
            // Calculate zoom factor based on distance change
            let distance_ratio = current_distance / prev_distance;
            let zoom_factor = 1.0 / distance_ratio;
            let new_zoom =
                (zoom_level.0 * zoom_factor).clamp(CAMERA_ZOOM_RANGE.start, CAMERA_ZOOM_RANGE.end);

            if let Ok(mut projection) = camera_query.single_mut() {
                if let Projection::Orthographic(ortho) = projection.as_mut() {
                    ortho.scale = new_zoom;
                }
            }

            **zoom_level = new_zoom;
        }

        *last_distance = Some(current_distance);
    } else {
        *last_distance = None;
    }
}

const CAMERA_KEYBOARD_ZOOM_SPEED: f32 = 1.0;
const CAMERA_MOUSE_WHEEL_ZOOM_SPEED: f32 = 0.25;
const CAMERA_ZOOM_RANGE: Range<f32> = 0.5..2.0;

/// Handle zooming using the mouse wheel or keyboard.
/// Zooms toward the center of the screen by adjusting the orthographic projection scale.
pub fn handle_zoom(
    ui_pointer: Res<UiPointerState>,
    mut mouse_wheel_input: MessageReader<MouseWheel>,
    mut zoom_level: ResMut<ZoomLevel>,
    mut camera_query: Query<(&mut Projection, &Transform), With<Camera2d>>,
) {
    // Native UI captured the pointer (e.g. hovering the minimap or a panel) —
    // don't zoom. Previews use `Pickable::IGNORE`, so zooming over them is allowed.
    if ui_pointer.over_ui_blocking {
        mouse_wheel_input.clear();
        return;
    }

    let mut zoom_delta = 0.0;
    for mouse_wheel_event in mouse_wheel_input.read() {
        zoom_delta -= mouse_wheel_event.y * CAMERA_MOUSE_WHEEL_ZOOM_SPEED;
    }

    if zoom_delta != 0.0 {
        let new_zoom =
            (zoom_level.0 + zoom_delta).clamp(CAMERA_ZOOM_RANGE.start, CAMERA_ZOOM_RANGE.end);

        // Update camera's orthographic projection scale
        // Since we're not adjusting camera position, this naturally zooms toward the center
        if let Ok((mut projection, _transform)) = camera_query.single_mut()
            && let Projection::Orthographic(ortho) = projection.as_mut()
        {
            ortho.scale = new_zoom;
        }

        **zoom_level = new_zoom;
    }
}

/// Handle camera movement (panning) using the mouse or keyboard.
pub fn handle_camera(
    ui_pointer: Res<UiPointerState>,
    keyboard_input: Res<ButtonInput<KeyCode>>,
    mut mouse_button_events: MessageReader<MouseButtonInput>,
    mut mouse_motion_events: MessageReader<MouseMotion>,
    mut mouse_pressed: ResMut<MousePressed>,
    mut panning_state: ResMut<PanningState>,
    mut cameras: Query<&mut Transform, With<Camera2d>>,
    drag_state: Res<super::drag::DragState>,
) {
    // Don't handle camera movement if the native UI captured the pointer/keyboard.
    let egui_wants_keyboard = ui_pointer.wants_keyboard;
    let egui_wants_pointer = ui_pointer.over_ui_blocking;

    // Update transforms.
    for mut camera_transform in cameras.iter_mut() {
        // Handle keyboard events only if egui doesn't want them
        if !egui_wants_keyboard {
            if keyboard_input.pressed(KeyCode::ArrowUp) {
                camera_transform.translation.y -= CAMERA_KEYBOARD_ZOOM_SPEED;
            }
            if keyboard_input.pressed(KeyCode::ArrowLeft) {
                camera_transform.translation.x -= CAMERA_KEYBOARD_ZOOM_SPEED;
            }
            if keyboard_input.pressed(KeyCode::ArrowDown) {
                camera_transform.translation.y += CAMERA_KEYBOARD_ZOOM_SPEED;
            }
            if keyboard_input.pressed(KeyCode::ArrowRight) {
                camera_transform.translation.x += CAMERA_KEYBOARD_ZOOM_SPEED;
            }
        }

        // Process mouse button events
        for button_event in mouse_button_events.read() {
            if button_event.button != MouseButton::Left {
                continue;
            }

            if button_event.state.is_pressed() {
                // Mouse button pressed
                if !egui_wants_pointer {
                    // Only set pressed if egui doesn't want it initially
                    *mouse_pressed = MousePressed(true);
                }
            } else {
                // Mouse button released - always clear panning state
                *mouse_pressed = MousePressed(false);
                panning_state.is_panning = false;
            }
        }

        // Handle mouse motion for panning
        let motion_delta = mouse_motion_events
            .read()
            .fold(Vec2::ZERO, |acc, mouse_motion| acc + mouse_motion.delta);

        if motion_delta != Vec2::ZERO {
            // Check if we should start or continue panning
            if mouse_pressed.0 && drag_state.dragging_entity.is_none() {
                if !panning_state.is_panning && !egui_wants_pointer {
                    // Start panning (mouse is pressed, not over egui initially, and not dragging a node)
                    panning_state.is_panning = true;
                }

                // Continue panning once started, even if pointer crosses over egui
                if panning_state.is_panning {
                    camera_transform.translation -= Vec3::new(motion_delta.x, -motion_delta.y, 0.0);
                }
            }
        }
    }
}

/// Modal root marker for the native help screen.
#[derive(Component)]
pub struct HelpRoot;

/// Toggle the help screen with `H` (unless a text field is focused).
pub fn toggle_help(
    ui_pointer: Res<UiPointerState>,
    keyboard: Res<ButtonInput<KeyCode>>,
    mut help_state: ResMut<HelpScreenState>,
) {
    if ui_pointer.wants_keyboard {
        return;
    }
    if keyboard.just_pressed(KeyCode::KeyH) {
        help_state.show = !help_state.show;
    }
}

/// Spawn/despawn the native help modal.
pub fn manage_help_panel(
    mut commands: Commands,
    theme: Res<Theme>,
    help_state: Res<HelpScreenState>,
    root: Query<Entity, With<HelpRoot>>,
) {
    const SHORTCUTS: &[&str] = &[
        "Arrow keys / drag  -  Pan camera",
        "Mouse wheel  -  Zoom",
        "N  -  Find node",
        "T  -  Theme picker",
        "P  -  Toggle node previews",
        "H  -  This help",
        "Click layer indicator  -  Switch layers",
        "Double-click a node  -  Open controller",
    ];

    let exists = !root.is_empty();
    if help_state.show && !exists {
        commands
            .spawn((HelpRoot, modal_scrim()))
            .with_children(|scrim| {
                scrim
                    .spawn((
                        Node {
                            flex_direction: FlexDirection::Column,
                            width: Val::Px(360.0),
                            padding: UiRect::all(Val::Px(16.0)),
                            row_gap: Val::Px(6.0),
                            border: UiRect::all(Val::Px(1.0)),
                            ..default()
                        },
                        BackgroundColor(theme.color(Role::Panel)),
                        ThemedBg(Role::Panel),
                        BorderColor::all(theme.color(Role::Border)),
                        ThemedBorder(Role::Border),
                    ))
                    .with_children(|p| {
                        p.spawn(heading(&theme, "Keyboard Shortcuts"));
                        for line in SHORTCUTS {
                            p.spawn(muted(&theme, *line, theme.metrics.font_md));
                        }
                        p.spawn(button(&theme, "Close")).observe(on_help_close);
                    });
            });
    } else if !help_state.show && exists {
        for entity in &root {
            commands.entity(entity).despawn();
        }
    }
}

fn on_help_close(_activate: On<Activate>, mut help_state: ResMut<HelpScreenState>) {
    help_state.show = false;
}
