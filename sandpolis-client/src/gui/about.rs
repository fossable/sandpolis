//! About screen with Easter egg activation.
//!
//! The about screen can be activated by triple-clicking on the layer indicator.

use crate::gui::ui::panel::modal_scrim;
use crate::gui::ui::theme::{Role, Theme, ThemedBg, ThemedBorder};
use crate::gui::ui::widgets::{button, heading, muted, text};
use bevy::camera::ClearColorConfig;
use bevy::prelude::*;
use bevy_ui_widgets::Activate;
use std::time::{Duration, Instant};

/// Resource to track about screen state.
#[derive(Resource, Default)]
pub struct AboutScreenState {
    pub show: bool,
    /// Tracks clicks on the layer indicator for Easter egg.
    pub logo_click_count: u8,
    pub last_logo_click: Option<Instant>,
}

/// Marker component for the 3D logo entity.
#[derive(Component)]
pub struct AboutLogo;

/// Marker component for the about screen's 3D camera.
#[derive(Component)]
pub struct AboutCamera;

/// Easter egg: Check if the click sequence has timed out.
/// Returns true if we should reset the counter.
fn should_reset_clicks(last_click: Option<Instant>) -> bool {
    if let Some(last) = last_click {
        last.elapsed() > Duration::from_secs(2)
    } else {
        false
    }
}

/// System to handle the Easter egg for opening about screen.
/// Easter egg: Triple-click on the layer indicator within 2 seconds.
pub fn handle_about_easter_egg(mut about_state: ResMut<AboutScreenState>) {
    // Reset click counter if too much time has passed
    if should_reset_clicks(about_state.last_logo_click) {
        about_state.logo_click_count = 0;
        about_state.last_logo_click = None;
    }
}

/// Call this from the layer indicator when it's clicked.
pub fn register_logo_click(about_state: &mut AboutScreenState) {
    let now = Instant::now();

    // Reset if the last click was too long ago
    if should_reset_clicks(about_state.last_logo_click) {
        about_state.logo_click_count = 0;
    }

    about_state.logo_click_count += 1;
    about_state.last_logo_click = Some(now);

    // Easter egg activated! Open about screen on 3rd click
    if about_state.logo_click_count >= 3 {
        about_state.show = !about_state.show;
        about_state.logo_click_count = 0;
        about_state.last_logo_click = None;

        if about_state.show {
            info!("Easter egg activated! About screen opened.");
        }
    }
}

/// System to spawn the 3D logo when about screen is shown.
pub fn spawn_about_logo(
    mut commands: Commands,
    about_state: Res<AboutScreenState>,
    logo_query: Query<Entity, With<AboutLogo>>,
    camera_query: Query<Entity, With<AboutCamera>>,
    asset_server: Res<AssetServer>,
    mut materials: ResMut<Assets<StandardMaterial>>,
) {
    // Only proceed if state just changed
    if !about_state.is_changed() {
        return;
    }

    if about_state.show {
        // Only spawn if not already spawned
        if logo_query.is_empty() {
            // Load the STL mesh
            let mesh_handle: Handle<Mesh> = asset_server.load("sandpolis.stl");

            // Spawn the 3D logo
            commands.spawn((
                Mesh3d(mesh_handle),
                MeshMaterial3d(materials.add(StandardMaterial {
                    base_color: Color::srgb(0.784, 0.580, 0.216), // #c89437
                    metallic: 0.5,
                    perceptual_roughness: 0.3,
                    ..default()
                })),
                Transform::from_xyz(0.0, 2.0, 0.0).with_scale(Vec3::splat(0.01)),
                AboutLogo,
            ));

            // Spawn camera for 3D logo view (only if not already spawned)
            if camera_query.is_empty() {
                commands.spawn((
                    Camera3d::default(),
                    Transform::from_xyz(0.0, 2.0, 5.0)
                        .looking_at(Vec3::new(0.0, 2.0, 0.0), Vec3::Y),
                    Camera {
                        order: 1, // Render after the main 2D camera
                        // Don't clear: overlay the logo on the world rather than
                        // erasing it (the native about panel renders separately).
                        clear_color: ClearColorConfig::None,
                        ..default()
                    },
                    AboutCamera,
                ));

                // Add a light to illuminate the logo
                commands.spawn((
                    PointLight {
                        intensity: 2_000_000.0,
                        shadows_enabled: false,
                        ..default()
                    },
                    Transform::from_xyz(4.0, 8.0, 4.0),
                ));
            }
        }
    } else {
        // Despawn logo and camera when about screen is hidden
        for entity in logo_query.iter() {
            commands.entity(entity).despawn();
        }
        for entity in camera_query.iter() {
            commands.entity(entity).despawn();
        }
    }
}

/// System to rotate the logo slowly backwards.
pub fn rotate_about_logo(time: Res<Time>, mut logo_query: Query<&mut Transform, With<AboutLogo>>) {
    for mut transform in logo_query.iter_mut() {
        // Rotate backwards (negative rotation around X axis) at a slow speed
        // Adjust the rotation speed here (currently ~10 degrees per second)
        transform.rotate_x(-0.5 * time.delta_secs());
    }
}

/// Marker for the native about panel root.
#[derive(Component)]
pub struct AboutRoot;

/// Spawn/despawn the native about panel (the 3D logo renders above it).
pub fn manage_about_panel(
    mut commands: Commands,
    theme: Res<Theme>,
    about_state: Res<AboutScreenState>,
    root: Query<Entity, With<AboutRoot>>,
) {
    let exists = !root.is_empty();
    if about_state.show && !exists {
        commands
            .spawn((
                AboutRoot,
                crate::gui::ui::gating::BlocksWorldInput,
                GlobalZIndex(crate::gui::ui::z::CHROME),
                Node {
                    position_type: PositionType::Absolute,
                    left: Val::Px(0.0),
                    right: Val::Px(0.0),
                    bottom: Val::Px(40.0),
                    justify_content: JustifyContent::Center,
                    ..default()
                },
            ))
            .with_children(|root| {
                root.spawn((
                    Node {
                        flex_direction: FlexDirection::Column,
                        align_items: AlignItems::Center,
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
                    p.spawn(heading(&theme, "SANDPOLIS"));
                    p.spawn(muted(
                        &theme,
                        "Security & Systems Management Platform",
                        theme.metrics.font_md,
                    ));
                    p.spawn(text(
                        &theme,
                        format!("Version {}", env!("CARGO_PKG_VERSION")),
                        theme.metrics.font_sm,
                        Role::Text,
                    ));
                    p.spawn(muted(
                        &theme,
                        "github.com/fossable/sandpolis",
                        theme.metrics.font_sm,
                    ));
                    p.spawn(button(&theme, "Close")).observe(on_about_close);
                });
            });
    } else if !about_state.show && exists {
        for entity in &root {
            commands.entity(entity).despawn();
        }
    }
}

fn on_about_close(_activate: On<Activate>, mut about_state: ResMut<AboutScreenState>) {
    about_state.show = false;
    about_state.logo_click_count = 0;
    about_state.last_logo_click = None;
}
