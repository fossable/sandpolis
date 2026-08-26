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
    #[clap(flatten)]
    pub instance: sandpolis_instance::cli::InstanceCommandLine,

    /// A process is exactly one instance, so which one it is has to be said.
    #[command(subcommand)]
    pub command: Commands,
}

/// Flags for the server daemon (`sandpolis server`).
#[cfg(feature = "server")]
#[derive(clap::Args, Debug, Clone)]
pub struct ServerArgs {
    /// Directory holding this server's database and the realm configs it
    /// serves ($S7S_DATA).
    ///
    /// Every `*.realm.ron` file in the directory declares one realm, named
    /// after the part of the filename before the suffix. A blank file means
    /// "generate a realm CA for me", which is then written back into the file —
    /// after which that file is the durable copy of the realm's trust root. A
    /// global stratum server that finds no realm config creates
    /// `default.realm.ron`.
    ///
    /// Without this flag the server is ephemeral: its databases are kept in
    /// memory and nothing survives the process.
    #[clap(long, value_name = "DIR", env = "S7S_DATA")]
    pub data: Option<PathBuf>,

    /// Path to a realm cert naming this server's upstream ($S7S_REALM).
    ///
    /// Having one is what puts this server in the local stratum: it carries the
    /// realm CA and this server's own certificate, whose common name is the
    /// upstream's address. A local stratum server serves no realms of its own.
    #[clap(long, value_name = "PATH", env = "S7S_REALM")]
    pub realm: Option<PathBuf>,

    /// Address:port for this server to listen on.
    #[clap(long, default_value = "0.0.0.0:8768")]
    pub listen: std::net::SocketAddr,

    /// IP addresses denied access to the server, rejected before authentication
    /// runs. May be given multiple times.
    #[clap(long, value_name = "IP")]
    pub blocked_ips: Vec<std::net::IpAddr>,
}

/// Flags for the agent daemon (`sandpolis agent`).
#[cfg(feature = "agent")]
#[derive(clap::Args, Debug, Clone, Default)]
pub struct AgentArgs {
    /// Directory holding this agent's database ($S7S_DATA).
    ///
    /// Without this flag the agent is ephemeral: its database is kept in memory
    /// and nothing survives the process.
    #[clap(long, value_name = "DIR", env = "S7S_DATA")]
    pub data: Option<PathBuf>,

    /// Path to the realm cert naming the server this agent attaches to
    /// ($S7S_REALM).
    ///
    /// It carries the realm CA and this agent's own certificate, whose common
    /// name is the server's address.
    #[clap(long, value_name = "PATH", env = "S7S_REALM")]
    pub realm: Option<PathBuf>,

    /// Cron expression putting the agent in polling mode, e.g. `0 */5 * * * *`
    /// for a check-in every five minutes ($S7S_POLL).
    ///
    /// Without it the agent stays connected continuously.
    #[clap(long, value_name = "CRON", env = "S7S_POLL")]
    pub poll: Option<String>,

    /// How long each check-in window stays open, in seconds ($S7S_POLL_TIMEOUT).
    ///
    /// Only meaningful alongside `--poll`.
    #[clap(
        long,
        value_name = "SECONDS",
        env = "S7S_POLL_TIMEOUT",
        default_value_t = sandpolis_agent::PollConfig::default_timeout_secs(),
    )]
    pub poll_timeout: u64,
}

/// Flags for the client, which every client subcommand shares.
#[cfg(feature = "client")]
#[derive(clap::Args, Debug, Clone, Default)]
pub struct ClientArgs {
    /// Path to a realm cert naming the server this client connects to
    /// ($S7S_REALM).
    ///
    /// It carries the realm CA and this client's own certificate, whose common
    /// name is the server's address. Without it, realm certs are read from
    /// `--data` (or the default state directory), and if none are found there
    /// either, the GUI asks for one to be picked interactively.
    #[clap(long, value_name = "PATH", env = "S7S_REALM")]
    pub realm: Option<PathBuf>,

    /// Directory holding this client's database and its realm certs
    /// ($S7S_DATA).
    ///
    /// Every `*.realm.pem` file in the directory attaches this client to the
    /// server that cert names, which is how a client is configured without
    /// naming a file on the command line. Without this flag the client is
    /// ephemeral: its database is kept in memory and nothing survives the
    /// process.
    #[clap(long, value_name = "DIR", env = "S7S_DATA")]
    pub data: Option<PathBuf>,

    /// Frame rate for the GUI and TUI ($S7S_FPS).
    #[clap(long, default_value_t = 30, env = "S7S_FPS")]
    pub fps: u32,
}

#[cfg(feature = "server")]
impl ServerArgs {
    /// The process-wide options these flags describe. The realms and stratum
    /// are filled in by the caller, which is what reads the `--data` directory
    /// and the realm cert.
    pub fn options(&self) -> RuntimeOptions {
        RuntimeOptions {
            database: sandpolis_instance::database::config::DatabaseConfig {
                storage: self.data.clone(),
                ..Default::default()
            },
            instance_type: sandpolis_instance::InstanceType::Server,
            listen: self.listen,
            blocked_ips: self.blocked_ips.clone(),
            ..Default::default()
        }
    }
}

#[cfg(feature = "agent")]
impl AgentArgs {
    /// The process-wide options these flags describe. What the realm cert
    /// contributes is filled in by the caller, which is what loads it.
    pub fn options(&self) -> RuntimeOptions {
        RuntimeOptions {
            database: sandpolis_instance::database::config::DatabaseConfig {
                storage: self.data.clone(),
                ..Default::default()
            },
            instance_type: sandpolis_instance::InstanceType::Agent,
            poll: self
                .poll
                .clone()
                .map(|schedule| sandpolis_agent::PollConfig {
                    schedule,
                    timeout_secs: self.poll_timeout,
                }),
            ..Default::default()
        }
    }
}

#[cfg(feature = "client")]
impl ClientArgs {
    /// The process-wide options these flags describe.
    pub fn options(&self) -> RuntimeOptions {
        RuntimeOptions {
            database: sandpolis_instance::database::config::DatabaseConfig {
                storage: self.data.clone(),
                ..Default::default()
            },
            instance_type: sandpolis_instance::InstanceType::Client,
            fps: self.fps,
            ..Default::default()
        }
    }
}

/// Subcommands for `sandpolis agents`, which manages agent instances.
#[cfg(feature = "client")]
#[derive(Subcommand, Debug, Clone)]
pub enum AgentsCommand {
    /// List all connected agents
    List {
        /// Emit machine-readable JSON instead of opening a TUI
        #[clap(long)]
        json: bool,

        #[clap(flatten)]
        client: ClientArgs,
    },
    /// Restart (reboot) the target agent's device
    Restart {
        /// Target instance
        #[clap(long)]
        instance: sandpolis_instance::InstanceId,

        #[clap(flatten)]
        client: ClientArgs,
    },
}

/// Subcommands for `sandpolis servers`, which manages configured servers.
#[cfg(feature = "client")]
#[derive(Subcommand, Debug, Clone)]
pub enum ServersCommand {
    /// List all configured servers
    List {
        /// Emit machine-readable JSON instead of opening a TUI
        #[clap(long)]
        json: bool,

        #[clap(flatten)]
        client: ClientArgs,
    },
}

#[derive(Subcommand, Debug, Clone)]
pub enum Commands {
    /// Show versions of all installed layers
    About,

    /// Run the configuration LSP
    #[cfg(feature = "client")]
    Lsp {
        #[clap(flatten)]
        args: crate::lsp::LspArgs,
    },

    /// Run the agent daemon
    #[cfg(feature = "agent")]
    Agent {
        #[clap(flatten)]
        args: AgentArgs,
    },

    /// Run the server daemon
    #[cfg(feature = "server")]
    Server {
        #[clap(flatten)]
        args: ServerArgs,
    },

    /// Manage agent instances
    #[cfg(feature = "client")]
    Agents {
        #[command(subcommand)]
        action: AgentsCommand,
    },

    /// Manage configured servers
    #[cfg(feature = "client")]
    Servers {
        #[command(subcommand)]
        action: ServersCommand,
    },

    /// Run the client in the foreground
    #[cfg(feature = "client")]
    Client {
        #[clap(flatten)]
        client: ClientArgs,
    },

    /// Manage probes
    #[cfg(all(feature = "client", feature = "probe"))]
    Probe {
        #[clap(flatten)]
        target: TargetArgs,

        #[clap(flatten)]
        client: ClientArgs,
    },

    /// Connect to remote desktop sessions
    #[cfg(all(feature = "client", feature = "desktop"))]
    Desktop {
        #[command(subcommand)]
        action: Option<sandpolis_desktop::cli::DesktopCommand>,

        #[clap(flatten)]
        target: TargetArgs,

        #[clap(flatten)]
        client: ClientArgs,
    },

    /// Connect to remote shell sessions
    #[cfg(all(feature = "client", feature = "shell"))]
    Shell {
        #[clap(flatten)]
        target: TargetArgs,

        #[clap(flatten)]
        client: ClientArgs,
    },

    /// Inspect agent health
    #[cfg(all(feature = "client", feature = "health"))]
    Health {
        #[clap(flatten)]
        target: TargetArgs,

        #[clap(flatten)]
        client: ClientArgs,
    },

    /// Inspect agent inventory
    #[cfg(all(feature = "client", feature = "inventory"))]
    Inventory {
        #[clap(flatten)]
        target: TargetArgs,

        #[clap(flatten)]
        client: ClientArgs,
    },

    /// Browse agent filesystems
    #[cfg(all(feature = "client", feature = "filesystem"))]
    Filesystem {
        #[clap(flatten)]
        target: TargetArgs,

        #[clap(flatten)]
        client: ClientArgs,
    },

    /// Manage accounts
    #[cfg(all(feature = "client", feature = "account"))]
    Account {
        #[clap(flatten)]
        target: TargetArgs,

        #[clap(flatten)]
        client: ClientArgs,
    },

    /// Manage cold snapshots
    #[cfg(all(feature = "client", feature = "snapshot"))]
    Snapshot {
        #[clap(flatten)]
        target: TargetArgs,

        #[clap(flatten)]
        client: ClientArgs,
    },

    /// Wake / power control (interactive TUI)
    #[cfg(feature = "client")]
    Wake {
        #[clap(flatten)]
        target: TargetArgs,

        #[clap(flatten)]
        client: ClientArgs,
    },

    /// Inspect audit events (interactive TUI)
    #[cfg(all(feature = "client", feature = "audit"))]
    Audit {
        #[clap(flatten)]
        target: TargetArgs,

        #[clap(flatten)]
        client: ClientArgs,
    },

    /// Manage tunnels
    #[cfg(all(feature = "client", feature = "tunnel"))]
    Tunnel {
        #[clap(flatten)]
        target: TargetArgs,

        #[clap(flatten)]
        client: ClientArgs,
    },
}

impl Commands {
    /// Commands that run on their own without starting any instances or
    /// establishing a client connection.
    pub fn standalone(&self) -> bool {
        match self {
            Commands::About => true,
            #[cfg(feature = "client")]
            Commands::Lsp { .. } => true,
            #[allow(unreachable_patterns)]
            _ => false,
        }
    }

    /// The flags for the server daemon, when that's what this command is.
    #[cfg(feature = "server")]
    pub fn server_daemon(&self) -> Option<&ServerArgs> {
        match self {
            Commands::Server { args } => Some(args),
            #[allow(unreachable_patterns)]
            _ => None,
        }
    }

    /// The flags for the agent daemon, when that's what this command is.
    #[cfg(feature = "agent")]
    pub fn agent_daemon(&self) -> Option<&AgentArgs> {
        match self {
            Commands::Agent { args } => Some(args),
            #[allow(unreachable_patterns)]
            _ => None,
        }
    }

    /// The client flags this command carries, which is also how a command says
    /// it runs as a client at all. Daemons and standalone commands have none.
    #[cfg(feature = "client")]
    pub fn client_args(&self) -> Option<&ClientArgs> {
        match self {
            Commands::Client { client } => Some(client),
            Commands::Agents { action } => match action {
                AgentsCommand::List { client, .. } => Some(client),
                AgentsCommand::Restart { client, .. } => Some(client),
            },
            Commands::Servers { action } => match action {
                ServersCommand::List { client, .. } => Some(client),
            },
            #[cfg(feature = "probe")]
            Commands::Probe { client, .. } => Some(client),
            #[cfg(feature = "desktop")]
            Commands::Desktop { client, .. } => Some(client),
            #[cfg(feature = "shell")]
            Commands::Shell { client, .. } => Some(client),
            #[cfg(feature = "health")]
            Commands::Health { client, .. } => Some(client),
            #[cfg(feature = "inventory")]
            Commands::Inventory { client, .. } => Some(client),
            #[cfg(feature = "filesystem")]
            Commands::Filesystem { client, .. } => Some(client),
            #[cfg(feature = "account")]
            Commands::Account { client, .. } => Some(client),
            #[cfg(feature = "snapshot")]
            Commands::Snapshot { client, .. } => Some(client),
            Commands::Wake { client, .. } => Some(client),
            #[cfg(feature = "audit")]
            Commands::Audit { client, .. } => Some(client),
            #[cfg(feature = "tunnel")]
            Commands::Tunnel { client, .. } => Some(client),
            #[allow(unreachable_patterns)]
            _ => None,
        }
    }

    /// Whether this command draws in the terminal it was started from, which is
    /// every client subcommand except the GUI. Those get a log file instead of
    /// stderr so the logs don't corrupt the view.
    pub fn owns_terminal(&self) -> bool {
        #[cfg(feature = "client")]
        {
            !matches!(self, Commands::Client { .. }) && self.client_args().is_some()
        }
        #[cfg(not(feature = "client"))]
        {
            false
        }
    }

    /// Dispatch a [`standalone`](Self::standalone) command. These run without
    /// starting any instances or opening a client connection. Panics if called
    /// with a client subcommand (those go through `dispatch_client`).
    #[allow(unused_variables)]
    pub async fn dispatch_standalone(self) -> Result<ExitCode> {
        match self {
            #[cfg(feature = "client")]
            Commands::Lsp { args } => {
                crate::lsp::run(args).await?;
            }
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
            Commands::Agents { action } => client::agents(action, fps).await,
            Commands::Servers { action } => client::servers(action, &state.server, fps).await,
            #[cfg(feature = "probe")]
            Commands::Probe { target, .. } => {
                sandpolis_probe::cli::dispatch(target, &state.probe, fps).await
            }
            #[cfg(feature = "desktop")]
            Commands::Desktop { action, target, .. } => {
                sandpolis_desktop::cli::dispatch(action, target, &state.desktop, fps).await
            }
            #[cfg(feature = "shell")]
            Commands::Shell { target, .. } => {
                sandpolis_shell::cli::dispatch(target, state.shell.clone(), fps).await
            }
            #[cfg(feature = "health")]
            Commands::Health { target, .. } => client::stub("health", target, fps).await,
            #[cfg(feature = "inventory")]
            Commands::Inventory { target, .. } => client::stub("inventory", target, fps).await,
            #[cfg(feature = "filesystem")]
            Commands::Filesystem { target, .. } => client::stub("filesystem", target, fps).await,
            #[cfg(feature = "account")]
            Commands::Account { target, .. } => client::stub("account", target, fps).await,
            #[cfg(feature = "snapshot")]
            Commands::Snapshot { target, .. } => client::stub("snapshot", target, fps).await,
            Commands::Wake { target, .. } => client::stub("wake", target, fps).await,
            #[cfg(feature = "audit")]
            Commands::Audit { target, .. } => client::stub("audit", target, fps).await,
            #[cfg(feature = "tunnel")]
            Commands::Tunnel { target, .. } => client::stub("tunnel", target, fps).await,
            #[allow(unreachable_patterns)]
            _ => unreachable!("standalone commands are dispatched by dispatch_standalone"),
        }
    }
}

#[cfg(all(test, feature = "server"))]
mod test_stratum_args {
    use super::*;
    use sandpolis_instance::realm::RealmCert;
    use sandpolis_server::ServerStratum;

    /// Write a realm cert whose certificate names `url`, the way a server
    /// writes one out for its realm at startup.
    fn upstream_realm_cert(dir: &std::path::Path, url: &str) -> Result<PathBuf> {
        let url: sandpolis_server::ServerUrl = url.parse()?;
        let ca = RealmCert::new_cluster(Default::default(), url.realm.clone())?;
        let path = dir.join("upstream.realm.pem");
        ca.endpoint_cert(&url)?.write_pem(&path)?;
        Ok(path)
    }

    /// The flags `sandpolis server` was given.
    fn server_args(argv: &[&str]) -> Result<ServerArgs, clap::Error> {
        match CommandLine::try_parse_from(argv)?.command {
            Commands::Server { args, .. } => Ok(args),
            other => panic!("expected the server subcommand, got {other:?}"),
        }
    }

    /// A process is one instance, so it has to say which one.
    #[test]
    fn an_instance_is_required() {
        assert!(CommandLine::try_parse_from(["sandpolis"]).is_err());
    }

    /// Absent `--realm`, this is the network's single global stratum server.
    #[test]
    fn no_realm_cert_means_global_stratum() -> Result<()> {
        let args = server_args(&["sandpolis", "server"]).expect("`server` parses");
        assert_eq!(
            crate::stratum(args.realm.as_deref())?,
            ServerStratum::Global
        );
        Ok(())
    }

    /// The realm cert names the upstream and selects the local stratum; the
    /// address comes out of the certificate rather than a separate flag.
    #[test]
    fn realm_cert_means_local_stratum() -> Result<()> {
        let dir = tempfile::tempdir()?;
        let path = upstream_realm_cert(dir.path(), "gs.example.com:8768/default")?;

        let args = server_args(&["sandpolis", "server", "--realm", path.to_str().unwrap()])
            .expect("--realm parses");

        let ServerStratum::Local { global } = crate::stratum(args.realm.as_deref())? else {
            panic!("--realm must select the local stratum");
        };
        assert_eq!(global.host, "gs.example.com");
        assert_eq!(global.port, 8768);
        Ok(())
    }

    /// A local stratum server keeps a database of its own, so the two flags
    /// describe different things and go together.
    #[test]
    fn data_and_realm_go_together() -> Result<()> {
        let args = server_args(&[
            "sandpolis",
            "server",
            "--data",
            "/var/lib/sandpolis",
            "--realm",
            "./upstream.realm.pem",
        ])
        .expect("--data and --realm parse together");

        assert_eq!(
            args.data.as_deref(),
            Some(std::path::Path::new("/var/lib/sandpolis"))
        );
        Ok(())
    }

    /// A realm cert that isn't there is a misconfiguration, not something to
    /// silently fall back from.
    #[test]
    fn missing_realm_cert_is_an_error() {
        let args = server_args(&[
            "sandpolis",
            "server",
            "--realm",
            "/nonexistent/upstream.realm.pem",
        ])
        .expect("--realm parses");
        assert!(crate::stratum(args.realm.as_deref()).is_err());
    }
}

#[cfg(all(test, feature = "agent"))]
mod test_agent_args {
    use super::*;

    /// The agent daemon's flags belong to the agent daemon, not to the process.
    #[test]
    fn agent_takes_its_own_flags() {
        let command = CommandLine::try_parse_from([
            "sandpolis",
            "agent",
            "--realm",
            "./fleet.realm.pem",
            "--data",
            "/var/lib/sandpolis",
        ])
        .expect("`agent --realm --data` parses")
        .command;

        let Commands::Agent { args, .. } = command else {
            panic!("expected the agent subcommand, got {command:?}");
        };
        assert_eq!(
            args.realm.as_deref(),
            Some(std::path::Path::new("./fleet.realm.pem"))
        );
        assert_eq!(
            args.data.as_deref(),
            Some(std::path::Path::new("/var/lib/sandpolis"))
        );
    }

    /// Polling is a property of how this agent runs rather than of the
    /// certificate it holds, so it comes from the command line.
    #[test]
    fn poll_flags_build_the_poll_config() {
        let command = CommandLine::try_parse_from([
            "sandpolis",
            "agent",
            "--poll",
            "0 */5 * * * *",
            "--poll-timeout",
            "45",
        ])
        .expect("`agent --poll --poll-timeout` parses")
        .command;

        let Commands::Agent { args, .. } = command else {
            panic!("expected the agent subcommand, got {command:?}");
        };
        let poll = args.options().poll.expect("--poll selects polling mode");
        assert_eq!(poll.schedule, "0 */5 * * * *");
        assert_eq!(poll.timeout_secs, 45);
    }

    /// Without `--poll` the agent stays continuously connected, whatever
    /// `--poll-timeout` says.
    #[test]
    fn no_poll_flag_means_continuous() {
        let command = CommandLine::try_parse_from(["sandpolis", "agent", "--poll-timeout", "45"])
            .expect("`agent --poll-timeout` parses")
            .command;

        let Commands::Agent { args, .. } = command else {
            panic!("expected the agent subcommand, got {command:?}");
        };
        assert!(args.options().poll.is_none());
    }
}

#[cfg(all(test, feature = "client"))]
mod test_client_commands {
    use super::*;

    /// Managing agent instances is `agents list`, distinct from `agent` which
    /// runs the daemon.
    #[test]
    fn agents_list_parses() {
        let command = CommandLine::try_parse_from(["sandpolis", "agents", "list", "--json"])
            .expect("`agents list --json` parses")
            .command;

        let Commands::Agents {
            action: AgentsCommand::List { json, .. },
        } = command
        else {
            panic!("expected the agents list subcommand, got {command:?}");
        };
        assert!(json);
    }

    /// Managing configured servers is `servers list`, distinct from `server`
    /// which runs the daemon.
    #[test]
    fn servers_list_parses() {
        let command = CommandLine::try_parse_from(["sandpolis", "servers", "list", "--json"])
            .expect("`servers list --json` parses")
            .command;

        let Commands::Servers {
            action: ServersCommand::List { json, .. },
        } = command
        else {
            panic!("expected the servers list subcommand, got {command:?}");
        };
        assert!(json);
    }

    /// The plural commands manage instances, so they need an action to say
    /// which management operation to run.
    #[test]
    fn agents_requires_a_subcommand() {
        assert!(CommandLine::try_parse_from(["sandpolis", "agents"]).is_err());
    }
}

#[cfg(all(test, feature = "client"))]
mod test_lsp_args {
    use super::*;

    fn lsp_args(argv: &[&str]) -> Result<crate::lsp::LspArgs, clap::Error> {
        match CommandLine::try_parse_from(argv)?.command {
            Commands::Lsp { args } => Ok(args),
            other => panic!("expected the lsp subcommand, got {other:?}"),
        }
    }

    /// Naming no format leaves the root type undecided, which would serve
    /// completions for whichever one happened to be the default.
    #[test]
    fn a_root_type_is_required() {
        assert!(lsp_args(&["sandpolis", "lsp"]).is_err());
    }

    /// The flag selects the format it names.
    #[test]
    fn the_flag_selects_its_format() -> Result<()> {
        assert_eq!(
            lsp_args(&["sandpolis", "lsp", "--realm"])?.root_type(),
            "crate::config::RealmConfig"
        );
        Ok(())
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

    pub(super) async fn agents(action: AgentsCommand, fps: f32) -> Result<ExitCode> {
        match action {
            AgentsCommand::List { json, .. } => {
                if json {
                    return list_agents_json().await;
                }
                let widget = crate::client::tui::agent_list::AgentListWidget::new()?;
                sandpolis_client::tui::run_tui(fps, widget).await?;
                Ok(ExitCode::SUCCESS)
            }
            AgentsCommand::Restart { instance, .. } => {
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

    pub(super) async fn servers(
        action: ServersCommand,
        server_manager: &sandpolis_server::ServerManager,
        fps: f32,
    ) -> Result<ExitCode> {
        match action {
            ServersCommand::List { json, .. } => {
                if json {
                    return list_servers_json(server_manager);
                }
                let widget =
                    crate::client::tui::server_list::ServerListWidget::new(server_manager.clone())?;
                sandpolis_client::tui::run_tui(fps, widget).await?;
                Ok(ExitCode::SUCCESS)
            }
        }
    }

    fn list_servers_json(server_manager: &sandpolis_server::ServerManager) -> Result<ExitCode> {
        let servers: Vec<_> = server_manager
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
    /// to be established (the connection to the server comes up
    /// asynchronously), then subscribes to the instance model and reads from
    /// the client database.
    async fn list_agents_json() -> Result<ExitCode> {
        use sandpolis_instance::InstanceManagerData;
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
        sandpolis_client::sync::subscribe(sandpolis_instance::instance_manager_model_id(), None);
        sandpolis_client::sync::subscribe(
            sandpolis_instance::network::liveness::liveness_model_id(),
            None,
        );

        // Give the subscription a moment to deliver records.
        info!("list_agents_json: waiting 500ms for sync delivery");
        tokio::time::sleep(Duration::from_millis(500)).await;
        info!("list_agents_json: reading client database");

        let db =
            sandpolis_client::sync::client_database().context("client database unavailable")?;
        let realm = db.realm(RealmName::default())?;
        let r = realm.r_transaction()?;
        let all: Vec<InstanceManagerData> =
            r.scan().primary()?.all()?.collect::<Result<Vec<_>, _>>()?;

        // Which of them are up is a separate, server-written model, resolved the
        // same way the GUI resolves it: an observer's word counts only while the
        // observer itself is reachable.
        let rows: Vec<sandpolis_instance::network::liveness::LivenessData> =
            r.scan().primary()?.all()?.collect::<Result<Vec<_>, _>>()?;
        let online = sandpolis_instance::network::liveness::reachable(
            rows,
            sandpolis_client::sync::connected_instances(),
        );

        let agents: Vec<_> = all
            .into_iter()
            .filter(|i| i._instance_id.is_agent())
            .map(|i| {
                serde_json::json!({
                    "instance_id": i._instance_id.to_string(),
                    "cluster_id": i.cluster_id.to_string(),
                    "os": i.os_info.to_string(),
                    "online": online.contains(&i._instance_id),
                })
            })
            .collect();

        println!("{}", serde_json::to_string_pretty(&agents)?);
        Ok(ExitCode::SUCCESS)
    }
}
