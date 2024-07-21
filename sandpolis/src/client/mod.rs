use crate::{core::database::Database, CommandLine};
use anyhow::Result;
use clap::Parser;

use self::ui::AppState;

pub mod ui;

#[derive(Parser, Debug, Clone)]
pub struct ClientCommandLine {}

pub async fn main(args: CommandLine) -> Result<()> {
    let state = AppState {
        db: Database::new(None, "test", "test").await?,
    };

    // Setup database

    crate::client::ui::run(state);
    Ok(())
}
