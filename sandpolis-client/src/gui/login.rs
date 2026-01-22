use crate::gui::input::{LoginDialogState, LoginPhase};
use crate::gui::listeners::{DatabaseUpdate, DatabaseUpdateSender};
use bevy::prelude::*;
use sandpolis_database::{DataCreation, DataIdentifier};
use sandpolis_server::ServerUrl;
use sandpolis_server::client::SavedServerData;
use sandpolis_user::LoginPassword;
use sandpolis_user::messages::{LoginRequest, LoginResponse};
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
    pub task: bevy::tasks::Task<Result<(LoginResponse, sandpolis_core::InstanceId), String>>,
    pub server_url: ServerUrl,
    pub username: sandpolis_core::UserName,
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
        let username = match sandpolis_core::UserName::from_str(&login_state.username) {
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
                        _revision: sandpolis_database::DataRevision::Latest(0),
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
