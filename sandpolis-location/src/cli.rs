use anyhow::{bail, Result};
use clap::Parser;
use std::{path::PathBuf, str::FromStr};

#[derive(Parser, Debug, Clone)]
pub struct LocationCommandLine {
    /// Service to use for resolving IP location info
    #[cfg(feature = "server")]
    #[clap(long)]
    pub location_service: LocationService,
}
