use crate::{InstanceState, config::Configuration};
use anyhow::Result;
use bevy::{
    app::PluginGroup,
    audio::AudioSinkPlayback,
    color::palettes::basic::{BLUE, GRAY, WHITE},
    ecs::schedule::IntoScheduleConfigs,
    prelude::{
        App, AssetPlugin, AssetServer, AudioSink, BackgroundColor, Button, Camera2d, Changed,
        Commands, DefaultPlugins, Entity, Interaction, MessageReader, MonitorSelection, PostUpdate,
        Query, Res, ResMut, Startup, Timer, TimerMode, Update, Vec2, Vec3, Window, WindowPlugin,
        With, default, info,
    },
    window::{AppLifecycle, WindowMode},
};
use bevy_egui::{EguiPlugin, EguiPrimaryContextPass};
use bevy_rapier2d::prelude::{NoUserData, RapierConfiguration, RapierPhysicsPlugin};
use sandpolis_instance::LayerName;

// Import GUI types from sandpolis-client submodules
use sandpolis_client::gui::about::{
    AboutScreenState, handle_about_easter_egg, render_about_screen, rotate_about_logo,
    spawn_about_logo,
};
use sandpolis_client::gui::activity::{
    animate_activity_lines, cleanup_layer_activity_lines, despawn_completed_activity_lines,
    spawn_network_activity_lines, spawn_transfer_activity_lines, update_activity_line_positions,
};
use sandpolis_client::gui::controller::{
    NodeControllerState, close_controller_on_layer_change, handle_node_double_click,
    render_node_controller,
};
use sandpolis_client::gui::drag::{
    DragState, SelectionSet, disable_forces_while_dragging, handle_node_selection,
    render_selection_ui, start_node_drag, stop_node_drag, update_node_drag,
    update_selection_visuals,
};
use sandpolis_client::gui::edges::{
    render_edge_labels, render_edges, update_edge_visibility, update_edges_for_layer,
};
use sandpolis_client::gui::input::{
    CurrentLayer, HelpScreenState, LayerChangeTimer, LoginDialogState, MousePressed, PanningState,
    ZoomLevel, handle_camera, handle_keymap, handle_zoom,
};
use sandpolis_client::gui::layer_picker::{
    LayerPickerState, handle_layer_picker_toggle, render_layer_picker_button,
    render_layer_picker_panel,
};
use sandpolis_client::gui::layer_ui::{LayerIndicatorState, render_layer_indicator};
use sandpolis_client::gui::layer_visuals::{
    update_node_colors_for_layer, update_node_svgs_for_layer, update_node_visibility_for_layer,
};
use sandpolis_client::gui::layout::{
    LayoutConfig, LayoutState, apply_damping, apply_repulsion_forces, apply_spring_forces,
    check_stabilization,
};
use sandpolis_client::gui::listeners::{
    DatabaseUpdate, DatabaseUpdateChannel, DatabaseUpdateSender, setup_all_listeners,
};
use sandpolis_client::gui::login::{
    LoginOperation, check_saved_servers, handle_login_phase1, handle_login_phase2,
};
use sandpolis_client::gui::minimap::{MinimapViewport, render_minimap};
use sandpolis_client::gui::node::{NodeEntity, WorldView, scale_node_svgs, spawn_node};
use sandpolis_client::gui::node_picker::{
    NodePickerState, handle_node_picker_toggle, render_node_picker_panel,
};
use sandpolis_client::gui::preview::{render_node_previews, toggle_node_preview_visibility};
use sandpolis_client::gui::queries::{query_all_instances, query_instance_metadata};
use sandpolis_client::gui::responsive::update_responsive_ui;
use sandpolis_client::gui::theme::{
    CurrentTheme, ThemePickerState, apply_theme_to_egui, handle_theme_picker_toggle,
    initialize_theme, render_theme_picker,
};

// Re-export submodules for external access
pub use sandpolis_client::gui::controller;
pub use sandpolis_client::gui::layer_ext;
pub use sandpolis_client::gui::preview;

/// Initialize and start rendering the UI.
pub async fn main(config: Configuration, state: InstanceState) -> Result<()> {
    // Create channel for database updates from resident listeners
    let (db_update_tx, db_update_rx) = tokio::sync::mpsc::unbounded_channel();

    // Spawn background task for database listeners
    let network = state.network.clone();
    let db_update_tx_clone = db_update_tx.clone();
    tokio::spawn(async move {
        setup_all_listeners(network, db_update_tx_clone).await;
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
    // .add_plugins(RapierDebugRenderPlugin::default())
    .add_plugins(bevy_svg::prelude::SvgPlugin)
    .add_plugins(bevy_stl::StlPlugin)
    .add_plugins(EguiPlugin::default())
    .insert_resource(CurrentLayer(LayerName::from("Desktop")))
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
    .insert_resource(LayoutConfig::default())
    .insert_resource(LayoutState::default())
    .insert_resource(DragState::default())
    .insert_resource(NodeControllerState::default())
    .insert_resource(LayerPickerState::default())
    .insert_resource(NodePickerState::default())
    .insert_resource(HelpScreenState::default())
    .insert_resource(LoginDialogState::default())
    .insert_resource(LoginOperation::default())
    .insert_resource(SelectionSet::default())
    .insert_resource(AboutScreenState::default())
    .insert_resource(CurrentTheme::default())
    .insert_resource(ThemePickerState::default())
    .insert_resource(state.instance.clone())
    .insert_resource(state.network.clone())
    .insert_resource(state.server.clone())
    .insert_resource(config)
    .insert_resource(MousePressed(false))
    .insert_resource(PanningState::default())
    .add_systems(Startup, setup)
    .add_systems(Startup, install_egui_loaders)
    .add_systems(Startup, initialize_theme)
    .add_systems(
        Update,
        (
            // Theme system (runs first to ensure theme is applied)
            apply_theme_to_egui,
            // Input handling (desktop)
            #[cfg(not(target_os = "android"))]
            handle_zoom,
            #[cfg(not(target_os = "android"))]
            handle_camera,
            // Input handling (mobile)
            #[cfg(target_os = "android")]
            sandpolis_client::gui::input::handle_touch_camera,
            #[cfg(target_os = "android")]
            sandpolis_client::gui::input::handle_touch_zoom,
            handle_layer_picker_toggle,
            handle_node_picker_toggle,
            handle_theme_picker_toggle,
            button_handler,
            handle_lifetime,
            // Responsive UI updates
            update_responsive_ui,
            // Login systems
            check_saved_servers,
            handle_login_phase1,
            handle_login_phase2,
            // About screen systems
            handle_about_easter_egg,
            spawn_about_logo,
            rotate_about_logo,
        ),
    )
    .add_systems(
        Update,
        (
            // Selection systems (must run before drag)
            handle_node_selection,
            update_selection_visuals,
            // Drag systems
            start_node_drag,
            update_node_drag,
            stop_node_drag,
            disable_forces_while_dragging,
        ),
    )
    .add_systems(
        Update,
        (
            // Layout systems
            apply_repulsion_forces,
            apply_spring_forces,
            apply_damping,
            check_stabilization,
        ),
    )
    .add_systems(
        EguiPrimaryContextPass,
        (
            // UI rendering with egui
            render_minimap,
            render_layer_indicator,
            render_layer_picker_button,
            render_layer_picker_panel,
            render_node_picker_panel,
            render_node_previews,
            render_edge_labels,
            render_node_controller,
            render_selection_ui,
            render_about_screen,
            render_theme_picker,
            handle_keymap,
        ),
    )
    .add_systems(
        PostUpdate,
        (
            // Non-egui systems
            toggle_node_preview_visibility,
            // Edge systems
            render_edges,
            update_edges_for_layer,
            update_edge_visibility,
            // Layer visuals
            update_node_svgs_for_layer,
            update_node_colors_for_layer,
            update_node_visibility_for_layer,
            // Node SVG scaling - MUST run after update_node_svgs_for_layer
            scale_node_svgs.after(update_node_svgs_for_layer),
            // Controller systems
            handle_node_double_click,
            close_controller_on_layer_change,
            // Database updates
            process_database_updates,
        ),
    )
    .add_systems(
        Update,
        (
            // Activity line systems
            spawn_transfer_activity_lines,
            spawn_network_activity_lines,
            update_activity_line_positions,
            animate_activity_lines,
            despawn_completed_activity_lines,
            cleanup_layer_activity_lines,
        ),
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
    instance_layer: Res<sandpolis_instance::InstanceLayer>,
    network_layer: Res<sandpolis_instance::network::NetworkLayer>,
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
    if let Ok(instances) = query_all_instances(&instance_layer, &network_layer) {
        for instance_id in instances {
            if let Ok(metadata) = query_instance_metadata(instance_id) {
                // Spawn local instance at center, others at random positions
                let position = if metadata.instance_id == instance_layer.instance_id {
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
    node_query: Query<(Entity, &NodeEntity)>,
) {
    // Process all pending updates
    while let Ok(update) = update_channel.receiver.try_recv() {
        match update {
            DatabaseUpdate::InstanceAdded(instance_id) => {
                // Spawn new node at random position
                if let Ok(metadata) = query_instance_metadata(instance_id) {
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
            DatabaseUpdate::InstanceRemoved(instance_id) => {
                // Despawn node entity matching the instance_id
                for (entity, node_entity) in node_query.iter() {
                    if node_entity.instance_id == instance_id {
                        commands.entity(entity).despawn();
                        info!("Despawned node for removed instance: {}", instance_id);
                        break;
                    }
                }
            }
            DatabaseUpdate::NetworkTopologyChanged => {
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
    mut lifecycle_events: MessageReader<AppLifecycle>,
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
