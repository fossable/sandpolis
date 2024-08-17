use crate::CommandLine;
use anyhow::Result;
use clap::Parser;

pub mod ui;

// Prohibit using a local database on platforms that don't support
#[cfg(all(feature = "local-database", target_os = "android"))]
compile_error!("Platform does not support local-database");

#[derive(Parser, Debug, Clone, Default)]
pub struct ClientCommandLine {}

pub async fn main(args: CommandLine) -> Result<()> {
    crate::client::ui::run(args).await?;
    Ok(())
}
