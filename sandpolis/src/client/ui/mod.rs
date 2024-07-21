use bevy::{
    color::palettes::basic::*,
    input::{
        gestures::RotationGesture,
        mouse::{MouseButtonInput, MouseMotion, MouseWheel},
        touch::TouchPhase,
    },
    prelude::*,
    window::{AppLifecycle, WindowMode},
};
use bevy_egui::{EguiContexts, EguiPlugin};
use bevy_rapier2d::prelude::*;
use std::ops::{Add, Range};

use crate::core::database::Database;

use self::node::spawn_node;

pub mod node;

#[derive(Resource)]
pub struct AppState {
    pub db: Database,
}

#[derive(Resource)]
struct MousePressed(bool);

/// Initialize and start rendering the UI.
pub fn run(state: AppState) {
    let mut app = App::new();
    app.add_plugins(
        DefaultPlugins
            .set(WindowPlugin {
                primary_window: Some(Window {
                    resizable: false,
                    mode: if cfg!(target_os = "android") {
                        WindowMode::BorderlessFullscreen
                    } else {
                        WindowMode::Windowed
                    },
                    // on iOS, gestures must be enabled.
                    // This doesn't work on Android
                    recognize_rotation_gesture: true,
                    ..default()
                }),
                ..default()
            })
            .disable::<bevy::log::LogPlugin>(),
    )
    .add_plugins(EguiPlugin)
    .add_plugins(RapierPhysicsPlugin::<NoUserData>::pixels_per_meter(100.0))
    .add_plugins(RapierDebugRenderPlugin::default())
    .insert_resource(state)
    .add_systems(Startup, setup)
    .add_systems(
        Update,
        (
            // touch_camera,
            move_camera,
            button_handler,
            handle_lifetime,
            // ui_example_system,
        ),
    );

    // MSAA makes some Android devices panic, this is under investigation
    // https://github.com/bevyengine/bevy/issues/8229
    #[cfg(target_os = "android")]
    app.insert_resource(Msaa::Off);

    app.run();
}

// fn ui_example_system(mut contexts: EguiContexts) {
//     egui::Window::new("Hello").show(contexts.ctx_mut(), |ui| {
//         ui.label("world");
//     });
// }

fn touch_camera(
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

// Processes input related to camera movement.
fn move_camera(
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

fn setup(
    mut commands: Commands,
    mut rapier_config: ResMut<RapierConfiguration>,
    asset_server: Res<AssetServer>,
    state: Res<AppState>,
    mut contexts: EguiContexts,
) {
    rapier_config.gravity = Vec2::ZERO;
    commands.spawn(Camera2dBundle::default());
    commands.insert_resource(MousePressed(false));

    // Spawn the local client
    spawn_node(
        &asset_server,
        &mut commands,
        &mut contexts,
        state.db.metadata.id,
        state.db.metadata.os_info.os_type(),
    );
}

fn button_handler(
    mut interaction_query: Query<
        (&Interaction, &mut BackgroundColor),
        (Changed<Interaction>, With<Button>),
    >,
) {
    for (interaction, mut color) in &mut interaction_query {
        match *interaction {
            Interaction::Pressed => {
                *color = BLUE.into();
            }
            Interaction::Hovered => {
                *color = GRAY.into();
            }
            Interaction::None => {
                *color = WHITE.into();
            }
        }
    }
}

// Pause audio when app goes into background and resume when it returns.
// This is handled by the OS on iOS, but not on Android.
fn handle_lifetime(
    mut lifecycle_events: EventReader<AppLifecycle>,
    music_controller: Query<&AudioSink>,
) {
    let Ok(music_controller) = music_controller.get_single() else {
        return;
    };

    for event in lifecycle_events.read() {
        match event {
            AppLifecycle::Idle | AppLifecycle::WillSuspend | AppLifecycle::WillResume => {}
            AppLifecycle::Suspended => music_controller.pause(),
            AppLifecycle::Running => music_controller.play(),
        }
    }
}
