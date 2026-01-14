use self::{
    about::AboutScreenState,
    components::{
        DatabaseUpdateChannel, DatabaseUpdateSender, LayerIndicatorState, MinimapViewport,
        SelectionSet, WorldView,
    },
    input::{HelpScreenState, LayerChangeTimer, LoginDialogState, MousePressed},
    node::spawn_node,
    theme::CurrentTheme,
};
#[cfg(feature = "layer-desktop")]
use crate::Layer;
use crate::{InstanceState, config::Configuration};
use anyhow::Result;
use bevy::{
    color::palettes::basic::*,
    ecs::event::EventReader,
    prelude::*,
    window::{AppLifecycle, WindowMode},
};
use bevy_egui::{EguiPlugin, EguiPrimaryContextPass};
use bevy_rapier2d::prelude::*;

pub mod about;
pub mod activity;
pub mod components;
pub mod controller;
pub mod drag;
pub mod edges;
pub mod input;
pub mod layer_switcher;
pub mod layer_ui;
pub mod layer_visuals;
pub mod layout;
pub mod listeners;
pub mod login;
pub mod minimap;
pub mod node;
pub mod node_picker;
pub mod preview;
pub mod queries;
pub mod responsive;
pub mod theme;

/// Only one layer can be selected at a time.
#[derive(Resource, Deref, DerefMut, Debug)]
pub struct CurrentLayer(Layer);

#[derive(Resource, Deref, DerefMut)]
pub struct ZoomLevel(f32);

/// Initialize and start rendering the UI.
pub async fn main(config: Configuration, state: InstanceState) -> Result<()> {
    // Create channel for database updates from resident listeners
    let (db_update_tx, db_update_rx) = tokio::sync::mpsc::unbounded_channel();

    // Spawn background task for database listeners
    let state_clone = state.clone();
    let db_update_tx_clone = db_update_tx.clone();
    tokio::spawn(async move {
        listeners::setup_all_listeners(state_clone, db_update_tx_clone).await;
    });

    let mut app = App::new();

    app.add_plugins(
        DefaultPlugins
            .set(AssetPlugin {
                file_path: "../sandpolis-client/assets".to_string(),
                ..default()
            })
            .set(WindowPlugin {
                primary_window: Some(Window {
                    resizable: true,
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
    .add_plugins(RapierPhysicsPlugin::<NoUserData>::pixels_per_meter(100.0))
    .add_plugins(RapierDebugRenderPlugin::default())
    .add_plugins(bevy_svg::prelude::SvgPlugin)
    .add_plugins(bevy_stl::StlPlugin)
    .add_plugins(EguiPlugin::default())
    .insert_resource(CurrentLayer(Layer::Desktop))
    .insert_resource(ZoomLevel(1.0))
    .insert_resource(LayerChangeTimer(Timer::from_seconds(3.0, TimerMode::Once)))
    .insert_resource(MinimapViewport::default())
    .insert_resource(LayerIndicatorState::default())
    .insert_resource(DatabaseUpdateChannel {
        receiver: db_update_rx,
    })
    .insert_resource(DatabaseUpdateSender {
        sender: db_update_tx.clone(),
    })
    .insert_resource(layout::LayoutConfig::default())
    .insert_resource(layout::LayoutState::default())
    .insert_resource(drag::DragState::default())
    .insert_resource(controller::NodeControllerState::default())
    .insert_resource(layer_switcher::LayerSwitcherState::default())
    .insert_resource(node_picker::NodePickerState::default())
    .insert_resource(HelpScreenState::default())
    .insert_resource(LoginDialogState::default())
    .insert_resource(login::LoginOperation::default())
    .insert_resource(SelectionSet::default())
    .insert_resource(AboutScreenState::default())
    .insert_resource(CurrentTheme::default())
    .insert_resource(state)
    .insert_resource(config)
    .insert_resource(MousePressed(false))
    .add_systems(Startup, setup)
    .add_systems(Startup, install_egui_loaders)
    .add_systems(Startup, theme::initialize_theme)
    .add_systems(
        Update,
        (
            // Theme system (runs first to ensure theme is applied)
            theme::apply_theme_to_egui,
            // Input handling (desktop)
            #[cfg(not(target_os = "android"))]
            self::input::handle_zoom,
            #[cfg(not(target_os = "android"))]
            self::input::handle_camera,
            // Input handling (mobile)
            #[cfg(target_os = "android")]
            self::input::handle_touch_camera,
            #[cfg(target_os = "android")]
            self::input::handle_touch_zoom,
            layer_switcher::handle_layer_switcher_toggle,
            node_picker::handle_node_picker_toggle,
            button_handler,
            handle_lifetime,
            // Responsive UI updates
            responsive::update_responsive_ui,
            // Login systems
            login::check_saved_servers,
            login::handle_login_phase1,
            login::handle_login_phase2,
            // About screen systems
            about::handle_about_toggle,
            about::spawn_about_logo,
            about::rotate_about_logo,
        ),
    )
    .add_systems(
        Update,
        (
            // Selection systems (must run before drag)
            drag::handle_node_selection,
            drag::update_selection_visuals,
            // Drag systems
            drag::start_node_drag,
            drag::update_node_drag,
            drag::stop_node_drag,
            drag::disable_forces_while_dragging,
        ),
    )
    .add_systems(
        Update,
        (
            // Layout systems
            layout::apply_repulsion_forces,
            layout::apply_spring_forces,
            layout::apply_damping,
            layout::check_stabilization,
        ),
    )
    .add_systems(
        EguiPrimaryContextPass,
        (
            // UI rendering with egui
            minimap::render_minimap,
            layer_ui::render_layer_indicator,
            layer_switcher::render_layer_switcher_button,
            layer_switcher::render_layer_switcher_panel,
            node_picker::render_node_picker_panel,
            preview::render_node_previews,
            edges::render_edge_labels,
            controller::render_node_controller,
            drag::render_selection_ui,
            about::render_about_screen,
            self::input::handle_keymap,
        ),
    )
    .add_systems(
        PostUpdate,
        (
            // Non-egui systems
            preview::toggle_node_preview_visibility,
            // Edge systems
            edges::render_edges,
            edges::update_edges_for_layer,
            edges::update_edge_visibility,
            // Layer visuals
            layer_visuals::update_node_svgs_for_layer,
            layer_visuals::update_node_colors_for_layer,
            // Node SVG scaling - MUST run after update_node_svgs_for_layer
            node::scale_node_svgs.after(layer_visuals::update_node_svgs_for_layer),
            // Controller systems
            controller::handle_node_double_click,
            controller::close_controller_on_layer_change,
            // Database updates
            process_database_updates,
        ),
    )
    .add_systems(
        Update,
        (
            // Activity line systems
            activity::spawn_transfer_activity_lines,
            activity::spawn_network_activity_lines,
            activity::update_activity_line_positions,
            activity::animate_activity_lines,
            activity::despawn_completed_activity_lines,
            activity::cleanup_layer_activity_lines,
        ),
    );

    #[cfg(feature = "layer-desktop")]
    app.add_systems(
        Update,
        sandpolis_desktop::client::gui::handle_layer
            .run_if(|current_layer: Res<CurrentLayer>| **current_layer == Layer::Desktop),
    );

    app.run();
    Ok(())
}

/// Install egui image loaders for SVG support (runs once at startup)
fn install_egui_loaders(mut contexts: bevy_egui::EguiContexts) {
    if let Ok(ctx) = contexts.ctx_mut() {
        egui_extras::install_image_loaders(ctx);
    }
}

fn setup(
    mut commands: Commands,
    mut rapier_config: Query<&mut RapierConfiguration>,
    asset_server: Res<AssetServer>,
    state: Res<InstanceState>,
) {
    // Set zero gravity for floating nodes
    for mut rapier_config in &mut rapier_config {
        rapier_config.gravity = Vec2::ZERO;
    }

    // Spawn main camera with WorldView marker
    commands.spawn((
        Camera2d,
        WorldView,
        // MSAA makes some Android devices panic, this is under investigation
        // https://github.com/bevyengine/bevy/issues/8229
        #[cfg(target_os = "android")]
        Msaa::Off,
    ));

    // Query database for initial instances and spawn nodes
    if let Ok(instances) = queries::query_all_instances(&state) {
        for instance_id in instances {
            if let Ok(metadata) = queries::query_instance_metadata(&state, instance_id) {
                // Spawn local instance at center, others at random positions
                let position = if metadata.instance_id == state.instance.instance_id {
                    Some(Vec3::ZERO) // Center of screen
                } else {
                    None // Random position
                };

                spawn_node(
                    &asset_server,
                    &mut commands,
                    metadata.instance_id,
                    metadata.os_type,
                    metadata.is_server,
                    position,
                );
            }
        }
    }
}

/// Process database updates from resident listeners
fn process_database_updates(
    mut update_channel: ResMut<DatabaseUpdateChannel>,
    mut commands: Commands,
    asset_server: Res<AssetServer>,
    state: Res<InstanceState>,
) {
    // Process all pending updates
    while let Ok(update) = update_channel.receiver.try_recv() {
        match update {
            components::DatabaseUpdate::InstanceAdded(instance_id) => {
                // Spawn new node at random position
                if let Ok(metadata) = queries::query_instance_metadata(&state, instance_id) {
                    spawn_node(
                        &asset_server,
                        &mut commands,
                        metadata.instance_id,
                        metadata.os_type,
                        metadata.is_server,
                        None, // Random position for dynamically added nodes
                    );
                }
            }
            components::DatabaseUpdate::InstanceRemoved(_instance_id) => {
                // TODO: Despawn node entity
                // Need to query for entity with matching instance_id and despawn it
            }
            components::DatabaseUpdate::NetworkTopologyChanged => {
                // Edges will be rebuilt by update_edges_for_layer system
            }
            _ => {
                // Other updates will be handled in later phases
            }
        }
    }
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
    let Ok(music_controller) = music_controller.single() else {
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
