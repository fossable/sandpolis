use crate::DesktopLayer;
use crate::screenshot::{
    DesktopScreenshotRequest, DesktopScreenshotRequester, DesktopScreenshotResult,
};
use anyhow::{Context, Result, bail};
use clap::Subcommand;
use sandpolis_client::cli::TargetArgs;
use std::path::PathBuf;
use std::process::ExitCode;
use std::time::Duration;

#[derive(Subcommand, Debug, Clone)]
pub enum DesktopCommand {
    /// Capture a single screenshot from the target agent
    Screenshot {
        /// Display identifier on the agent (default: first display)
        #[clap(long, default_value = "")]
        desktop: String,

        /// Output PNG path (default: raw PNG to stdout)
        #[clap(long)]
        output: Option<PathBuf>,
    },
}

pub async fn dispatch(
    action: Option<DesktopCommand>,
    target: TargetArgs,
    _layer: &DesktopLayer,
    fps: f32,
) -> Result<ExitCode> {
    match action {
        Some(DesktopCommand::Screenshot { desktop, output }) => {
            screenshot(target, desktop, output).await
        }
        None => {
            // The live viewer isn't wired to a stream yet.
            sandpolis_client::tui::run_tui(
                fps,
                sandpolis_client::tui::PlaceholderPanel::new("desktop viewer"),
            )
            .await?;
            Ok(ExitCode::SUCCESS)
        }
    }
}

async fn screenshot(
    target: TargetArgs,
    desktop: String,
    output: Option<PathBuf>,
) -> Result<ExitCode> {
    let Some(instance) = target.instance else {
        bail!("--instance is required for screenshot");
    };

    let conn = sandpolis_client::sync::wait_for_connection(Duration::from_secs(10))
        .await
        .context("no server connection")?;

    let (requester, mut rx) = DesktopScreenshotRequester::channel();
    let (id, _tx) = conn
        .open_stream_to(
            instance,
            requester,
            DesktopScreenshotRequest {
                desktop_uuid: desktop,
            },
        )
        .await?;

    let result = tokio::time::timeout(Duration::from_secs(20), rx.recv()).await;
    conn.close_stream(id);

    let frame = match result {
        Ok(Some(DesktopScreenshotResult::Ok(frame))) => frame,
        Ok(Some(DesktopScreenshotResult::Failed)) => {
            if target.json {
                println!("{{\"status\":\"failed\"}}");
            } else {
                eprintln!("Screenshot capture failed");
            }
            return Ok(ExitCode::FAILURE);
        }
        _ => {
            if target.json {
                println!("{{\"status\":\"timeout\"}}");
            } else {
                eprintln!("Timed out waiting for screenshot");
            }
            return Ok(ExitCode::FAILURE);
        }
    };

    let img = image::RgbaImage::from_raw(frame.width, frame.height, frame.rgba)
        .context("invalid frame dimensions")?;
    let mut png = std::io::Cursor::new(Vec::new());
    image::DynamicImage::ImageRgba8(img).write_to(&mut png, image::ImageFormat::Png)?;
    let bytes = png.into_inner();

    if let Some(path) = &output {
        std::fs::write(path, &bytes)?;
    } else if !target.json {
        use std::io::Write;
        std::io::stdout().write_all(&bytes)?;
    }

    if target.json {
        match &output {
            Some(p) => println!(
                "{{\"status\":\"ok\",\"bytes\":{},\"output\":{:?}}}",
                bytes.len(),
                p.display().to_string()
            ),
            None => println!("{{\"status\":\"ok\",\"bytes\":{}}}", bytes.len()),
        }
    }

    Ok(ExitCode::SUCCESS)
}
