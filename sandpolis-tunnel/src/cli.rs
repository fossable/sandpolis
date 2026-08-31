//! `sandpolis tunnel` — inspect configured tunnels.
//!
//! Tunnels are declared in realm config, so the CLI is read-only: it lists the
//! tunnels the server is bridging and their live traffic counters.

use crate::TunnelData;
use anyhow::{Context, Result};
use clap::Subcommand;
use sandpolis_client::cli::TargetArgs;
use std::process::ExitCode;
use std::time::Duration;

#[derive(Subcommand, Debug, Clone)]
pub enum TunnelCommand {
    /// List configured tunnels and their live state
    List,
}

pub async fn dispatch(
    action: Option<TunnelCommand>,
    target: TargetArgs,
    fps: f32,
) -> Result<ExitCode> {
    match action {
        Some(TunnelCommand::List) => list(target).await,
        // No subcommand: list noninteractively under `--json`, otherwise open a
        // placeholder browser.
        None if target.json => list(target).await,
        None => {
            sandpolis_client::tui::run_tui(
                fps,
                sandpolis_client::tui::PlaceholderPanel::new(
                    "tunnel browser (pass a subcommand, e.g. `tunnel list`)",
                ),
            )
            .await?;
            Ok(ExitCode::SUCCESS)
        }
    }
}

async fn list(target: TargetArgs) -> Result<ExitCode> {
    sandpolis_client::sync::wait_for_connection(Duration::from_secs(30))
        .await
        .context("No server connection")?;

    crate::client::subscribe();
    // Give the opening sync snapshot a moment to replicate down.
    tokio::time::sleep(Duration::from_millis(500)).await;

    let tunnels: Vec<TunnelData> = crate::client::all_tunnels()?
        .into_iter()
        .filter(|t| {
            target
                .instance
                .is_none_or(|id| t.listener_id == id || t.terminator_id == id)
        })
        .collect();

    if target.json {
        println!("{}", serde_json::to_string(&tunnels)?);
    } else if tunnels.is_empty() {
        println!("No tunnels");
    } else {
        for t in &tunnels {
            println!(
                "{} [{}] {} -> {} {} {} conns={} rx={} tx={}",
                t.name,
                t.state_str(),
                t.listen_addr,
                t.target_addr,
                t.protocol,
                t.effective_mode,
                t.active_connections,
                t.rx_bytes,
                t.tx_bytes,
            );
        }
    }
    Ok(ExitCode::SUCCESS)
}

impl TunnelData {
    fn state_str(&self) -> &'static str {
        match self.state {
            crate::TunnelState::Pending => "pending",
            crate::TunnelState::Active => "active",
            crate::TunnelState::Failed => "failed",
        }
    }
}
