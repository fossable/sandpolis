use anyhow::Result;
use bevy::{
    color::palettes::basic::*,
    prelude::*,
    window::{AppLifecycle, WindowMode},
};
use bevy_egui::{EguiContexts, EguiPlugin};
use bevy_rapier2d::prelude::*;

use crate::{
    core::{database::Database, Layer},
    CommandLine,
};

use self::{
    input::{LayerChangeTimer, MousePressed},
    node::spawn_node,
};

pub mod input;
pub mod layer;
pub mod node;

#[derive(Resource)]
pub struct AppState {
    pub db: Option<Database>,
}

#[derive(Resource, Deref, DerefMut)]
pub struct CurrentLayer(Layer);

#[derive(Resource, Deref, DerefMut)]
pub struct ZoomLevel(f32);

/// Initialize and start rendering the UI.
pub async fn run(args: CommandLine) -> Result<()> {
    let mut state = AppState {
        #[cfg(feature = "local-database")]
        db: Some(Database::new(None, "test", "test").await?),
        #[cfg(not(feature = "local-database"))]
        db: None,
    };

    let mut app = App::new();
    app.add_plugins(
        DefaultPlugins
            .set(WindowPlugin {
                primary_window: Some(Window {
                    resizable: false,
                    mode: if cfg!(target_os = "android") {
                        WindowMode::BorderlessFullscreen(MonitorSelection::Current)
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
    .insert_resource(ZoomLevel(1.0))
    .insert_resource(LayerChangeTimer(Timer::from_seconds(3.0, TimerMode::Once)))
    .insert_resource(state)
    .insert_resource(args)
    .insert_resource(MousePressed(false))
    .add_systems(Startup, setup)
    .add_systems(
        Update,
        (
            // touch_camera,
            self::input::handle_zoom,
            self::input::handle_camera,
            self::input::handle_keymap,
            self::input::handle_layer_change,
            button_handler,
            handle_lifetime,
        ),
    );

    #[cfg(feature = "layer-desktop")]
    app.add_systems(
        Update,
        self::layer::desktop::handle_layer.run_if(self::layer::desktop::check_layer_active),
    );

    // MSAA makes some Android devices panic, this is under investigation
    // https://github.com/bevyengine/bevy/issues/8229
    #[cfg(target_os = "android")]
    app.insert_resource(Msaa::Off);

    app.run();
    Ok(())
}

fn setup(
    mut commands: Commands,
    mut rapier_config: Query<&mut RapierConfiguration>,
    asset_server: Res<AssetServer>,
    state: Res<AppState>,
    contexts: EguiContexts,
) {
    for mut rapier_config in &mut rapier_config {
        rapier_config.gravity = Vec2::ZERO;
    }
    commands.spawn(Camera2dBundle::default());

    // Spawn the local client
    // spawn_node(
    //     &asset_server,
    //     &mut commands,
    //     state.db.metadata.id,
    //     state.db.metadata.os_info.os_type(),
    // );
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

// #[derive(Component)]
// pub struct ConnectionTask(pub Task<()>);

// pub fn prepare_connection_tasks(
//     args: Res<CommandLine>,
//     pool: Res<AsyncComputeTaskPool>,
//     mut commands: Commands,
// ) {
//     while let Some(key) = queue.queue.pop() {
//         if let Some(entity) = chunks.entity(key) {
//             let task = pool.spawn(async move {
//                 // TODO no references
//                 // Create server connection(s)
//                 for server in args.server.unwrap_or(Vec::new()) {
//                     state.db.add_server(&server, "test", "test").await?;
//                 }
//             });

//             cmds.entity(entity).insert(ConnectionTask { 0: task });
//         }
//     }
// }

// pub fn apply_connection_tasks(
//     mut query: Query<(Entity, &mut ConnectionTask)>,
//     mut commands: Commands,
// ) {
//     query.for_each_mut(|(entity, mut task)| {
//         if futures::block_on(futures::poll_once(&mut task.0)) {
//             cmds.entity(entity).remove::<GenTask>();
//         }
//         return;
//     });
// }
