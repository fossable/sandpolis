use crate::CommandLine;
use anyhow::Result;
use clap::Parser;

pub mod ui;

#[derive(Parser, Debug, Clone, Default)]
pub struct ClientCommandLine {}

pub async fn main(args: CommandLine) -> Result<()> {
    crate::client::ui::run(args).await?;
    Ok(())
}
