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

use crate::core::{database::Database, Layer};

use self::{input::MousePressed, node::spawn_node};

pub mod input;
pub mod node;

#[derive(Resource)]
pub struct AppState {
    pub db: Database,
}

#[derive(Resource, Deref, DerefMut)]
pub struct CurrentLayer(Layer);

#[derive(Resource, Deref, DerefMut)]
pub struct LayerChangeTimer(Timer);

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
    .insert_resource(CurrentLayer(Layer::Desktop))
    .insert_resource(LayerChangeTimer(Timer::from_seconds(3.0, TimerMode::Once)))
    .insert_resource(state)
    .insert_resource(MousePressed(false))
    .add_systems(Startup, setup)
    .add_systems(
        Update,
        (
            // touch_camera,
            self::input::handle_camera,
            self::input::handle_keymap,
            self::input::handle_layer_change,
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

fn setup(
    mut commands: Commands,
    mut rapier_config: ResMut<RapierConfiguration>,
    asset_server: Res<AssetServer>,
    state: Res<AppState>,
    mut contexts: EguiContexts,
) {
    rapier_config.gravity = Vec2::ZERO;
    commands.spawn(Camera2dBundle::default());

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
