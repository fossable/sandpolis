use anyhow::bail;
use anyhow::Result;
use axum::Router;
use clap::builder::OsStr;
use clap::{Parser, Subcommand};
use futures::Future;
use sandpolis::cli::CommandLine;
use sandpolis::cli::Commands;
use sandpolis::InstanceState;
use sandpolis_database::Database;
use sandpolis_database::Document;
use sandpolis_group::GroupCaCert;
use std::fs::File;
use std::io::Write;
use std::process::ExitCode;
use tokio::task::JoinSet;
use tracing::info;
use tracing_subscriber::filter::LevelFilter;

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

    // Load state
    let state = InstanceState::new(db);

    // Prepare to launch instances
    let mut tasks = JoinSet::new();

    #[cfg(feature = "server")]
    tasks.spawn(async { server(&args, state.clone()).await });

    #[cfg(feature = "agent")]
    tasks.spawn(async { agent(&args, state.clone()).await });

    // The client must run on the main thread per bevy requirements
    #[cfg(feature = "client")]
    client(&args, state).await?;

    // If this was a client, don't hold up the user by waiting for server/agent
    if !cfg!(feature = "client") {
        while let Some(result) = tasks.join_next().await {
            result?;
        }
    }

    Ok(ExitCode::SUCCESS)
}

#[cfg(feature = "server")]
async fn server(args: &CommandLine, state: InstanceState) -> Result<()> {
    let app: Router<()> = Router::new()
        .nest("/server", sandpolis_server::ServerLayer::server_routes())
        .nest("/server", sandpolis_user::UserLayer::server_routes())
        .route_layer(axum::middleware::from_fn(
            sandpolis_group::server::auth_middleware,
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
async fn agent(args: &CommandLine, state: InstanceState) -> Result<()> {
    let uds = UnixListener::bind(&args.agent_args.agent_socket)?;
    tokio::spawn(async move {
        let app = Router::new().nest("/layer/agent", sandpolis_agent::AgentLayer::agent_routes());

        #[cfg(feature = "layer-shell")]
        let app = app.nest("/layer/shell", sandpolis_shell::ShellLayer::agent_routes());

        #[cfg(feature = "layer-package")]
        let app = app.nest(
            "/layer/package",
            sandpolis_package::PackageLayer::agent_routes(),
        );

        #[cfg(feature = "layer-desktop")]
        let app = app.nest(
            "/layer/desktop",
            sandpolis_desktop::DesktopLayer::agent_routes(),
        );

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
async fn client(args: &CommandLine, state: InstanceState) -> Result<()> {
    #[cfg(feature = "client-gui")]
    if args.client.gui {}

    #[cfg(feature = "client-tui")]
    if args.client.tui {}

    Ok(())
}
