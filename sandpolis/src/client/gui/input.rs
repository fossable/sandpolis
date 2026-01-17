use super::ZoomLevel;
use bevy::{
    input::{
        mouse::{MouseButtonInput, MouseMotion, MouseWheel},
    },
    prelude::*,
};
#[cfg(target_os = "android")]
use bevy::input::touch::TouchPhase;
use bevy_egui::{EguiContexts, egui};
use std::ops::Range;

#[derive(Resource)]
pub struct MousePressed(pub bool);

#[derive(Resource, Default)]
pub struct PanningState {
    /// Whether we're actively panning (started panning and haven't released yet)
    pub is_panning: bool,
}

#[derive(Resource, Deref, DerefMut)]
pub struct LayerChangeTimer(pub Timer);

#[derive(Resource)]
pub struct HelpScreenState {
    pub show: bool,
}

impl Default for HelpScreenState {
    fn default() -> Self {
        Self { show: false }
    }
}

#[derive(Resource)]
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

impl Default for LoginDialogState {
    fn default() -> Self {
        Self {
            show: false,
            phase: LoginPhase::default(),
            server_address: String::new(),
            username: String::new(),
            password: String::new(),
            otp: String::new(),
            error_message: None,
            loading: false,
        }
    }
}

/// Handle touch input for panning on mobile devices
#[cfg(target_os = "android")]
pub fn handle_touch_camera(
    mut contexts: EguiContexts,
    windows: Query<&Window>,
    mut touches: EventReader<TouchInput>,
    mut camera: Query<&mut Transform, With<Camera2d>>,
    mut last_position: Local<Option<Vec2>>,
) {
    // Don't handle touch if egui wants the input
    let Ok(ctx) = contexts.ctx_mut() else {
        return;
    };
    if ctx.wants_pointer_input() || ctx.is_pointer_over_area() {
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
    mut contexts: EguiContexts,
    mut touches: EventReader<TouchInput>,
    mut zoom_level: ResMut<ZoomLevel>,
    mut camera_query: Query<&mut Projection, With<Camera2d>>,
    mut touch_positions: Local<std::collections::HashMap<u64, Vec2>>,
    mut last_distance: Local<Option<f32>>,
) {
    // Don't handle zoom if egui wants the input
    let Ok(ctx) = contexts.ctx_mut() else {
        return;
    };
    if ctx.wants_pointer_input() || ctx.is_pointer_over_area() {
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
            let new_zoom = (zoom_level.0 * zoom_factor).clamp(CAMERA_ZOOM_RANGE.start, CAMERA_ZOOM_RANGE.end);

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
    mut contexts: EguiContexts,
    mut mouse_wheel_input: EventReader<MouseWheel>,
    mut zoom_level: ResMut<ZoomLevel>,
    mut camera_query: Query<(&mut Projection, &Transform), With<Camera2d>>,
    controller_state: Res<super::controller::NodeControllerState>,
) {
    let Ok(ctx) = contexts.ctx_mut() else {
        return;
    };

    // Check if we're hovering over a controller window (which may have scrollable content)
    // Controller windows have expandable_node set
    let over_controller = controller_state.expanded_node.is_some() && ctx.is_pointer_over_area();

    // Allow zoom if:
    // 1. Not over any egui area, OR
    // 2. Over a preview window (not a controller)
    // Preview windows are identified by having is_pointer_over_area() true but not being the controller
    let should_handle_zoom = if ctx.is_pointer_over_area() {
        // We're over some egui area
        // Only block zoom if we're over the controller window specifically
        !over_controller
    } else {
        // Not over any egui area - always handle zoom
        true
    };

    if !should_handle_zoom {
        // Clear the events so they don't accumulate
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
        if let Ok((mut projection, _transform)) = camera_query.single_mut() {
            if let Projection::Orthographic(ortho) = projection.as_mut() {
                ortho.scale = new_zoom;
            }
        }

        **zoom_level = new_zoom;
    }
}

/// Handle camera movement (panning) using the mouse or keyboard.
pub fn handle_camera(
    mut contexts: EguiContexts,
    keyboard_input: Res<ButtonInput<KeyCode>>,
    mut mouse_button_events: EventReader<MouseButtonInput>,
    mut mouse_motion_events: EventReader<MouseMotion>,
    mut mouse_pressed: ResMut<MousePressed>,
    mut panning_state: ResMut<PanningState>,
    mut cameras: Query<&mut Transform, With<Camera2d>>,
    drag_state: Res<super::drag::DragState>,
) {
    // Don't handle camera movement if egui wants the input
    let Ok(ctx) = contexts.ctx_mut() else {
        return;
    };
    let egui_wants_keyboard = ctx.wants_keyboard_input();
    let egui_wants_pointer = ctx.wants_pointer_input() || ctx.is_pointer_over_area();

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

/// Show a help window with keyboard shortcuts.
pub fn handle_keymap(
    mut contexts: EguiContexts,
    keyboard_input: Res<ButtonInput<KeyCode>>,
    mut help_state: ResMut<HelpScreenState>,
    mut login_state: ResMut<LoginDialogState>,
    windows: Query<&Window>,
) {
    let Ok(ctx) = contexts.ctx_mut() else {
        return;
    };

    // Don't handle hotkeys if egui wants keyboard input (e.g., typing in text fields)
    // But we still need to check for hotkeys to toggle dialogs off even when they're open
    let egui_wants_keyboard = ctx.wants_keyboard_input();

    // Toggle help screen with 'H' key (only when not typing)
    if !egui_wants_keyboard && keyboard_input.just_pressed(KeyCode::KeyH) {
        help_state.show = !help_state.show;
    }

    // Toggle login dialog with 'L' key (only when not typing)
    if !egui_wants_keyboard && keyboard_input.just_pressed(KeyCode::KeyL) {
        login_state.show = !login_state.show;
    }

    let Ok(window) = windows.single() else {
        return;
    };
    let window_size = Vec2::new(window.width(), window.height());

    let Ok(ctx) = contexts.ctx_mut() else {
        return;
    };

    // Render login dialog
    if login_state.show {
        let is_server_address_phase = matches!(login_state.phase, LoginPhase::ServerAddress);

        // Extract banner data to avoid borrow checker issues
        let (banner_message, banner_mfa, banner_maintenance) =
            if let LoginPhase::Credentials { banner } = &login_state.phase {
                (banner.message.clone(), banner.mfa, banner.maintenance)
            } else {
                (None, false, false)
            };

        egui::Window::new("Login to Server")
            .id(egui::Id::new("login_dialog"))
            .pivot(egui::Align2::CENTER_CENTER)
            .resizable(false)
            .movable(false)
            .collapsible(false)
            .fixed_pos(egui::Pos2::new(window_size.x / 2.0, window_size.y / 2.0))
            .show(ctx, |ui| {
                if is_server_address_phase {
                    // Phase 1: Server address input
                    ui.vertical_centered(|ui| {
                        ui.add_space(8.0);
                        ui.heading("🏰 SANDPOLIS");
                        ui.label("Security & Systems Management");
                        ui.add_space(8.0);
                    });

                    ui.separator();
                    ui.add_space(8.0);

                    // Server address field
                    ui.horizontal(|ui| {
                        ui.label("Server Address:");
                        ui.add_space(4.0);
                    });
                    ui.text_edit_singleline(&mut login_state.server_address);
                    ui.add_space(8.0);

                    // Show error message if any
                    if let Some(error) = &login_state.error_message {
                        ui.colored_label(egui::Color32::RED, error);
                        ui.add_space(8.0);
                    }

                    ui.separator();
                    ui.add_space(8.0);

                    // Buttons
                    ui.horizontal(|ui| {
                        ui.add_enabled_ui(!login_state.loading, |ui| {
                            if ui
                                .button(if login_state.loading {
                                    "Connecting..."
                                } else {
                                    "Connect"
                                })
                                .clicked()
                            {
                                login_state.loading = true;
                                login_state.error_message = None;
                                // Connection logic will be handled in a system
                            }
                        });

                        if ui.button("Cancel (L)").clicked() {
                            login_state.show = false;
                            login_state.error_message = None;
                            login_state.loading = false;
                        }
                    });
                } else {
                    // Phase 2: Credentials input with server banner
                    ui.vertical_centered(|ui| {
                        ui.add_space(8.0);

                        // TODO: Display banner image if available
                        // For now, just show the default heading
                        ui.heading("🏰 SANDPOLIS");

                        // Display server banner message if available
                        if let Some(message) = &banner_message {
                            ui.label(message);
                        }
                        ui.add_space(8.0);
                    });

                    if banner_maintenance {
                        ui.colored_label(
                            egui::Color32::YELLOW,
                            "⚠ Server is in maintenance mode. Only admin users can login.",
                        );
                        ui.add_space(8.0);
                    }

                    ui.separator();
                    ui.add_space(8.0);

                    // Show server address (read-only)
                    ui.horizontal(|ui| {
                        ui.label("Server:");
                        ui.colored_label(egui::Color32::GRAY, &login_state.server_address);
                    });
                    ui.add_space(8.0);

                    // Username field
                    ui.horizontal(|ui| {
                        ui.label("Username:");
                        ui.add_space(4.0);
                    });
                    ui.text_edit_singleline(&mut login_state.username);
                    ui.add_space(8.0);

                    // Password field
                    ui.horizontal(|ui| {
                        ui.label("Password:");
                        ui.add_space(4.0);
                    });
                    let password_edit =
                        egui::TextEdit::singleline(&mut login_state.password).password(true);
                    ui.add(password_edit);
                    ui.add_space(8.0);

                    // OTP field (shown based on MFA requirement)
                    if banner_mfa {
                        ui.horizontal(|ui| {
                            ui.label("OTP:");
                            ui.add_space(4.0);
                        });
                        ui.text_edit_singleline(&mut login_state.otp);
                        ui.add_space(8.0);
                    }

                    // Show error message if any
                    if let Some(error) = &login_state.error_message {
                        ui.colored_label(egui::Color32::RED, error);
                        ui.add_space(8.0);
                    }

                    ui.separator();
                    ui.add_space(8.0);

                    // Buttons
                    ui.horizontal(|ui| {
                        ui.add_enabled_ui(!login_state.loading, |ui| {
                            if ui
                                .button(if login_state.loading {
                                    "Logging in..."
                                } else {
                                    "Login"
                                })
                                .clicked()
                            {
                                login_state.loading = true;
                                login_state.error_message = None;
                                // Login logic will be handled in a system
                            }
                        });

                        if ui.button("Back").clicked() {
                            login_state.phase = LoginPhase::ServerAddress;
                            login_state.username.clear();
                            login_state.password.clear();
                            login_state.otp.clear();
                            login_state.error_message = None;
                            login_state.loading = false;
                        }

                        if ui.button("Cancel (L)").clicked() {
                            login_state.show = false;
                            login_state.phase = LoginPhase::ServerAddress;
                            login_state.username.clear();
                            login_state.password.clear();
                            login_state.otp.clear();
                            login_state.error_message = None;
                            login_state.loading = false;
                        }
                    });
                }
            });
    }

    // Render help screen
    if !help_state.show {
        return;
    }

    egui::Window::new("Keyboard Shortcuts")
        .id(egui::Id::new("keymap"))
        .pivot(egui::Align2::CENTER_CENTER)
        .resizable(false)
        .movable(false)
        .collapsible(false)
        .fixed_pos(egui::Pos2::new(window_size.x / 2.0, window_size.y / 2.0))
        .show(ctx, |ui| {
            ui.heading("Camera Controls");
            ui.separator();
            ui.label("Arrow Keys     Pan camera");
            ui.label("Mouse Drag     Pan camera");
            ui.label("Mouse Wheel    Zoom in/out");
            ui.add_space(8.0);

            ui.heading("Layer Switching");
            ui.separator();
            ui.label("Click Layer    Open layer switcher");
            #[cfg(feature = "layer-filesystem")]
            ui.label("F              Filesystem layer");
            #[cfg(feature = "layer-desktop")]
            ui.label("D              Desktop layer");
            ui.add_space(8.0);

            ui.heading("UI Controls");
            ui.separator();
            ui.label("L              Toggle login dialog");
            ui.label("N              Toggle node picker");
            ui.label("P              Toggle node previews");
            ui.label("T              Toggle theme picker");
            ui.label("H              Toggle this help screen");
            ui.label("Click node     Select and open controller");
            ui.label("Drag node      Move node position");
            ui.add_space(8.0);

            ui.separator();
            if ui.button("Close (H)").clicked() {
                help_state.show = false;
            }
        });
}
