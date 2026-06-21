use crate::ShellLayer;
use crate::client::tui::ShellTerminalWidget;
use anyhow::Result;
use sandpolis_client::cli::TargetArgs;
use std::process::ExitCode;

/// Open an interactive shell on the target agent. With `--instance` this opens
/// the TUI terminal pointed at that agent; without one it shows a placeholder
/// (the agent picker isn't built yet).
pub async fn dispatch(target: TargetArgs, shell: ShellLayer, fps: f32) -> Result<ExitCode> {
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
