use anyhow::{bail, Result};
use clap::{Parser, Subcommand};
use std::{path::PathBuf, str::FromStr};

#[derive(Parser, Debug, Clone)]
pub struct InstanceCommandLine {
    /// Enable debug mode
    #[clap(long, num_args = 0, default_value_t = false)]
    pub debug: bool,

    /// Enable trace mode
    #[clap(long, num_args = 0, default_value_t = false)]
    pub trace: bool,
}
