use crate::gui::input::{LoginDialogState, LoginPhase};
use crate::gui::listeners::{DatabaseUpdate, DatabaseUpdateSender};
use crate::gui::ui::panel::modal_scrim;
use crate::gui::ui::text_input::text_input;
use crate::gui::ui::theme::{Role, Theme, ThemedBg, ThemedBorder};
use crate::gui::ui::widgets::{button, heading, muted, text};
use bevy::input_focus::{FocusCause, InputFocus};
use bevy::prelude::*;
use bevy::text::EditableText;
use bevy_ui_widgets::Activate;
use sandpolis_instance::database::{DataCreation, DataIdentifier};
use sandpolis_server::ServerUrl;
use sandpolis_server::client::SavedServerData;
use sandpolis_server::login::{LoginPassword, LoginRequest, LoginResponse};
use std::str::FromStr;
use std::time::Duration;
use tracing::{debug, error, info};

/// Resource to track ongoing login operations
#[derive(Resource, Default)]
pub struct LoginOperation {
    pub phase1_handle: Option<LoginPhase1Handle>,
    pub phase2_handle: Option<LoginPhase2Handle>,
}

pub struct LoginPhase1Handle {
    pub task: bevy::tasks::Task<Result<sandpolis_server::ServerConnection, String>>,
}

pub struct LoginPhase2Handle {
    pub task: bevy::tasks::Task<Result<(LoginResponse, sandpolis_instance::InstanceId), String>>,
    pub server_url: ServerUrl,
    pub username: sandpolis_server::user::UserName,
}

/// System to handle phase 1: connecting to server and fetching banner
pub fn handle_login_phase1(
    mut login_state: ResMut<LoginDialogState>,
    mut login_operation: ResMut<LoginOperation>,
    server_layer: Res<sandpolis_server::ServerLayer>,
) {
    // Check if we need to start phase 1
    if matches!(login_state.phase, LoginPhase::ServerAddress)
        && login_state.loading
        && login_operation.phase1_handle.is_none()
    {
        // Parse server URL
        let server_url = match ServerUrl::from_str(&login_state.server_address) {
            Ok(url) => url,
            Err(e) => {
                login_state.error_message = Some(format!("Invalid server address: {}", e));
                login_state.loading = false;
                return;
            }
        };

        debug!(address = %server_url, "Starting phase 1: connecting to server");

        // Clone what we need for the async task
        let server_layer = server_layer.clone();

        // Spawn async task
        let task = bevy::tasks::AsyncComputeTaskPool::get().spawn(async move {
            server_layer
                .connect(server_url)
                .await
                .map_err(|e| format!("Connection failed: {}", e))
        });

        login_operation.phase1_handle = Some(LoginPhase1Handle { task });
    }

    // Check if phase 1 task is complete
    if let Some(mut handle) = login_operation.phase1_handle.take() {
        if let Some(result) = bevy::tasks::block_on(bevy::tasks::poll_once(&mut handle.task)) {
            match result {
                Ok(connection) => {
                    info!("Phase 1 complete: connected to server and fetched banner");
                    login_state.phase = LoginPhase::Credentials {
                        banner: connection.banner.clone(),
                    };
                    login_state.loading = false;
                    login_state.error_message = None;
                }
                Err(e) => {
                    error!(error = %e, "Phase 1 failed");
                    login_state.error_message = Some(e);
                    login_state.loading = false;
                }
            }
        } else {
            // Task still running, put handle back
            login_operation.phase1_handle = Some(handle);
        }
    }
}

/// System to handle phase 2: performing login with credentials
pub fn handle_login_phase2(
    mut login_state: ResMut<LoginDialogState>,
    mut login_operation: ResMut<LoginOperation>,
    server_layer: Res<sandpolis_server::ServerLayer>,
    db_update_sender: Res<DatabaseUpdateSender>,
) {
    // Check if we need to start phase 2
    if matches!(login_state.phase, LoginPhase::Credentials { .. })
        && login_state.loading
        && login_operation.phase2_handle.is_none()
    {
        // Parse server URL
        let server_url = match ServerUrl::from_str(&login_state.server_address) {
            Ok(url) => url,
            Err(e) => {
                login_state.error_message = Some(format!("Invalid server address: {}", e));
                login_state.loading = false;
                return;
            }
        };

        // Parse username
        let username = match sandpolis_server::user::UserName::from_str(&login_state.username) {
            Ok(u) => u,
            Err(e) => {
                login_state.error_message = Some(format!("Invalid username: {}", e));
                login_state.loading = false;
                return;
            }
        };

        debug!(username = %username, "Starting phase 2: logging in");

        // Clone what we need for the async task
        let server_layer_clone = server_layer.clone();
        let password = login_state.password.clone();
        let totp_token = if login_state.otp.is_empty() {
            None
        } else {
            Some(login_state.otp.clone())
        };

        // Clone values for use outside the async block
        let server_url_clone = server_url.clone();
        let username_clone = username.clone();

        // Spawn async task
        let task = bevy::tasks::AsyncComputeTaskPool::get().spawn(async move {
            // First connect to server
            let connection = server_layer_clone
                .connect(server_url_clone)
                .await
                .map_err(|e| format!("Connection failed: {}", e))?;

            // Get the server's instance ID from the connection
            let server_instance_id = connection.data.read().remote_instance;

            // Create login request with hashed password
            let login_request = LoginRequest {
                username: username_clone.clone(),
                password: LoginPassword::new(connection.cluster_id, &password),
                totp_token,
                lifetime: Some(Duration::from_secs(86400)), // 24 hours
            };

            // Perform login
            let response = connection
                .login(login_request)
                .await
                .map_err(|e| format!("Login request failed: {}", e))?;

            Ok((response, server_instance_id))
        });

        login_operation.phase2_handle = Some(LoginPhase2Handle {
            task,
            server_url,
            username,
        });
    }

    // Check if phase 2 task is complete
    if let Some(mut handle) = login_operation.phase2_handle.take() {
        if let Some(result) = bevy::tasks::block_on(bevy::tasks::poll_once(&mut handle.task)) {
            match result {
                Ok((LoginResponse::Ok(client_auth_token), server_instance_id)) => {
                    info!("Phase 2 complete: login successful");

                    // Save the server for future use
                    if let Err(e) = server_layer.save_server(SavedServerData {
                        address: handle.server_url,
                        token: client_auth_token,
                        user: handle.username,
                        _id: DataIdentifier::default(),
                        _revision: sandpolis_instance::database::DataRevision::Latest(0),
                        _creation: DataCreation::default(),
                    }) {
                        error!(error = %e, "Failed to save server");
                    }

                    // Notify the UI to spawn a new server node
                    if let Err(e) = db_update_sender
                        .sender
                        .send(DatabaseUpdate::InstanceAdded(server_instance_id))
                    {
                        error!(error = %e, "Failed to send InstanceAdded event");
                    }

                    // Close dialog and reset state
                    login_state.show = false;
                    login_state.phase = LoginPhase::ServerAddress;
                    login_state.server_address.clear();
                    login_state.username.clear();
                    login_state.password.clear();
                    login_state.otp.clear();
                    login_state.error_message = None;
                    login_state.loading = false;
                }
                Ok((LoginResponse::Denied, _)) => {
                    error!("Phase 2 failed: login denied");
                    login_state.error_message =
                        Some("Invalid username, password, or OTP".to_string());
                    login_state.loading = false;
                }
                Ok((LoginResponse::Expired, _)) => {
                    error!("Phase 2 failed: account expired");
                    login_state.error_message = Some("Account has expired".to_string());
                    login_state.loading = false;
                }
                Ok((LoginResponse::Invalid, _)) => {
                    error!("Phase 2 failed: invalid request");
                    login_state.error_message = Some("Invalid login request".to_string());
                    login_state.loading = false;
                }
                Err(e) => {
                    error!(error = %e, "Phase 2 failed");
                    login_state.error_message = Some(e);
                    login_state.loading = false;
                }
            }
        } else {
            // Task still running, put handle back
            login_operation.phase2_handle = Some(handle);
        }
    }
}

/// System to check for saved servers and skip to phase 2 if applicable
pub fn check_saved_servers(
    mut login_state: ResMut<LoginDialogState>,
    server_layer: Res<sandpolis_server::ServerLayer>,
) {
    // Only run when dialog is opened and in ServerAddress phase
    if !login_state.show || !matches!(login_state.phase, LoginPhase::ServerAddress) {
        return;
    }

    // Only run if server address is not empty
    if login_state.server_address.is_empty() {
        return;
    }

    // Parse server URL
    let server_url = match ServerUrl::from_str(&login_state.server_address) {
        Ok(url) => url,
        Err(_) => return, // Invalid URL, don't try to match
    };

    // Check if we have a saved server with this address
    for server_resident in server_layer.servers.iter() {
        let server = server_resident.read();
        if server.address == server_url {
            debug!(
                address = %server_url,
                "Found saved server, attempting auto-login"
            );

            // TODO: We could skip to phase 2 with the saved credentials,
            // or even attempt auto-login with the saved token.
            // For now, we'll just fetch the banner and pre-fill the username.
            login_state.username = server.user.to_string();

            // Trigger connection to fetch banner
            login_state.loading = true;
            return;
        }
    }
}

// ── Native login UI ──────────────────────────────────────────────────────────

/// Modal root; tracks which phase the form was built for so it can be rebuilt
/// when the login flow advances.
#[derive(Component)]
pub struct LoginRoot {
    pub phase: u8,
}

#[derive(Component)]
pub struct LoginServerInput;
#[derive(Component)]
pub struct LoginUserInput;
#[derive(Component)]
pub struct LoginPassInput;
#[derive(Component)]
pub struct LoginOtpInput;
#[derive(Component)]
pub struct LoginErrorText;

fn phase_id(phase: &LoginPhase) -> u8 {
    match phase {
        LoginPhase::ServerAddress => 0,
        LoginPhase::Credentials { .. } => 1,
    }
}

/// Spawn/despawn the login modal, rebuilding it when the phase changes.
pub fn manage_login(
    mut commands: Commands,
    theme: Res<Theme>,
    state: Res<LoginDialogState>,
    root: Query<(Entity, &LoginRoot)>,
    mut focus: ResMut<InputFocus>,
) {
    let existing = root.iter().next();
    if !state.show {
        if let Some((entity, _)) = existing {
            commands.entity(entity).despawn();
            focus.clear();
        }
        return;
    }

    let phase = phase_id(&state.phase);
    let needs_rebuild = match existing {
        Some((_, root)) => root.phase != phase,
        None => true,
    };
    if needs_rebuild {
        if let Some((entity, _)) = existing {
            commands.entity(entity).despawn();
        }
        spawn_login_modal(&mut commands, &theme, &state, phase);
    }
}

fn spawn_login_modal(commands: &mut Commands, theme: &Theme, state: &LoginDialogState, phase: u8) {
    commands
        .spawn((LoginRoot { phase }, modal_scrim()))
        .with_children(|scrim| {
            scrim
                .spawn((
                    Node {
                        flex_direction: FlexDirection::Column,
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
                    p.spawn(heading(theme, "Login to Server"));
                    if phase == 0 {
                        p.spawn(muted(theme, "Server address", theme.metrics.font_sm));
                        p.spawn((LoginServerInput, text_input(theme)));
                    } else {
                        if let LoginPhase::Credentials { banner } = &state.phase {
                            if let Some(message) = &banner.message {
                                p.spawn(text(theme, message.clone(), theme.metrics.font_md, Role::Text));
                            }
                            if banner.maintenance {
                                p.spawn(text(
                                    theme,
                                    "Server is in maintenance mode",
                                    theme.metrics.font_sm,
                                    Role::Warn,
                                ));
                            }
                        }
                        p.spawn(muted(
                            theme,
                            format!("Server: {}", state.server_address),
                            theme.metrics.font_sm,
                        ));
                        p.spawn(muted(theme, "Username", theme.metrics.font_sm));
                        p.spawn((LoginUserInput, text_input(theme)));
                        p.spawn(muted(theme, "Password", theme.metrics.font_sm));
                        p.spawn((LoginPassInput, text_input(theme)));
                        let mfa = matches!(&state.phase, LoginPhase::Credentials { banner } if banner.mfa);
                        if mfa {
                            p.spawn(muted(theme, "One-time code", theme.metrics.font_sm));
                            p.spawn((LoginOtpInput, text_input(theme)));
                        }
                    }
                    p.spawn((
                        LoginErrorText,
                        text(theme, String::new(), theme.metrics.font_sm, Role::Error),
                    ));
                    p.spawn(Node {
                        column_gap: Val::Px(8.0),
                        ..default()
                    })
                    .with_children(|row| {
                        let primary = if phase == 0 { "Connect" } else { "Login" };
                        row.spawn(button(theme, primary)).observe(on_login_primary);
                        if phase == 1 {
                            row.spawn(button(theme, "Back")).observe(on_login_back);
                        }
                        row.spawn(button(theme, "Cancel")).observe(on_login_cancel);
                    });
                });
        });
}

/// Focus the first field when a form is (re)built.
pub fn focus_login_input(
    server: Query<Entity, Added<LoginServerInput>>,
    user: Query<Entity, Added<LoginUserInput>>,
    mut focus: ResMut<InputFocus>,
) {
    if let Ok(entity) = server.single() {
        focus.set(entity, FocusCause::Navigated);
    } else if let Ok(entity) = user.single() {
        focus.set(entity, FocusCause::Navigated);
    }
}

/// Copy text-input contents into [`LoginDialogState`] for the login systems.
pub fn sync_login_inputs(
    mut state: ResMut<LoginDialogState>,
    server: Query<&EditableText, With<LoginServerInput>>,
    user: Query<&EditableText, With<LoginUserInput>>,
    pass: Query<&EditableText, With<LoginPassInput>>,
    otp: Query<&EditableText, With<LoginOtpInput>>,
) {
    if let Ok(input) = server.single() {
        let value = input.value().to_string();
        if state.server_address != value {
            state.server_address = value;
        }
    }
    if let Ok(input) = user.single() {
        let value = input.value().to_string();
        if state.username != value {
            state.username = value;
        }
    }
    if let Ok(input) = pass.single() {
        let value = input.value().to_string();
        if state.password != value {
            state.password = value;
        }
    }
    if let Ok(input) = otp.single() {
        let value = input.value().to_string();
        if state.otp != value {
            state.otp = value;
        }
    }
}

/// Mirror the login error message into the form's error label.
pub fn update_login_error(
    state: Res<LoginDialogState>,
    mut label: Query<&mut Text, With<LoginErrorText>>,
) {
    if let Ok(mut text) = label.single_mut() {
        let message = state.error_message.clone().unwrap_or_default();
        if text.0 != message {
            text.0 = message;
        }
    }
}

fn on_login_primary(_activate: On<Activate>, mut state: ResMut<LoginDialogState>) {
    state.loading = true;
    state.error_message = None;
}

fn on_login_back(_activate: On<Activate>, mut state: ResMut<LoginDialogState>) {
    state.phase = LoginPhase::ServerAddress;
    state.username.clear();
    state.password.clear();
    state.otp.clear();
    state.error_message = None;
    state.loading = false;
}

fn on_login_cancel(_activate: On<Activate>, mut state: ResMut<LoginDialogState>) {
    state.show = false;
    state.phase = LoginPhase::ServerAddress;
    state.username.clear();
    state.password.clear();
    state.otp.clear();
    state.error_message = None;
    state.loading = false;
}
