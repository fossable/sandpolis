use bevy::{
    input::{
        gestures::RotationGesture,
        mouse::{MouseButtonInput, MouseMotion, MouseWheel},
        touch::TouchPhase,
    },
    prelude::*,
    window::{AppLifecycle, WindowMode},
};
use bevy_egui::EguiContexts;
use std::ops::{Add, Range};

use crate::core::Layer;

use super::{CurrentLayer, LayerChangeTimer};

#[derive(Resource)]
pub struct MousePressed(pub bool);

// #[cfg(target_os = "android")]
pub fn touch_camera(
    windows: Query<&Window>,
    mut touches: EventReader<TouchInput>,
    mut camera: Query<&mut Transform, With<Camera3d>>,
    mut last_position: Local<Option<Vec2>>,
    mut rotations: EventReader<RotationGesture>,
) {
    let window = windows.single();

    for touch in touches.read() {
        if touch.phase == TouchPhase::Started {
            *last_position = None;
        }
        if let Some(last_position) = *last_position {
            let mut transform = camera.single_mut();
            *transform = Transform::from_xyz(
                transform.translation.x
                    + (touch.position.x - last_position.x) / window.width() * 5.0,
                transform.translation.y,
                transform.translation.z
                    + (touch.position.y - last_position.y) / window.height() * 5.0,
            )
            .looking_at(Vec3::ZERO, Vec3::Y);
        }
        *last_position = Some(touch.position);
    }
    // Rotation gestures only work on iOS
    for rotation in rotations.read() {
        let mut transform = camera.single_mut();
        let forward = transform.forward();
        transform.rotate_axis(forward, rotation.0 / 10.0);
    }
}

const CAMERA_KEYBOARD_ZOOM_SPEED: f32 = 1.0;
const CAMERA_MOUSE_WHEEL_ZOOM_SPEED: f32 = 0.25;
const CAMERA_ZOOM_RANGE: Range<f32> = 1.0..12.0;

/// Handle camera movement (panning) using the mouse or keyboard.
pub fn handle_camera(
    keyboard_input: Res<ButtonInput<KeyCode>>,
    mut mouse_button_events: EventReader<MouseButtonInput>,
    mut mouse_motion_events: EventReader<MouseMotion>,
    mut mouse_wheel_input: EventReader<MouseWheel>,
    mut mouse_pressed: ResMut<MousePressed>,
    mut cameras: Query<&mut Transform, With<Camera2d>>,
) {
    let mut distance_delta = 0.0;

    // Handle mouse events.
    for mouse_wheel_event in mouse_wheel_input.read() {
        distance_delta -= mouse_wheel_event.y * CAMERA_MOUSE_WHEEL_ZOOM_SPEED;
    }

    // Update transforms.
    for mut camera_transform in cameras.iter_mut() {
        // Handle keyboard events.
        if keyboard_input.pressed(KeyCode::KeyW) {
            camera_transform.translation.y -= CAMERA_KEYBOARD_ZOOM_SPEED;
        }
        if keyboard_input.pressed(KeyCode::KeyA) {
            camera_transform.translation.x -= CAMERA_KEYBOARD_ZOOM_SPEED;
        }
        if keyboard_input.pressed(KeyCode::KeyS) {
            camera_transform.translation.y += CAMERA_KEYBOARD_ZOOM_SPEED;
        }
        if keyboard_input.pressed(KeyCode::KeyD) {
            camera_transform.translation.x += CAMERA_KEYBOARD_ZOOM_SPEED;
        }

        let local_z = camera_transform.local_z().as_vec3().normalize_or_zero();

        if distance_delta != 0.0 {
            // TODO z position doesn't work in 2D
            camera_transform.translation = (camera_transform.translation.length() + distance_delta)
                .clamp(CAMERA_ZOOM_RANGE.start, CAMERA_ZOOM_RANGE.end)
                * local_z;
            debug!(
                position = ?camera_transform.translation,
                "Moved camera position"
            );
        }

        // Store left-pressed state in the MousePressed resource
        for button_event in mouse_button_events.read() {
            if button_event.button != MouseButton::Left {
                continue;
            }
            *mouse_pressed = MousePressed(button_event.state.is_pressed());
        }

        if mouse_pressed.0 {
            let displacement = mouse_motion_events
                .read()
                .fold(Vec2::ZERO, |acc, mouse_motion| acc + mouse_motion.delta);
            camera_transform.translation -= Vec3::new(displacement.x, -displacement.y, 0.0);
        }
    }
}

/// Show a help window with keyboard shortcuts.
pub fn handle_keymap(mut contexts: EguiContexts, keyboard_input: Res<ButtonInput<KeyCode>>) {
    if keyboard_input.pressed(KeyCode::KeyK) {
        egui::Window::new("Keyboard shortcuts")
            .id(egui::Id::new("keymap"))
            .resizable(false)
            .movable(false)
            .collapsible(false)
            .show(contexts.ctx_mut(), |ui| {
                ui.label("W  -  Pan camera upwards");
                ui.label("A  -  Pan camera upwards");
                ui.label("S  -  Pan camera upwards");
                ui.label("D  -  Pan camera upwards");
                ui.label(">  -  Next layer");
                ui.label("<  -  Previous layer");
                ui.label("M  -  Meta layer");
            });
    }
}

/// Switch to another layer from keypress
pub fn handle_layer_change(
    mut commands: Commands,
    mut contexts: EguiContexts,
    keyboard_input: Res<ButtonInput<KeyCode>>,
    mut current_layer: ResMut<CurrentLayer>,
    time: Res<Time>,
    mut timer: ResMut<LayerChangeTimer>,
    mut windows: Query<&mut Window>,
) {
    #[cfg(feature = "layer-filesystem")]
    if keyboard_input.pressed(KeyCode::KeyF) {
        **current_layer = Layer::Filesystem;
        timer.reset();
    }

    // Now show the current layer for a few seconds
    if !timer.tick(time.delta()).finished() {
        let window_size = windows.single_mut().size();
        egui::Window::new("Current layer")
            .id(egui::Id::new("current_layer"))
            .resizable(false)
            .movable(false)
            .collapsible(false)
            .fixed_pos(egui::Pos2::new(
                window_size.x / 2.0,
                window_size.y + window_size.y / 3.0,
            ))
            .show(contexts.ctx_mut(), |ui| {
                ui.label(format!("{:?}", **current_layer));
            });
    }
}
