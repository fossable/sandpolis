use crate::ProbeLayer;
use anyhow::Result;
use sandpolis_client::cli::TargetArgs;
use std::process::ExitCode;

/// Probe devices. The interactive device-list TUI is not yet built, so this
/// currently opens a placeholder panel (or reports unimplemented for `--json`).
pub async fn dispatch(target: TargetArgs, _layer: &ProbeLayer, fps: f32) -> Result<ExitCode> {
    if target.json {
        println!("{{\"status\":\"unimplemented\",\"command\":\"probe\"}}");
        return Ok(ExitCode::FAILURE);
    }
    sandpolis_client::tui::run_tui(fps, sandpolis_client::tui::PlaceholderPanel::new("probe"))
        .await?;
    Ok(ExitCode::SUCCESS)
}
