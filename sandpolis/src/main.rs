use anyhow::bail;
use anyhow::Result;
use clap::builder::OsStr;
use clap::{Parser, Subcommand};
use futures::Future;
use sandpolis::CommandLine;
use std::process::ExitCode;
use tokio::task::JoinSet;
use tracing::info;
use tracing_subscriber::filter::LevelFilter;

#[derive(Parser, Debug, Clone)]
#[clap(author, version, about, long_about = None)]
struct CommandLine {
    #[cfg(feature = "server")]
    #[clap(flatten)]
    pub server: sandpolis_server::cli::ServerCommandLine,

    #[cfg(feature = "client")]
    #[clap(flatten)]
    pub client: sandpolis_client::cli::ClientCommandLine,

    #[cfg(feature = "agent")]
    #[clap(flatten)]
    pub agent: sandpolis_agent::cli::AgentCommandLine,

    #[clap(flatten)]
    pub instance: sandpolis_instance::cli::InstanceCommandLine,

    #[clap(flatten)]
    pub database: sandpolis_database::cli::DatabaseCommandLine,

    #[command(subcommand)]
    pub command: Option<Commands>,
}

#[derive(Subcommand, Debug, Clone)]
pub enum Commands {
    #[cfg(feature = "server")]
    /// Generate a new endpoint certificate signed by the group CA
    GenerateCert {
        /// Group to generate the certificate for
        #[clap(long, default_value = "default")]
        group: String,

        /// Output file path
        #[clap(long, default_value = "./endpoint.json")]
        output: PathBuf,
    },

    InstallCert {},
}

#[tokio::main]
async fn main() -> Result<ExitCode> {
    #[cfg(all(
        not(feature = "server"),
        not(feature = "agent"),
        not(feature = "client")
    ))]
    bail!("No instance was enabled at build time");

    let args = CommandLine::parse();

    // Initialize logging for the instance
    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::builder()
                .with_default_directive(if args.instance.trace {
                    LevelFilter::TRACE.into()
                } else if args.instance.debug {
                    LevelFilter::DEBUG.into()
                } else {
                    LevelFilter::INFO.into()
                })
                .from_env()?,
        )
        .init();

    info!(
        version = sandpolis::built_info::PKG_VERSION,
        build_time = sandpolis::built_info::BUILT_TIME_UTC,
        "Initializing Sandpolis"
    );

    // Get ready to do some cryptography
    rustls::crypto::aws_lc_rs::default_provider()
        .install_default()
        .expect("crypto provider is available");

    // Load instance database
    let db = Database::new(args.database.storage)?;

    // Dispatch subcommands
    match args.command {
        #[cfg(feature = "server")]
        Some(Commands::GenerateCert { group, output }) => {
            let g = groups.get_document(&group)?.expect("the group exists");
            let ca: Document<GroupCaCert> = g.get_document("ca")?.expect("the CA exists");

            let cert = ca.data.client_cert(&group.parse()?)?;

            info!(path = %output.display(), "Writing endpoint certificate");
            let mut output = File::create(output)?;
            output.write_all(&serde_json::to_vec(&cert)?)?;

            return Ok(ExitCode::SUCCESS);
        }
        _ => (),
    }

    // Prepare to launch instances
    let mut tasks = JoinSet::new();

    #[cfg(feature = "server")]
    tasks.spawn(async { server(&args, db.clone()).await });

    #[cfg(feature = "agent")]
    tasks.spawn(async { agent(&args, db.clone()).await });

    // The client must run on the main thread per bevy requirements
    #[cfg(feature = "client")]
    client(&args, db).await?;

    // If this was a client, don't hold up the user by waiting for server/agent
    if !cfg!(feature = "client") {
        while let Some(result) = tasks.join_next().await {
            result?;
        }
    }

    Ok(ExitCode::SUCCESS)
}

#[cfg(feature = "server")]
async fn server(args: &CommandLine, db: Database) -> Result<()> {
    let state = ServerState::new(db);
    
    let app: Router<()> = Router::new()
        .fallback(fallback_handler)
        .nest("/server", ServerLayer::router())
        .route_layer(axum::middleware::from_fn(
            layer::server::group::auth_middleware,
        ))
        .with_state(state);

    info!(listener = ?args.server_args.listen, "Starting server listener");
    axum_server::bind(args.server_args.listen)
        .acceptor(GroupAcceptor::new(groups)?)
        .serve(app.into_make_service())
        .await
        .context("binding socket")?;

    Ok(())
}

#[cfg(feature = "agent")]
async fn agent(args: &CommandLine, db: Database) -> Result<()> {
    let state = AgentState::new(db);
    
    let uds = UnixListener::bind(&args.agent_args.agent_socket)?;
    tokio::spawn(async move {
        let app = Router::new().nest("/layer/agent", layer::agent::router());

        #[cfg(feature = "layer-shell")]
        let app = app.nest("/layer/shell", sandpolis_shell::agent::router());

        #[cfg(feature = "layer-package")]
        let app = app.nest("/layer/package", sandpolis_package::agent::router());

        #[cfg(feature = "layer-desktop")]
        let app = app.nest("/layer/desktop", sandpolis_desktop::agent::router());

        axum::serve(uds, app.with_state(state).into_make_service())
            .await
            .unwrap();
    });

    let mut servers = if let Some(servers) = args.server {
        servers
    } else {
        Vec::new()
    };

    // If a server is running in the same process, add it with highest priority
    #[cfg(feature = "server")]
    {
        servers.insert(
            0,
            ServerAddress::Ip(args.server_args.listen),
            // .to_string()
            // .replace("0.0.0.0", "127.0.0.1"),
        );
    }

    if servers.len() == 0 {
        bail!("No server defined");
    }

    ServerConnection::new(
        cert,
        servers,
        ConnectionCooldown {
            initial: Duration::from_millis(4000),
            constant: None,
            limit: None,
        },
    )?
    .run()
    .await;
    Ok(())
}

#[cfg(feature = "client")]
async fn client(args: &CommandLine, db: Database) -> Result<()> {
    let state = ClientState::new(db);
    
    #[cfg(feature = "client-gui")]
    if args.client.gui {}

    #[cfg(feature = "client-tui")]
    if args.client.tui {}
}
