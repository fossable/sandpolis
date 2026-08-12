use crate::RuntimeOptions;
use anyhow::Result;
use clap::Parser;
use clap::Subcommand;
use colored::Colorize;
use std::path::PathBuf;
use std::process::ExitCode;
use tracing::info;

#[cfg(feature = "client")]
use sandpolis_client::cli::TargetArgs;

#[derive(Parser, Debug, Clone)]
#[clap(author, version, about = "Test")]
pub struct CommandLine {
    /// Path to a `.realm` file declaring a realm this server should serve. May
    /// be given multiple times.
    ///
    /// The filename stem is the realm's name. A blank file means "generate a
    /// realm CA for me", which is then written back into the file — after which
    /// that file is the durable copy of the realm's trust root.
    ///
    /// Serving realms makes this the network's global stratum server, so it
    /// can't be combined with `--server`.
    #[cfg(feature = "server")]
    #[clap(long, value_name = "PATH", conflicts_with = "server")]
    pub realm: Vec<PathBuf>,

    /// Path to a `.server` file naming the server this instance connects to
    /// ($S7S_SERVER).
    ///
    /// The file carries the realm CA and this instance's own certificate, whose
    /// common name is the server's address. On a build with the server feature
    /// it also selects the local stratum, with that server as the upstream.
    #[clap(long, value_name = "PATH", env = "S7S_SERVER")]
    pub server: Option<PathBuf>,

    /// Address:port for this server to listen on.
    #[cfg(feature = "server")]
    #[clap(long, default_value = "0.0.0.0:8768")]
    pub listen: std::net::SocketAddr,

    /// The domain this instance belongs to.
    #[cfg(feature = "server")]
    #[clap(long)]
    pub domain: Option<String>,

    /// IP addresses denied access to the server, rejected before authentication
    /// runs. May be given multiple times.
    #[cfg(feature = "server")]
    #[clap(long, value_name = "IP")]
    pub blocked_ips: Vec<std::net::IpAddr>,

    /// Frame rate for the GUI and TUI.
    #[cfg(feature = "client")]
    #[clap(long, default_value_t = 30)]
    pub fps: u32,

    /// Directory where the admin socket is created.
    #[clap(long, default_value = "/tmp")]
    pub socket_dir: PathBuf,

    #[clap(flatten)]
    pub instance: sandpolis_instance::cli::InstanceCommandLine,

    #[clap(flatten)]
    pub database: sandpolis_instance::database::cli::DatabaseCommandLine,

    #[command(subcommand)]
    pub command: Option<Commands>,
}

impl CommandLine {
    /// The process-wide options these flags describe. What the `.server` file
    /// contributes is filled in by the caller, which is what loads it.
    pub fn options(&self) -> RuntimeOptions {
        RuntimeOptions {
            database: sandpolis_instance::database::config::DatabaseConfig {
                storage: self.database.data_dir.clone(),
                ephemeral: self.database.ephemeral,
                ..Default::default()
            },
            instance: sandpolis_instance::config::InstanceConfig {
                socket_directory: Some(self.socket_dir.clone()),
                #[cfg(feature = "server")]
                domain: self.domain.clone(),
                #[cfg(not(feature = "server"))]
                domain: None,
            },
            #[cfg(feature = "server")]
            listen: self.listen,
            #[cfg(feature = "server")]
            blocked_ips: self.blocked_ips.clone(),
            #[cfg(feature = "client")]
            fps: self.fps,
            #[cfg(feature = "agent")]
            poll: None,
            #[cfg(feature = "agent")]
            servers: Vec::new(),
            #[cfg(feature = "server")]
            realms: Vec::new(),
        }
    }
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

/// Flags shared by the two certificate-minting subcommands.
///
/// These issue from a realm's CA, which lives in its `.realm` file, so no
/// database is involved.
#[cfg(feature = "server")]
#[derive(clap::Args, Debug, Clone)]
pub struct NewCertArgs {
    /// Path to the `.realm` file whose CA signs the new certificate.
    #[clap(long, value_name = "PATH")]
    pub realm: PathBuf,

    /// Address the holder will use to reach the server, as `host` or
    /// `host:port`. Defaults to the realm file's own `address`.
    #[clap(long)]
    pub address: Option<String>,

    /// Run the agent in polling mode on this cron schedule (e.g. "0 */5 * * * *"
    /// to check in every five minutes) instead of staying continuously
    /// connected.
    #[clap(long)]
    pub poll: Option<String>,

    /// How long the agent stays connected during each polling check-in, in
    /// seconds.
    #[clap(long, default_value_t = sandpolis_instance::realm::config::PollConfig::default_timeout_secs())]
    pub poll_timeout: u64,

    /// Output file path, or none for STDOUT.
    #[clap(long)]
    pub output: Option<PathBuf>,
}

#[cfg(feature = "server")]
impl NewCertArgs {
    /// Load the realm and work out what URL the minted certificate should name.
    fn resolve(&self) -> Result<(sandpolis_instance::realm::RealmClusterCert, sandpolis_server::ServerUrl)> {
        use anyhow::{Context, anyhow, bail};
        use sandpolis_instance::realm::RealmClusterCert;

        let config = crate::config::RealmConfig::load(&self.realm)?;

        let Some(ca) = config.ca.as_ref() else {
            bail!(
                "{} declares no realm CA. Start the server once with \
                 `--realm {}` to generate one.",
                self.realm.display(),
                self.realm.display()
            );
        };
        let (cert, key) = ca.load_der(config.base_dir())?;
        let key = key.ok_or_else(|| {
            anyhow!(
                "{} has the realm CA certificate but not its private key, \
                 so it cannot issue new certificates",
                self.realm.display()
            )
        })?;

        let url = match self.address.as_ref() {
            Some(address) => {
                let mut url: sandpolis_server::ServerUrl = address
                    .parse()
                    .with_context(|| format!("Parsing address {address:?}"))?;
                url.realm = config.name.clone();
                url
            }
            None => config.server_url()?.ok_or_else(|| {
                anyhow!(
                    "{} declares no address; pass --address to say how the \
                     holder reaches this server",
                    self.realm.display()
                )
            })?,
        };

        Ok((
            RealmClusterCert {
                name: config.name.clone(),
                cert,
                key: Some(key),
                ..Default::default()
            },
            url,
        ))
    }

    fn poll(&self) -> Option<sandpolis_instance::realm::config::PollConfig> {
        self.poll
            .as_ref()
            .map(|schedule| sandpolis_instance::realm::config::PollConfig {
                schedule: schedule.clone(),
                timeout_secs: self.poll_timeout,
            })
    }

    /// Write `file` to `--output`, or to stdout when none was given.
    fn emit(&self, file: sandpolis_instance::realm::config::ServerCertFile) -> Result<()> {
        match self.output.as_ref() {
            Some(path) => {
                info!(path = %path.display(), "Writing endpoint certificate");
                file.write(path)?;
            }
            None => println!("{}", file.to_ron()?),
        }
        Ok(())
    }
}

#[derive(Subcommand, Debug, Clone)]
pub enum Commands {
    #[cfg(feature = "server")]
    /// Generate a `.server` file for a client instance
    NewClientCert {
        #[clap(flatten)]
        args: NewCertArgs,
    },

    #[cfg(feature = "server")]
    /// Generate a `.server` file for an agent instance
    NewAgentCert {
        #[clap(flatten)]
        args: NewCertArgs,
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

    /// Dispatch a [`standalone`](Self::standalone) command. These run without
    /// starting any instances or opening a client connection. Panics if called
    /// with a client subcommand (those go through `dispatch_client`).
    #[allow(unused_variables)]
    pub async fn dispatch_standalone(self, options: &RuntimeOptions) -> Result<ExitCode> {
        match self {
            #[cfg(feature = "client")]
            Commands::Lsp => {
                crate::lsp::run().await;
            }
            #[cfg(feature = "server")]
            Commands::NewClientCert { args } => {
                use sandpolis_instance::realm::config::ServerCertFile;

                let (ca, url) = args.resolve()?;
                args.emit(ServerCertFile::from_client(
                    &ca.client_cert(&url)?,
                    args.poll(),
                ))?;
            }
            #[cfg(feature = "server")]
            Commands::NewAgentCert { args } => {
                use sandpolis_instance::realm::config::ServerCertFile;

                let (ca, url) = args.resolve()?;
                args.emit(ServerCertFile::from_agent(
                    &ca.agent_cert(&url)?,
                    args.poll(),
                ))?;
            }
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
        options: &RuntimeOptions,
        state: &crate::InstanceState,
    ) -> Result<ExitCode> {
        let fps = options.fps as f32;
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

#[cfg(all(test, feature = "server"))]
mod test_stratum_args {
    use super::*;
    use sandpolis_instance::realm::RealmClusterCert;
    use sandpolis_server::ServerStratum;

    /// Write a `.server` file whose certificate names `url`, the way
    /// `new-client-cert` would.
    fn client_server_file(dir: &std::path::Path, url: &str) -> Result<PathBuf> {
        let url: sandpolis_server::ServerUrl = url.parse()?;
        let ca = RealmClusterCert::new(Default::default(), url.realm.clone())?;
        let path = dir.join("upstream.server");
        ca.client_cert(&url)?.write_server_file(&path, None)?;
        Ok(path)
    }

    /// Absent `--server`, this is the network's single global stratum server.
    #[test]
    fn no_server_file_means_global_stratum() -> Result<()> {
        let args = CommandLine::try_parse_from(["sandpolis"]).expect("bare invocation parses");
        assert_eq!(crate::stratum(&args)?, ServerStratum::Global);
        Ok(())
    }

    /// The `.server` file names the upstream and selects the local stratum; the
    /// address comes out of the certificate rather than a separate flag.
    #[test]
    fn server_file_means_local_stratum() -> Result<()> {
        let dir = tempfile::tempdir()?;
        let path = client_server_file(dir.path(), "gs.example.com:8768/default")?;

        let args =
            CommandLine::try_parse_from(["sandpolis", "--server", path.to_str().unwrap()])
                .expect("--server parses");

        let ServerStratum::Local { global } = crate::stratum(&args)? else {
            panic!("--server must select the local stratum");
        };
        assert_eq!(global.host, "gs.example.com");
        assert_eq!(global.port, 8768);
        Ok(())
    }

    /// A realm is something the global stratum server serves; an instance that
    /// attaches to one can't also be it.
    #[test]
    fn realm_conflicts_with_server() {
        let result = CommandLine::try_parse_from([
            "sandpolis",
            "--realm",
            "./default.realm",
            "--server",
            "./upstream.server",
        ]);
        assert!(
            result.is_err(),
            "a local stratum server must not serve realms of its own"
        );
    }

    /// A `.server` file that isn't there is a misconfiguration, not something to
    /// silently fall back from.
    #[test]
    fn missing_server_file_is_an_error() {
        let args = CommandLine::try_parse_from([
            "sandpolis",
            "--server",
            "/nonexistent/upstream.server",
        ])
        .expect("--server parses");
        assert!(crate::stratum(&args).is_err());
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
            .filter(|i| i._instance_id.is_agent())
            .map(|i| {
                serde_json::json!({
                    "instance_id": i._instance_id.to_string(),
                    "cluster_id": i.cluster_id.to_string(),
                    "os": i.os_info.to_string(),
                    "domain": i.domain,
                })
            })
            .collect();

        println!("{}", serde_json::to_string_pretty(&agents)?);
        Ok(ExitCode::SUCCESS)
    }
}
