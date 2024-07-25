use crate::{core::database::Database, CommandLine};
use anyhow::Result;
use clap::Parser;

use self::ui::AppState;

pub mod ui;

#[derive(Parser, Debug, Clone, Default)]
pub struct ClientCommandLine {}

pub async fn main(args: CommandLine) -> Result<()> {
    let mut state = AppState {
        db: Database::new(None, "test", "test").await?,
    };

    // Create server connection(s)
    for server in args.server.unwrap_or(Vec::new()) {
        state.db.add_server(&server, "test", "test").await?;
    }

    crate::client::ui::run(state);
    Ok(())
}
