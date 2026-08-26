use crate::ShellManager;
use crate::client::tui::ShellTerminalWidget;
use anyhow::Result;
use sandpolis_client::cli::TargetArgs;
use std::process::ExitCode;

/// Open an interactive shell on the target agent. With `--instance` this opens
/// the TUI terminal pointed at that agent; without one it shows a placeholder
/// (the agent picker isn't built yet). Noninteractive (`--json`) operation is
/// not implemented, and reports so.
pub async fn dispatch(target: TargetArgs, shell: ShellManager, fps: f32) -> Result<ExitCode> {
    if target.json {
        println!("{{\"status\":\"unimplemented\",\"command\":\"shell\"}}");
        return Ok(ExitCode::FAILURE);
    }

    let Some(instance) = target.instance else {
        sandpolis_client::tui::run_tui(
            fps,
            sandpolis_client::tui::PlaceholderPanel::new("shell (pass --instance <id>)"),
        )
        .await?;
        return Ok(ExitCode::SUCCESS);
    };

    let widget = ShellTerminalWidget::new(instance, shell);
    sandpolis_client::tui::run_tui(fps, widget).await?;
    Ok(ExitCode::SUCCESS)
}
