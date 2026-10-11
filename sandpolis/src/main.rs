use anyhow::Result;
use clap::{CommandFactory, FromArgMatches};
use sandpolis::cli::CommandLine;
use std::process::ExitCode;
use tracing_subscriber::filter::LevelFilter;

#[tokio::main]
async fn main() -> Result<ExitCode> {
    #[cfg(all(
        not(feature = "server"),
        not(feature = "agent"),
        not(feature = "client")
    ))]
    {
        anyhow::bail!("No instance was enabled at build time");
    }

    // The emblem rides above the help text, as a real image on terminals with
    // a graphics protocol and in block letters elsewhere. clap is handed it as
    // `before_help` so it appears only when help is actually printed, and
    // never when the output is a pipe or a file.
    #[allow(unreachable_code)]
    let args = {
        let mut command = CommandLine::command();
        if let Some(emblem) = cilki::terminal::before_help(&cilki::emblem::SANDPOLIS, 56, 7) {
            command = command.before_help(emblem);
        }
        CommandLine::from_arg_matches(&command.get_matches())?
    };

    // A client subcommand opens a TUI (or prints JSON), so it owns the terminal;
    // send logs to a file in that case instead of corrupting the view. Daemons
    // and the GUI log to stderr.
    let use_log_file = args.command.owns_terminal();

    // Initialize logging for the instance
    let level = if args.instance.trace {
        LevelFilter::TRACE
    } else if args.instance.debug {
        LevelFilter::DEBUG
    } else {
        LevelFilter::INFO
    };
    let make_filter = || {
        tracing_subscriber::EnvFilter::builder()
            .with_default_directive(level.into())
            .from_env()
    };
    // Color only when a person is watching: ANSI escapes in a captured log
    // break anything that parses it (fail2ban, grep).
    if use_log_file {
        let file = std::fs::OpenOptions::new()
            .append(true)
            .create(true)
            .open("sandpolis.log")?;
        tracing_subscriber::fmt()
            .with_env_filter(make_filter()?)
            .with_writer(file)
            .with_ansi(false)
            .init();
    } else {
        use std::io::IsTerminal;
        tracing_subscriber::fmt()
            .with_env_filter(make_filter()?)
            .with_writer(std::io::stderr)
            .with_ansi(std::io::stderr().is_terminal())
            .init();
    }

    // Get ready to do some cryptography
    rustls::crypto::aws_lc_rs::default_provider()
        .install_default()
        .expect("crypto provider is available");

    // Standalone subcommands (cert generation, version info, LSP) run without
    // starting an instance or opening a connection.
    if args.command.standalone() {
        return args.command.dispatch_standalone().await;
    }

    // One process is exactly one instance, named by the subcommand.
    #[cfg(feature = "server")]
    if let Some(server_args) = args.command.server_daemon().cloned() {
        return sandpolis::server::start(server_args).await;
    }

    #[cfg(feature = "agent")]
    if let Some(agent_args) = args.command.agent_daemon().cloned() {
        return sandpolis::agent::start(agent_args).await;
    }

    // Everything left runs as a client: the GUI in the foreground, or a
    // subcommand's focused TUI.
    #[cfg(feature = "client")]
    {
        return sandpolis::client::start(args.command).await;
    }

    #[allow(unreachable_code)]
    Ok(ExitCode::SUCCESS)
}
