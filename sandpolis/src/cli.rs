use crate::config::Configuration;
use anyhow::Result;
use clap::Parser;
use clap::Subcommand;
use colored::Colorize;
use sandpolis_instance::realm::RealmName;
use std::path::PathBuf;
use std::process::ExitCode;
use tracing::info;

#[cfg(feature = "client")]
use crate::InstanceState;
#[cfg(feature = "client")]
use sandpolis_client::cli::TargetArgs;

#[derive(Parser, Debug, Clone)]
#[clap(author, version, about = "Test")]
pub struct CommandLine {
    /// Configuration file path ($S7S_CONFIG)
    #[cfg(feature = "server")]
    #[clap(long)]
    pub config: Option<PathBuf>,

    #[clap(flatten)]
    pub instance: sandpolis_instance::cli::InstanceCommandLine,

    #[clap(flatten)]
    pub network: sandpolis_instance::network::cli::NetworkCommandLine,

    #[clap(flatten)]
    pub database: sandpolis_instance::database::cli::DatabaseCommandLine,

    #[clap(flatten)]
    pub realm: sandpolis_instance::realm::cli::RealmCommandLine,

    #[cfg(feature = "agent")]
    #[clap(flatten)]
    pub agent: sandpolis_agent::cli::AgentCommandLine,

    #[command(subcommand)]
    pub command: Option<Commands>,
}

/// Subcommands for `sandpolis agent`.
#[cfg(feature = "client")]
#[derive(Subcommand, Debug, Clone)]
pub enum AgentCommand {
    /// List all connected agents as JSON
    List,
    /// Restart (reboot) the target agent's device
    Restart {
        /// Target instance
        #[clap(long)]
        instance: sandpolis_instance::InstanceId,
    },
}

/// Subcommands for `sandpolis server`.
#[cfg(feature = "client")]
#[derive(Subcommand, Debug, Clone)]
pub enum ServerCommand {
    /// List all configured servers as JSON
    List,
}

#[derive(Subcommand, Debug, Clone)]
pub enum Commands {
    #[cfg(feature = "server")]
    /// Generate a new realm certificate for use with a client instance
    NewClientCert {
        /// Name of a realm that exists on the server
        #[clap(long, default_value = "default")]
        realm: RealmName,

        /// Output file path or none for STDOUT
        #[clap(long)]
        output: Option<PathBuf>,
    },

    #[cfg(feature = "server")]
    /// Generate a new realm certificate for use with an agent instance
    NewAgentCert {
        /// Name of a realm that exists on the server
        #[clap(long, default_value = "default")]
        realm: RealmName,

        /// Output file path or none for STDOUT
        #[clap(long)]
        output: Option<PathBuf>,
    },

    InstallCert {},

    /// Show versions of all installed layers
    About,

    /// Run the configuration LSP
    #[cfg(feature = "client")]
    Lsp,

    /// Manage agent instances
    #[cfg(feature = "client")]
    Agent {
        #[command(subcommand)]
        action: Option<AgentCommand>,
    },

    /// Manage server instances
    #[cfg(feature = "client")]
    Server {
        #[command(subcommand)]
        action: Option<ServerCommand>,
    },

    /// Manage probes
    #[cfg(all(feature = "client", feature = "layer-probe"))]
    Probe {
        #[clap(flatten)]
        target: TargetArgs,
    },

    /// Connect to remote desktop sessions
    #[cfg(all(feature = "client", feature = "layer-desktop"))]
    Desktop {
        #[command(subcommand)]
        action: Option<sandpolis_desktop::cli::DesktopCommand>,

        #[clap(flatten)]
        target: TargetArgs,
    },

    /// Connect to remote shell sessions
    #[cfg(all(feature = "client", feature = "layer-shell"))]
    Shell {
        #[clap(flatten)]
        target: TargetArgs,
    },

    /// Inspect agent health
    #[cfg(all(feature = "client", feature = "layer-health"))]
    Health {
        #[clap(flatten)]
        target: TargetArgs,
    },

    /// Inspect agent inventory
    #[cfg(all(feature = "client", feature = "layer-inventory"))]
    Inventory {
        #[clap(flatten)]
        target: TargetArgs,
    },

    /// Browse agent filesystems
    #[cfg(all(feature = "client", feature = "layer-filesystem"))]
    Filesystem {
        #[clap(flatten)]
        target: TargetArgs,
    },

    /// Manage accounts
    #[cfg(all(feature = "client", feature = "layer-account"))]
    Account {
        #[clap(flatten)]
        target: TargetArgs,
    },

    /// Manage cold snapshots
    #[cfg(all(feature = "client", feature = "layer-snapshot"))]
    Snapshot {
        #[clap(flatten)]
        target: TargetArgs,
    },

    /// Wake / power control (interactive TUI)
    #[cfg(feature = "client")]
    Wake {
        #[clap(flatten)]
        target: TargetArgs,
    },

    /// Inspect audit events (interactive TUI)
    #[cfg(all(feature = "client", feature = "layer-audit"))]
    Audit {
        #[clap(flatten)]
        target: TargetArgs,
    },

    /// Manage tunnels
    #[cfg(all(feature = "client", feature = "layer-tunnel"))]
    Tunnel {
        #[clap(flatten)]
        target: TargetArgs,
    },
}

impl Commands {
    /// Commands that run on their own without starting any instances or
    /// establishing a client connection.
    pub fn standalone(&self) -> bool {
        match self {
            #[cfg(feature = "server")]
            Commands::NewClientCert { .. } | Commands::NewAgentCert { .. } => true,
            Commands::InstallCert {} | Commands::About => true,
            #[cfg(feature = "client")]
            Commands::Lsp => true,
            #[allow(unreachable_patterns)]
            _ => false,
        }
    }

    #[allow(unused_variables)]
    /// Dispatch a [`standalone`](Self::standalone) command. These run without
    /// starting any instances or opening a client connection. Panics if called
    /// with a client subcommand (those go through `dispatch_client`).
    #[allow(unused_variables)]
    pub async fn dispatch_standalone(self, config: &Configuration) -> Result<ExitCode> {
        match self {
            #[cfg(feature = "client")]
            Commands::Lsp => {
                crate::lsp::run().await;
            }
            #[cfg(feature = "server")]
            Commands::NewClientCert { realm, output } => {
                use sandpolis_instance::realm::RealmClusterCert;

                let database = sandpolis_instance::database::DatabaseLayer::new(
                    config.database.clone(),
                    &crate::MODELS,
                )?;

                let db = database.realm(realm.parse()?)?;
                let r = db.r_transaction()?;

                let cluster_certs: Vec<RealmClusterCert> =
                    r.scan().primary()?.all()?.collect::<Result<Vec<_>, _>>()?;

                let Some(cluster_cert) = cluster_certs.first() else {
                    return Err(anyhow::anyhow!("No cluster cert found"));
                };

                let client_cert = cluster_cert.client_cert()?;

                if let Some(path) = output {
                    info!(path = %path.display(), "Writing endpoint certificate");
                    std::fs::write(path, &serde_json::to_vec(&client_cert)?)?;
                } else {
                    todo!()
                }
            }
            #[cfg(feature = "server")]
            Commands::NewAgentCert { .. } => todo!(),
            Commands::InstallCert {} => todo!(),
            Commands::About => {
                for line in fossable::sandpolis_word() {
                    println!("{line}");
                }
                println!("{} {}", "Layer".bold(), "Version".bold());
                for (layer, version) in crate::layers().iter() {
                    println!(
                        "{layer} {}.{}.{}",
                        version.major, version.minor, version.patch
                    );
                }
            }
            #[allow(unreachable_patterns)]
            _ => unreachable!("client subcommands are dispatched by dispatch_client"),
        }
        Ok(ExitCode::SUCCESS)
    }

    /// Dispatch a client subcommand: opens a focused TUI, or runs
    /// noninteractively (`--json`). Requires the live [`InstanceState`].
    #[cfg(feature = "client")]
    pub async fn dispatch_client(
        self,
        config: &Configuration,
        state: &crate::InstanceState,
    ) -> Result<ExitCode> {
        let fps = config.client.fps as f32;
        match self {
            Commands::Agent { action } => client::agent(action, fps).await,
            Commands::Server { action } => client::server(action, &state.server, fps).await,
            #[cfg(feature = "layer-probe")]
            Commands::Probe { target } => {
                sandpolis_probe::cli::dispatch(target, &state.probe, fps).await
            }
            #[cfg(feature = "layer-desktop")]
            Commands::Desktop { action, target } => {
                sandpolis_desktop::cli::dispatch(action, target, &state.desktop, fps).await
            }
            #[cfg(feature = "layer-shell")]
            Commands::Shell { target } => {
                sandpolis_shell::cli::dispatch(target, state.shell.clone(), fps).await
            }
            #[cfg(feature = "layer-health")]
            Commands::Health { target } => client::stub("health", target, fps).await,
            #[cfg(feature = "layer-inventory")]
            Commands::Inventory { target } => client::stub("inventory", target, fps).await,
            #[cfg(feature = "layer-filesystem")]
            Commands::Filesystem { target } => client::stub("filesystem", target, fps).await,
            #[cfg(feature = "layer-account")]
            Commands::Account { target } => client::stub("account", target, fps).await,
            #[cfg(feature = "layer-snapshot")]
            Commands::Snapshot { target } => client::stub("snapshot", target, fps).await,
            Commands::Wake { target } => client::stub("wake", target, fps).await,
            #[cfg(feature = "layer-audit")]
            Commands::Audit { target } => client::stub("audit", target, fps).await,
            #[cfg(feature = "layer-tunnel")]
            Commands::Tunnel { target } => client::stub("tunnel", target, fps).await,
            #[allow(unreachable_patterns)]
            _ => unreachable!("standalone commands are dispatched by dispatch_standalone"),
        }
    }
}

#[cfg(feature = "client")]
mod client {
    use super::*;
    use anyhow::{Context, bail};

    /// A not-yet-implemented client subcommand: opens a placeholder TUI, or
    /// prints an unimplemented JSON result for noninteractive callers.
    pub(super) async fn stub(name: &str, target: TargetArgs, fps: f32) -> Result<ExitCode> {
        if target.json {
            println!(
                "{}",
                serde_json::json!({"status": "unimplemented", "command": name})
            );
            return Ok(ExitCode::FAILURE);
        }
        sandpolis_client::tui::run_tui(fps, sandpolis_client::tui::PlaceholderPanel::new(name))
            .await?;
        Ok(ExitCode::SUCCESS)
    }

    pub(super) async fn agent(action: Option<AgentCommand>, fps: f32) -> Result<ExitCode> {
        match action {
            None => {
                let widget = crate::client::tui::agent_list::AgentListWidget::new()?;
                sandpolis_client::tui::run_tui(fps, widget).await?;
                Ok(ExitCode::SUCCESS)
            }
            Some(AgentCommand::List) => list_agents_json().await,
            Some(AgentCommand::Restart { instance }) => {
                // The agent reboot stream is not yet wired end-to-end; report
                // honestly rather than pretend success.
                println!(
                    "{}",
                    serde_json::json!({
                        "instance": instance.to_string(),
                        "status": "unimplemented",
                        "detail": "agent reboot stream is not yet wired",
                    })
                );
                Ok(ExitCode::FAILURE)
            }
        }
    }

    pub(super) async fn server(
        action: Option<ServerCommand>,
        server_layer: &sandpolis_server::ServerLayer,
        fps: f32,
    ) -> Result<ExitCode> {
        match action {
            None => {
                let widget = crate::client::tui::server_list::ServerListWidget::new(
                    server_layer.clone(),
                )?;
                sandpolis_client::tui::run_tui(fps, widget).await?;
                Ok(ExitCode::SUCCESS)
            }
            Some(ServerCommand::List) => list_servers_json(server_layer),
        }
    }

    fn list_servers_json(server_layer: &sandpolis_server::ServerLayer) -> Result<ExitCode> {
        let servers: Vec<_> = server_layer
            .servers
            .iter()
            .map(|resident| {
                let data = resident.read();
                serde_json::json!({
                    "address": data.address.to_string(),
                    "user": data.user.to_string(),
                })
            })
            .collect();

        println!("{}", serde_json::to_string_pretty(&servers)?);
        Ok(ExitCode::SUCCESS)
    }

    /// Print every known agent instance as JSON. Waits for the sync websocket
    /// to be established (CoLo mode starts the server asynchronously), then
    /// subscribes to the instance model and reads from the client database.
    async fn list_agents_json() -> Result<ExitCode> {
        use sandpolis_instance::InstanceLayerData;
        use sandpolis_instance::realm::RealmName;
        use std::time::Duration;

        info!("list_agents_json: waiting for server connection (up to 30s)");
        if sandpolis_client::sync::wait_for_connection(Duration::from_secs(30))
            .await
            .is_none()
        {
            bail!("no server connection");
        }
        info!("list_agents_json: connection established, subscribing");

        // Subscribe only after the connection is up so the call isn't a no-op.
        sandpolis_client::sync::subscribe(sandpolis_instance::instance_layer_model_id(), None);

        // Give the subscription a moment to deliver records.
        info!("list_agents_json: waiting 500ms for sync delivery");
        tokio::time::sleep(Duration::from_millis(500)).await;
        info!("list_agents_json: reading client database");

        let db =
            sandpolis_client::sync::client_database().context("client database unavailable")?;
        let realm = db.realm(RealmName::default())?;
        let r = realm.r_transaction()?;
        let all: Vec<InstanceLayerData> =
            r.scan().primary()?.all()?.collect::<Result<Vec<_>, _>>()?;

        let agents: Vec<_> = all
            .into_iter()
            .filter(|i| i.instance_id.is_agent())
            .map(|i| {
                serde_json::json!({
                    "instance_id": i.instance_id.to_string(),
                    "cluster_id": i.cluster_id.to_string(),
                    "os": i.os_info.to_string(),
                })
            })
            .collect();

        println!("{}", serde_json::to_string_pretty(&agents)?);
        Ok(ExitCode::SUCCESS)
    }
}
