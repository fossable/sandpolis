use anyhow::bail;
use anyhow::Result;
use axum::routing::{any, get, post};
use axum::Router;
use clap::Parser;
use sandpolis::cli::CommandLine;
use sandpolis::cli::Commands;
use sandpolis::InstanceState;
use sandpolis_database::Collection;
use sandpolis_database::Database;
use sandpolis_database::Document;
use sandpolis_group::{GroupCaCert, GroupData};
use sandpolis_network::ServerAddress;
use sandpolis_network::{ConnectionCooldown, ServerConnection};
use std::fs::File;
use std::io::Write;
use std::process::ExitCode;
use std::time::Duration;
use tokio::task::JoinSet;
use tracing::info;
use tracing_subscriber::filter::LevelFilter;

pub mod routes;

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
    let db = Database::new(&args.database.storage)?;

    // Dispatch subcommands
    match args.command {
        #[cfg(feature = "server")]
        Some(Commands::GenerateCert { group, output }) => {
            let groups: Collection<GroupData> = db.collection("/groups")?;
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
    {
        let s = state.clone();
        let a = args.clone();
        tasks.spawn(async move { server(a, s).await });
    }

    #[cfg(feature = "agent")]
    {
        let s = state.clone();
        let a = args.clone();
        tasks.spawn(async move { agent(a, s).await });
    }

    // The client must run on the main thread per bevy requirements
    #[cfg(feature = "client")]
    client(args, state).await?;

    // If this was a client, don't hold up the user by waiting for server/agent
    if !cfg!(feature = "client") {
        while let Some(result) = tasks.join_next().await {
            result?;
        }
    }

    Ok(ExitCode::SUCCESS)
}

#[cfg(feature = "server")]
async fn server(args: CommandLine, state: InstanceState) -> Result<()> {
    // TODO fallback routes from client to agent?

    use anyhow::Context;

    let app: Router<InstanceState> = Router::new()
        // TODO from_fn_with_state for IP blocking
        .route_layer(axum::middleware::from_fn(
            sandpolis_group::server::auth_middleware,
        ));

    // Server layer
    let app: Router<InstanceState> = app.route(
        "/server/banner",
        get(sandpolis_server::server::routes::banner),
    );

    // User layer
    let app: Router<InstanceState> =
        app.route("/user/login", post(sandpolis_user::server::routes::login));

    // Network layer
    let app: Router<InstanceState> =
        app.route("/network/ping", get(sandpolis_network::routes::ping));

    // let app: Router<()> = app.with_state(state);

    info!(listener = ?args.server.listen, "Starting server listener");
    axum_server::bind(args.server.listen)
        .acceptor(sandpolis_group::server::GroupAcceptor::new(
            state.group.groups.clone(),
        )?)
        .serve(app.with_state(state).into_make_service())
        .await
        .context("binding socket")?;

    Ok(())
}

#[cfg(feature = "agent")]
async fn agent(args: CommandLine, state: InstanceState) -> Result<()> {
    let uds = tokio::net::UnixListener::bind(&args.agent.agent_socket)?;
    tokio::spawn(async move {
        let app: Router<InstanceState> = Router::new();

        // Agent layer
        let app = app.route(
            "/agent/uninstall",
            post(sandpolis_agent::agent::routes::uninstall),
        );

        // Network layer
        let app = app.route("/network/ping", get(sandpolis_network::routes::ping));

        // Shell layer
        #[cfg(feature = "layer-shell")]
        let app = app
            .route(
                "/shell/session",
                any(sandpolis_shell::agent::routes::session),
            )
            .route(
                "/shell/execute",
                post(sandpolis_shell::agent::routes::execute),
            );

        // Filesystem layer
        #[cfg(feature = "layer-filesystem")]
        let app = app
            .route(
                "/filesystem/session",
                any(sandpolis_filesystem::agent::routes::session),
            )
            .route(
                "/shell/delete",
                post(sandpolis_filesystem::agent::routes::delete),
            );

        // Package layer
        #[cfg(feature = "layer-package")]
        let app = app.nest(
            "/layer/package",
            sandpolis_package::PackageLayer::agent_routes(),
        );

        // Desktop layer
        #[cfg(feature = "layer-desktop")]
        let app = app.nest(
            "/layer/desktop",
            sandpolis_desktop::DesktopLayer::agent_routes(),
        );

        axum::serve(uds, app.with_state(state).into_make_service())
            .await
            .unwrap();
    });

    let mut servers = match args.network.server {
        Some(servers) => servers,
        None => Vec::new(),
    };

    // If a server is running in the same process, add it with highest priority
    #[cfg(feature = "server")]
    {
        servers.insert(
            0,
            ServerAddress::Ip(args.server.listen),
            // .to_string()
            // .replace("0.0.0.0", "127.0.0.1"),
        );
    }

    if servers.len() == 0 {
        bail!("No server defined");
    }

    ServerConnection::new(
        todo!(),
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
async fn client(args: CommandLine, state: InstanceState) -> Result<()> {
    #[cfg(feature = "client-gui")]
    if args.client.gui {}

    #[cfg(feature = "client-tui")]
    if args.client.tui {}

    Ok(())
}
