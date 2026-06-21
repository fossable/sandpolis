use crate::config::Configuration;
use anyhow::Result;
use clap::Parser;
use clap::Subcommand;
use colored::Colorize;
use sandpolis_instance::realm::RealmName;
use std::path::PathBuf;
use std::process::ExitCode;
use tracing::info;

#[cfg(all(feature = "client", not(target_os = "android")))]
use crate::InstanceState;
#[cfg(all(feature = "client", not(target_os = "android")))]
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

    /// Global `--json` / `--instance` flags for client subcommands.
    #[cfg(all(feature = "client", not(target_os = "android")))]
    #[clap(flatten)]
    pub target: TargetArgs,

    #[command(subcommand)]
    pub command: Option<Commands>,
}

/// Interactive TUI / noninteractive operation on the target agent.
#[cfg(all(feature = "client", not(target_os = "android")))]
#[derive(Subcommand, Debug, Clone)]
pub enum AgentCommand {
    /// Restart (reboot) the target agent's device
    Restart,
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

    /// List agents, or act on one (interactive TUI unless `--json`)
    #[cfg(all(feature = "client", not(target_os = "android")))]
    Agent {
        #[command(subcommand)]
        action: Option<AgentCommand>,
    },

    /// Browse and manage saved servers (interactive TUI)
    #[cfg(all(feature = "client", not(target_os = "android")))]
    Server,

    /// Probe devices (interactive TUI)
    #[cfg(all(feature = "client", not(target_os = "android"), feature = "layer-probe"))]
    Probe,

    /// Desktop viewer and screenshots
    #[cfg(all(feature = "client", not(target_os = "android"), feature = "layer-desktop"))]
    Desktop {
        #[command(subcommand)]
        action: Option<sandpolis_desktop::cli::DesktopCommand>,
    },

    /// Interactive shell on the target agent
    #[cfg(all(feature = "client", not(target_os = "android"), feature = "layer-shell"))]
    Shell,

    /// Inspect agent health (interactive TUI)
    #[cfg(all(feature = "client", not(target_os = "android"), feature = "layer-health"))]
    Health,

    /// Inspect agent inventory (interactive TUI)
    #[cfg(all(feature = "client", not(target_os = "android"), feature = "layer-inventory"))]
    Inventory,

    /// Browse agent filesystems (interactive TUI)
    #[cfg(all(feature = "client", not(target_os = "android"), feature = "layer-filesystem"))]
    Filesystem,

    /// Manage accounts (interactive TUI)
    #[cfg(all(feature = "client", not(target_os = "android"), feature = "layer-account"))]
    Account,

    /// Manage snapshots (interactive TUI)
    #[cfg(all(feature = "client", not(target_os = "android"), feature = "layer-snapshot"))]
    Snapshot,

    /// Wake / power control (interactive TUI)
    #[cfg(all(feature = "client", not(target_os = "android"), feature = "layer-wake"))]
    Wake,

    /// Inspect audit events (interactive TUI)
    #[cfg(all(feature = "client", not(target_os = "android"), feature = "layer-audit"))]
    Audit,

    /// Manage tunnels (interactive TUI)
    #[cfg(all(feature = "client", not(target_os = "android"), feature = "layer-tunnel"))]
    Tunnel,
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
    #[cfg(all(feature = "client", not(target_os = "android")))]
    pub async fn dispatch_client(
        self,
        config: &Configuration,
        state: &crate::InstanceState,
        target: TargetArgs,
    ) -> Result<ExitCode> {
        let fps = config.client.fps as f32;
        match self {
            Commands::Agent { action } => client::agent(action, target, state, fps).await,
            Commands::Server => {
                let widget = crate::client::tui::server_list::ServerListWidget::new(
                    state.server.clone(),
                )?;
                sandpolis_client::tui::run_tui(fps, widget).await?;
                Ok(ExitCode::SUCCESS)
            }
            #[cfg(feature = "layer-probe")]
            Commands::Probe => sandpolis_probe::cli::dispatch(target, &state.probe, fps).await,
            #[cfg(feature = "layer-desktop")]
            Commands::Desktop { action } => {
                sandpolis_desktop::cli::dispatch(action, target, &state.desktop, fps).await
            }
            #[cfg(feature = "layer-shell")]
            Commands::Shell => {
                sandpolis_shell::cli::dispatch(target, state.shell.clone(), fps).await
            }
            #[cfg(feature = "layer-health")]
            Commands::Health => client::stub("health", target, fps).await,
            #[cfg(feature = "layer-inventory")]
            Commands::Inventory => client::stub("inventory", target, fps).await,
            #[cfg(feature = "layer-filesystem")]
            Commands::Filesystem => client::stub("filesystem", target, fps).await,
            #[cfg(feature = "layer-account")]
            Commands::Account => client::stub("account", target, fps).await,
            #[cfg(feature = "layer-snapshot")]
            Commands::Snapshot => client::stub("snapshot", target, fps).await,
            #[cfg(feature = "layer-wake")]
            Commands::Wake => client::stub("wake", target, fps).await,
            #[cfg(feature = "layer-audit")]
            Commands::Audit => client::stub("audit", target, fps).await,
            #[cfg(feature = "layer-tunnel")]
            Commands::Tunnel => client::stub("tunnel", target, fps).await,
            #[allow(unreachable_patterns)]
            _ => unreachable!("standalone commands are dispatched by dispatch_standalone"),
        }
    }
}

#[cfg(all(feature = "client", not(target_os = "android")))]
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

    pub(super) async fn agent(
        action: Option<AgentCommand>,
        target: TargetArgs,
        _state: &InstanceState,
        fps: f32,
    ) -> Result<ExitCode> {
        match action {
            None => {
                if target.json {
                    return list_agents_json().await;
                }
                let widget = crate::client::tui::agent_list::AgentListWidget::new()?;
                sandpolis_client::tui::run_tui(fps, widget).await?;
                Ok(ExitCode::SUCCESS)
            }
            Some(AgentCommand::Restart) => {
                if target.json {
                    let Some(instance) = target.instance else {
                        bail!("--instance is required with --json");
                    };
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
                    return Ok(ExitCode::FAILURE);
                }
                let widget = crate::client::tui::agent_list::AgentListWidget::new()?;
                sandpolis_client::tui::run_tui(fps, widget).await?;
                Ok(ExitCode::SUCCESS)
            }
        }
    }

    /// Print every known agent instance as JSON. Subscribes to the instance
    /// model, waits briefly for sync, then reads the client database.
    async fn list_agents_json() -> Result<ExitCode> {
        use sandpolis_instance::InstanceLayerData;
        use sandpolis_instance::realm::RealmName;
        use std::time::Duration;

        sandpolis_client::sync::subscribe(
            sandpolis_instance::instance_layer_model_id(),
            None,
        );

        if sandpolis_client::sync::wait_for_connection(Duration::from_secs(10))
            .await
            .is_none()
        {
            bail!("no server connection");
        }
        // Give the subscription a moment to deliver records.
        tokio::time::sleep(Duration::from_millis(500)).await;

        let db = sandpolis_client::sync::client_database()
            .context("client database unavailable")?;
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
