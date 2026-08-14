use crate::RuntimeOptions;
use anyhow::Result;
#[cfg(feature = "client")]
use anyhow::bail;
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
    /// Directory holding this server's database and the `.realm` files it
    /// serves.
    ///
    /// Every `*.realm` file in the directory declares one realm, named after
    /// the filename stem. A blank file means "generate a realm CA for me",
    /// which is then written back into the file — after which that file is the
    /// durable copy of the realm's trust root. A global stratum server that
    /// finds no realm file creates `default.realm`.
    ///
    /// Without this flag the server is ephemeral: its databases are kept in
    /// memory and nothing survives the process.
    #[clap(long, value_name = "DIR")]
    pub data: Option<PathBuf>,

    /// Path to a `.server` file naming this server's upstream ($S7S_SERVER).
    ///
    /// Having one is what puts this server in the local stratum: it carries the
    /// realm CA and this server's own certificate, whose common name is the
    /// upstream's address. A local stratum server serves no realms of its own.
    #[clap(long, value_name = "PATH", env = "S7S_SERVER")]
    pub server: Option<PathBuf>,

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
    /// Directory holding this agent's database.
    ///
    /// Without this flag the agent is ephemeral: its database is kept in memory
    /// and nothing survives the process.
    #[clap(long, value_name = "DIR")]
    pub data: Option<PathBuf>,

    /// Path to the `.server` file naming the server this agent attaches to
    /// ($S7S_SERVER).
    ///
    /// The file carries the realm CA, this agent's own certificate — whose
    /// common name is the server's address — and its polling schedule, so one
    /// file is the whole connection policy.
    #[clap(long, value_name = "PATH", env = "S7S_SERVER")]
    pub server: Option<PathBuf>,
}

/// Flags for the client, which every client subcommand shares.
///
/// A client keeps no database on disk, so it has no `--data`.
#[cfg(feature = "client")]
#[derive(clap::Args, Debug, Clone, Default)]
pub struct ClientArgs {
    /// Path to a `.server` file naming the server this client connects to
    /// ($S7S_SERVER).
    ///
    /// The file carries the realm CA and this client's own certificate, whose
    /// common name is the server's address. Without it the GUI asks for a
    /// server to log into.
    #[clap(long, value_name = "PATH", env = "S7S_SERVER")]
    pub server: Option<PathBuf>,

    /// Frame rate for the GUI and TUI ($S7S_FPS).
    #[clap(long, default_value_t = 30, env = "S7S_FPS")]
    pub fps: u32,
}

#[cfg(feature = "server")]
impl ServerArgs {
    /// The process-wide options these flags describe. The realms and stratum
    /// are filled in by the caller, which is what reads the `--data` directory
    /// and the `.server` file.
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
    /// The process-wide options these flags describe. What the `.server` file
    /// contributes is filled in by the caller, which is what loads it.
    pub fn options(&self) -> RuntimeOptions {
        RuntimeOptions {
            database: sandpolis_instance::database::config::DatabaseConfig {
                storage: self.data.clone(),
                ..Default::default()
            },
            instance_type: sandpolis_instance::InstanceType::Agent,
            ..Default::default()
        }
    }
}

#[cfg(feature = "client")]
impl ClientArgs {
    /// The process-wide options these flags describe.
    pub fn options(&self) -> RuntimeOptions {
        RuntimeOptions {
            instance_type: sandpolis_instance::InstanceType::Client,
            fps: self.fps,
            ..Default::default()
        }
    }
}

/// Subcommands for `sandpolis agent`. Without one, the agent daemon runs.
#[cfg(feature = "client")]
#[derive(Subcommand, Debug, Clone)]
pub enum AgentCommand {
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

/// Subcommands for `sandpolis server`. Without one, the server daemon runs.
#[cfg(feature = "client")]
#[derive(Subcommand, Debug, Clone)]
pub enum ServerCommand {
    /// List all configured servers
    List {
        /// Emit machine-readable JSON instead of opening a TUI
        #[clap(long)]
        json: bool,

        #[clap(flatten)]
        client: ClientArgs,
    },
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
    fn resolve(
        &self,
    ) -> Result<(
        sandpolis_instance::realm::RealmClusterCert,
        sandpolis_server::ServerUrl,
    )> {
        use anyhow::{Context, anyhow, bail};
        use sandpolis_instance::realm::RealmClusterCert;

        let config = crate::config::RealmConfig::load(&self.realm)?;

        let Some(ca) = config.ca.as_ref() else {
            bail!(
                "{} declares no realm CA. Start the server once with \
                 `--data {}` to generate one.",
                self.realm.display(),
                self.realm
                    .parent()
                    .unwrap_or_else(|| std::path::Path::new("."))
                    .display()
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
    Lsp {
        #[clap(flatten)]
        args: crate::lsp::LspArgs,
    },

    /// Run the agent daemon, or manage agent instances
    #[cfg(any(feature = "agent", feature = "client"))]
    Agent {
        #[cfg(feature = "agent")]
        #[clap(flatten)]
        args: AgentArgs,

        #[cfg(feature = "client")]
        #[command(subcommand)]
        action: Option<AgentCommand>,
    },

    /// Run the server daemon, or manage server instances
    #[cfg(any(feature = "server", feature = "client"))]
    Server {
        #[cfg(feature = "server")]
        #[clap(flatten)]
        args: ServerArgs,

        #[cfg(feature = "client")]
        #[command(subcommand)]
        action: Option<ServerCommand>,
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
            #[cfg(feature = "server")]
            Commands::NewClientCert { .. } | Commands::NewAgentCert { .. } => true,
            Commands::InstallCert {} | Commands::About => true,
            #[cfg(feature = "client")]
            Commands::Lsp { .. } => true,
            #[allow(unreachable_patterns)]
            _ => false,
        }
    }

    /// The flags for the server daemon, when that's what this command is:
    /// `sandpolis server` with no action beneath it.
    #[cfg(feature = "server")]
    pub fn server_daemon(&self) -> Option<&ServerArgs> {
        match self {
            Commands::Server {
                args,
                #[cfg(feature = "client")]
                    action: None,
                ..
            } => Some(args),
            #[allow(unreachable_patterns)]
            _ => None,
        }
    }

    /// The flags for the agent daemon, when that's what this command is:
    /// `sandpolis agent` with no action beneath it.
    #[cfg(feature = "agent")]
    pub fn agent_daemon(&self) -> Option<&AgentArgs> {
        match self {
            Commands::Agent {
                args,
                #[cfg(feature = "client")]
                    action: None,
                ..
            } => Some(args),
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
            Commands::Agent { action, .. } => match action.as_ref()? {
                AgentCommand::List { client, .. } => Some(client),
                AgentCommand::Restart { client, .. } => Some(client),
            },
            Commands::Server { action, .. } => match action.as_ref()? {
                ServerCommand::List { client, .. } => Some(client),
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
            Commands::Agent { action, .. } => match action {
                Some(action) => client::agent(action, fps).await,
                // Only reachable on a build without the agent; otherwise this
                // is the daemon and never gets here.
                None => bail!(
                    "This build has no agent. Rebuild with `--features agent` to run one, \
                     or name a subcommand (see `sandpolis agent --help`)."
                ),
            },
            Commands::Server { action, .. } => match action {
                Some(action) => client::server(action, &state.server, fps).await,
                None => bail!(
                    "This build has no server. Rebuild with `--features server` to run one, \
                     or name a subcommand (see `sandpolis server --help`)."
                ),
            },
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

    /// Absent `--server`, this is the network's single global stratum server.
    #[test]
    fn no_server_file_means_global_stratum() -> Result<()> {
        let args = server_args(&["sandpolis", "server"]).expect("`server` parses");
        assert_eq!(crate::stratum(args.server.as_deref())?, ServerStratum::Global);
        Ok(())
    }

    /// The `.server` file names the upstream and selects the local stratum; the
    /// address comes out of the certificate rather than a separate flag.
    #[test]
    fn server_file_means_local_stratum() -> Result<()> {
        let dir = tempfile::tempdir()?;
        let path = client_server_file(dir.path(), "gs.example.com:8768/default")?;

        let args = server_args(&["sandpolis", "server", "--server", path.to_str().unwrap()])
            .expect("--server parses");

        let ServerStratum::Local { global } = crate::stratum(args.server.as_deref())? else {
            panic!("--server must select the local stratum");
        };
        assert_eq!(global.host, "gs.example.com");
        assert_eq!(global.port, 8768);
        Ok(())
    }

    /// A local stratum server keeps a database of its own, so the two flags
    /// describe different things and go together.
    #[test]
    fn data_and_server_go_together() -> Result<()> {
        let args = server_args(&[
            "sandpolis",
            "server",
            "--data",
            "/var/lib/sandpolis",
            "--server",
            "./upstream.server",
        ])
        .expect("--data and --server parse together");

        assert_eq!(
            args.data.as_deref(),
            Some(std::path::Path::new("/var/lib/sandpolis"))
        );
        Ok(())
    }

    /// A `.server` file that isn't there is a misconfiguration, not something to
    /// silently fall back from.
    #[test]
    fn missing_server_file_is_an_error() {
        let args = server_args(&[
            "sandpolis",
            "server",
            "--server",
            "/nonexistent/upstream.server",
        ])
        .expect("--server parses");
        assert!(crate::stratum(args.server.as_deref()).is_err());
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
            "--server",
            "./fleet.server",
            "--data",
            "/var/lib/sandpolis",
        ])
        .expect("`agent --server --data` parses")
        .command;

        let Commands::Agent { args, .. } = command else {
            panic!("expected the agent subcommand, got {command:?}");
        };
        assert_eq!(
            args.server.as_deref(),
            Some(std::path::Path::new("./fleet.server"))
        );
        assert_eq!(
            args.data.as_deref(),
            Some(std::path::Path::new("/var/lib/sandpolis"))
        );
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

    /// Neither flag leaves the root type undecided, which would serve
    /// completions for whichever format happened to be the default.
    #[test]
    fn a_root_type_is_required() {
        assert!(lsp_args(&["sandpolis", "lsp"]).is_err());
    }

    /// A document has one root type, so the two flags can't both be given.
    #[test]
    fn root_types_are_exclusive() {
        assert!(lsp_args(&["sandpolis", "lsp", "--realm", "--server"]).is_err());
    }

    /// Each flag selects the format it names.
    #[test]
    fn each_flag_selects_its_format() -> Result<()> {
        assert_eq!(
            lsp_args(&["sandpolis", "lsp", "--realm"])?.root_type(),
            "crate::config::RealmConfig"
        );
        assert_eq!(
            lsp_args(&["sandpolis", "lsp", "--server"])?.root_type(),
            "sandpolis_instance::realm::config::ServerCertFile"
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

    pub(super) async fn agent(action: AgentCommand, fps: f32) -> Result<ExitCode> {
        match action {
            AgentCommand::List { json, .. } => {
                if json {
                    return list_agents_json().await;
                }
                let widget = crate::client::tui::agent_list::AgentListWidget::new()?;
                sandpolis_client::tui::run_tui(fps, widget).await?;
                Ok(ExitCode::SUCCESS)
            }
            AgentCommand::Restart { instance, .. } => {
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
        action: ServerCommand,
        server_layer: &sandpolis_server::ServerLayer,
        fps: f32,
    ) -> Result<ExitCode> {
        match action {
            ServerCommand::List { json, .. } => {
                if json {
                    return list_servers_json(server_layer);
                }
                let widget =
                    crate::client::tui::server_list::ServerListWidget::new(server_layer.clone())?;
                sandpolis_client::tui::run_tui(fps, widget).await?;
                Ok(ExitCode::SUCCESS)
            }
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
    /// to be established (the connection to the server comes up
    /// asynchronously), then subscribes to the instance model and reads from
    /// the client database.
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
                })
            })
            .collect();

        println!("{}", serde_json::to_string_pretty(&agents)?);
        Ok(ExitCode::SUCCESS)
    }
}
