use crate::InstanceState;

#[cfg(feature = "client")]
pub mod gui;

#[cfg(all(feature = "client", not(target_os = "android")))]
pub mod tui;

/// Bring up everything a client needs and run `command`: the GUI in the
/// foreground, or a subcommand's focused TUI.
///
/// A client keeps nothing across runs — its database is in memory and it owns it
/// outright — so all it needs from the command line is the server to attach to.
#[cfg(not(target_os = "android"))]
pub async fn start(command: crate::cli::Commands) -> anyhow::Result<std::process::ExitCode> {
    let args = command.client_args().cloned().unwrap_or_default();
    let options = args.options();

    // Clients read no config file, so the `.server` file is the only way to
    // point one at a server without going through the GUI login dialog.
    let endpoint = crate::load_server_file(args.server.as_deref())?;
    let endpoint_certs = endpoint
        .as_ref()
        .map(|(cert, _)| cert.clone())
        .into_iter()
        .collect();

    let state = crate::endpoint_state(&options, endpoint_certs).await?;

    if let Some((cert, _)) = endpoint {
        spawn_server_connection(state.clone(), cert.url()?);
    }

    // The GUI establishes the sync websocket itself; a subcommand needs it
    // before its view can show anything.
    if let crate::cli::Commands::Client { .. } = command {
        tracing::info!("Starting Sandpolis client");
        gui::main(options, state).await?;
        return Ok(std::process::ExitCode::SUCCESS);
    }

    spawn_client_sync(state.clone());
    command.dispatch_client(&options, &state).await
}

/// Establish the websocket to the first available server and install it for DB
/// sync. Runs in the background until a server connection exists (the user logs
/// in), then opens the websocket once and hands it to `sandpolis_client::sync`.
pub fn spawn_client_sync(state: InstanceState) {
    let server = state.server.clone();
    let network = state.network.clone();
    let database = state.network.database.clone();
    let instance = state.instance.clone();

    // Start surfacing notifications before any of them can arrive. This is the
    // one path both the GUI and the subcommand TUIs take, so it covers every
    // way the client runs; without a GUI to toast into, delivery falls through
    // to the operating system.
    if let Err(e) = sandpolis_client::notification::watch(&database) {
        tracing::warn!(error = %e, "Notifications will not be surfaced");
    }

    tokio::spawn(async move {
        loop {
            let conns = server.server_connections();
            tracing::debug!(
                count = conns.len(),
                "spawn_client_sync: checking server connections"
            );
            for connection in conns {
                let has_ws = connection.inner.read().unwrap().is_some();
                tracing::debug!(has_ws, "spawn_client_sync: connection slot");
                if has_ws {
                    continue;
                }
                tracing::info!("spawn_client_sync: opening websocket");
                match connection.open_websocket(&network, &instance).await {
                    Ok(ic) => {
                        tracing::info!("Established sync websocket to server");
                        // Register every server for routing; the first also becomes
                        // the primary connection backing `sync::connection()`.
                        sandpolis_client::sync::register_connection(
                            connection.url.clone(),
                            ic.clone(),
                        );
                        sandpolis_client::sync::init(ic, database.clone());

                        // Domains group nodes in the world view, so they're
                        // needed for as long as the GUI runs. Subscribing is a
                        // no-op before `init`, hence here rather than at
                        // startup.
                        sandpolis_client::sync::subscribe(
                            sandpolis_instance::domain::domain_model_id(),
                            None,
                        );

                        // Notifications are wanted for as long as the client
                        // runs, not only while some view is open, so this
                        // subscription is standing rather than opened by a
                        // panel.
                        sandpolis_client::notification::subscribe();

                        // Which instances exist, and which of them are up. The
                        // client holds no connection to an agent, so both are
                        // things it can only learn by replication — and it needs
                        // them for as long as there's a world view to draw.
                        sandpolis_client::sync::subscribe(
                            sandpolis_instance::instance_layer_model_id(),
                            None,
                        );
                        sandpolis_client::sync::subscribe(
                            sandpolis_instance::network::liveness::liveness_model_id(),
                            None,
                        );
                    }
                    Err(e) => {
                        tracing::info!(error = %e, "Failed to open sync websocket");
                    }
                }
            }
            tokio::time::sleep(std::time::Duration::from_millis(200)).await;
        }
    });
}

/// Open (and retain) a connection to `url`, retrying until it succeeds.
///
/// Clients read no config file, so the `--server` file is how a standalone
/// client is pointed at a server without going through the GUI login dialog.
/// Either stratum works — the client addresses agents by id and the servers
/// route to them.
fn spawn_server_connection(state: InstanceState, url: sandpolis_server::ServerUrl) {
    let server = state.server.clone();

    // Surface the server in the (database-backed) saved server list so it
    // appears in the TUI, deduplicating so it isn't re-added every run. On a
    // read-only replica this list can't be written, which is harmless — the
    // connection itself still comes up.
    let already_saved = server.servers.iter().any(|s| s.read().address == url);
    if !already_saved {
        use sandpolis_instance::database::{DataCreation, DataIdentifier, DataRevision};
        if let Err(e) = server.save_server(sandpolis_server::client::SavedServerData {
            address: url.clone(),
            token: sandpolis_server::user::ClientAuthToken(String::new()),
            user: sandpolis_server::user::UserName::default(),
            _id: DataIdentifier::default(),
            _revision: DataRevision::Latest(0),
            _creation: DataCreation::default(),
        }) {
            tracing::debug!(error = %e, "Failed to save local server entry");
        }
    }

    tokio::spawn(async move {
        loop {
            tracing::debug!(%url, "Attempting server connection");
            match server.connect(url.clone()).await {
                Ok(connection) => {
                    server
                        .outbound
                        .write()
                        .unwrap()
                        .push(std::sync::Arc::new(connection));
                    tracing::info!(%url, "Connected to server");
                    return;
                }
                Err(e) => {
                    tracing::debug!(error = %e, %url, "Server not ready yet, retrying in 2s");
                    tokio::time::sleep(std::time::Duration::from_secs(2)).await;
                }
            }
        }
    });
}
