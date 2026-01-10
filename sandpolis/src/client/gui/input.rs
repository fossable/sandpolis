use super::{CurrentLayer, ZoomLevel};
use crate::Layer;
use bevy::{
    ecs::event::EventReader,
    input::{
        gestures::RotationGesture,
        mouse::{MouseButtonInput, MouseMotion, MouseWheel},
        touch::TouchPhase,
    },
    prelude::*,
};
use std::ops::Range;

#[derive(Resource)]
pub struct MousePressed(pub bool);

#[derive(Resource, Deref, DerefMut)]
pub struct LayerChangeTimer(pub Timer);

// #[cfg(target_os = "android")]
pub fn touch_camera(
    windows: Query<&Window>,
    mut touches: EventReader<TouchInput>,
    mut camera: Query<&mut Transform, With<Camera3d>>,
    mut last_position: Local<Option<Vec2>>,
    mut rotations: EventReader<RotationGesture>,
) {
    let window = windows.single().unwrap();

    for touch in touches.read() {
        if touch.phase == TouchPhase::Started {
            *last_position = None;
        }
        if let Some(last_position) = *last_position {
            let mut transform = camera.single_mut().unwrap();
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
        let mut transform = camera.single_mut().unwrap();
        let forward = transform.forward();
        transform.rotate_axis(forward, rotation.0 / 10.0);
    }
}

const CAMERA_KEYBOARD_ZOOM_SPEED: f32 = 1.0;
const CAMERA_MOUSE_WHEEL_ZOOM_SPEED: f32 = 0.25;
const CAMERA_ZOOM_RANGE: Range<f32> = 0.5..2.0;
const SPRITE_SIZE: f32 = 32.0;

/// Handle zooming using the mouse wheel or keyboard.
pub fn handle_zoom(
    keyboard_input: Res<ButtonInput<KeyCode>>,
    mut mouse_wheel_input: EventReader<MouseWheel>,
    mut zoom_level: ResMut<ZoomLevel>,
    mut sprites: Query<&mut Sprite>,
) {
    let mut zoom_delta = 0.0;
    for mouse_wheel_event in mouse_wheel_input.read() {
        zoom_delta -= mouse_wheel_event.y * CAMERA_MOUSE_WHEEL_ZOOM_SPEED;
    }

    if zoom_delta != 0.0 {
        **zoom_level =
            (zoom_level.0 + zoom_delta).clamp(CAMERA_ZOOM_RANGE.start, CAMERA_ZOOM_RANGE.end);

        for mut sprite in sprites.iter_mut() {
            sprite.custom_size = Some(Vec2 {
                x: zoom_level.0 * SPRITE_SIZE,
                y: zoom_level.0 * SPRITE_SIZE,
            });
        }
    }
}

/// Handle camera movement (panning) using the mouse or keyboard.
pub fn handle_camera(
    keyboard_input: Res<ButtonInput<KeyCode>>,
    mut mouse_button_events: EventReader<MouseButtonInput>,
    mut mouse_motion_events: EventReader<MouseMotion>,
    mut mouse_pressed: ResMut<MousePressed>,
    mut cameras: Query<&mut Transform, With<Camera2d>>,
) {
    // Update transforms.
    for mut camera_transform in cameras.iter_mut() {
        // Handle keyboard events.
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
pub fn handle_keymap(
    // mut contexts: EguiContexts,
    keyboard_input: Res<ButtonInput<KeyCode>>,
    mut windows: Query<&mut Window>,
) {
    if keyboard_input.pressed(KeyCode::KeyK) {
        let window_size = windows.single_mut().unwrap().size();

        // TODO separate window for layers and highlight active (HUD)
        // egui::Window::new("Keyboard shortcuts")
        //     .id(egui::Id::new("keymap"))
        //     .pivot(egui::Align2::CENTER_CENTER)
        //     .resizable(false)
        //     .movable(false)
        //     .collapsible(false)
        //     .fixed_pos(egui::Pos2::new(window_size.x / 2.0, window_size.y /
        // 2.0))     .show(contexts.ctx_mut(), |ui| {
        //         ui.label("W  -  Pan camera upwards");
        //         ui.label("A  -  Pan camera upwards");
        //         ui.label("S  -  Pan camera upwards");
        //         ui.label("D  -  Pan camera upwards");
        //         ui.label(">  -  Next layer");
        //         ui.label("<  -  Previous layer");
        //         ui.label("M  -  Meta layer");
        //         ui.label("F  -  Filesystem layer");
        //     });
    }
}

/// Switch to another layer from keypress
pub fn handle_layer_change(
    commands: Commands,
    // mut contexts: EguiContexts,
    keyboard_input: Res<ButtonInput<KeyCode>>,
    mut current_layer: ResMut<CurrentLayer>,
    time: Res<Time>,
    mut timer: ResMut<LayerChangeTimer>,
    mut windows: Query<&mut Window>,
) {
    // TODO don't allow change while timer is running

    #[cfg(feature = "layer-filesystem")]
    if keyboard_input.pressed(KeyCode::KeyF) {
        **current_layer = Layer::Filesystem;
        timer.reset();
    }
    #[cfg(feature = "layer-package")]
    if keyboard_input.pressed(KeyCode::KeyP) {
        **current_layer = Layer::Package;
        timer.reset();
    }
    #[cfg(feature = "layer-desktop")]
    if keyboard_input.pressed(KeyCode::KeyD) {
        **current_layer = Layer::Desktop;
        timer.reset();
    }

    // Now show the current layer for a few seconds
    if !timer.tick(time.delta()).finished() {
        let window_size = windows.single_mut().unwrap().size();
        // TODO util
        // egui::Window::new("Current layer")
        //     .id(egui::Id::new("current_layer"))
        //     .pivot(egui::Align2::CENTER_CENTER)
        //     .resizable(false)
        //     .movable(false)
        //     .collapsible(false)
        //     .title_bar(false)
        //     .fixed_pos(egui::Pos2::new(window_size.x / 2.0, window_size.y -
        // 30.0))     .show(contexts.ctx_mut(), |ui| {
        //         ui.label(format!("{:?}", **current_layer));
        //     });
    }
}
