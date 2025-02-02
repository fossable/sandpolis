use anyhow::{bail, Result};
use clap::{Parser, Subcommand};
use std::{path::PathBuf, str::FromStr};

#[derive(Parser, Debug, Clone)]
pub struct GroupCommandLine {
    /// Path to authentication certificate which will be installed into the database.
    /// Subsequent runs don't require this option.
    #[clap(long)]
    pub certificate: Option<PathBuf>,
}

impl GroupCommandLine {
    pub fn import_certificate() {}
}
