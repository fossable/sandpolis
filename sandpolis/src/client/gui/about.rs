use bevy::prelude::*;
use bevy_egui::{EguiContexts, egui};
use std::time::{Duration, Instant};

/// Resource to track about screen state
#[derive(Resource)]
pub struct AboutScreenState {
    pub show: bool,
    /// Tracks clicks on the layer indicator for Easter egg
    pub logo_click_count: u8,
    pub last_logo_click: Option<Instant>,
}

impl Default for AboutScreenState {
    fn default() -> Self {
        Self {
            show: false,
            logo_click_count: 0,
            last_logo_click: None,
        }
    }
}

/// Marker component for the 3D logo entity
#[derive(Component)]
pub struct AboutLogo;

/// Marker component for the about screen's 3D camera
#[derive(Component)]
pub struct AboutCamera;

/// Easter egg: Check if the click sequence has timed out
/// Returns true if we should reset the counter
fn should_reset_clicks(last_click: Option<Instant>) -> bool {
    if let Some(last) = last_click {
        last.elapsed() > Duration::from_secs(2)
    } else {
        false
    }
}

/// System to handle the Easter egg for opening about screen
/// Easter egg: Triple-click on the layer indicator within 2 seconds
pub fn handle_about_easter_egg(mut about_state: ResMut<AboutScreenState>) {
    // Reset click counter if too much time has passed
    if should_reset_clicks(about_state.last_logo_click) {
        about_state.logo_click_count = 0;
        about_state.last_logo_click = None;
    }
}

/// Call this from the layer indicator when it's clicked
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
            info!("🥚 Easter egg activated! About screen opened.");
        }
    }
}

/// System to spawn the 3D logo when about screen is shown
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

/// System to rotate the logo slowly backwards
pub fn rotate_about_logo(time: Res<Time>, mut logo_query: Query<&mut Transform, With<AboutLogo>>) {
    for mut transform in logo_query.iter_mut() {
        // Rotate backwards (negative rotation around X axis) at a slow speed
        // Adjust the rotation speed here (currently ~10 degrees per second)
        transform.rotate_x(-0.5 * time.delta_secs());
    }
}

/// System to render the about screen UI with egui
pub fn render_about_screen(
    mut contexts: EguiContexts,
    mut about_state: ResMut<AboutScreenState>,
    windows: Query<&Window>,
) {
    if !about_state.show {
        return;
    }

    let Ok(ctx) = contexts.ctx_mut() else {
        return;
    };

    let Ok(window) = windows.single() else {
        return;
    };

    let window_size = Vec2::new(window.width(), window.height());

    egui::Window::new("About Sandpolis")
        .id(egui::Id::new("about_screen"))
        .pivot(egui::Align2::CENTER_CENTER)
        .resizable(false)
        .movable(false)
        .collapsible(false)
        .fixed_pos(egui::Pos2::new(window_size.x / 2.0, window_size.y / 2.0))
        .show(ctx, |ui| {
            ui.vertical_centered(|ui| {
                // Space for the 3D logo rendering at the top
                ui.add_space(320.0); // Height of the 3D viewport + padding

                ui.heading("🏰 SANDPOLIS");
                ui.add_space(8.0);

                ui.label("Security & Systems Management Platform");
                ui.add_space(16.0);

                ui.separator();
                ui.add_space(16.0);

                ui.label("A Rust-based platform for managing and monitoring");
                ui.label("distributed systems and security infrastructure.");
                ui.add_space(16.0);

                ui.separator();
                ui.add_space(16.0);

                // Version information
                ui.horizontal(|ui| {
                    ui.label("Version:");
                    ui.colored_label(egui::Color32::GRAY, env!("CARGO_PKG_VERSION"));
                });
                ui.add_space(8.0);

                // Project links
                ui.horizontal(|ui| {
                    ui.label("Project:");
                    ui.hyperlink_to(
                        "github.com/fossable/sandpolis",
                        "https://github.com/fossable/sandpolis",
                    );
                });
                ui.add_space(16.0);

                ui.separator();
                ui.add_space(16.0);

                // Close button
                if ui.button("Close").clicked() {
                    about_state.show = false;
                    about_state.logo_click_count = 0;
                    about_state.last_logo_click = None;
                }
            });
        });
}
