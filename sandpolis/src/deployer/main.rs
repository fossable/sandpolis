//! # Deployer
//!
//! Deployer instances are responsible for installing, updating, or removing agent
//! and probe instances.
//!
//! If an existing agent or probe was originally installed by a package manager, it
//! cannot be updated or removed by a deployer.
//!
//! ## Instance Configuration
//!
//! ```py
//! {
//!   "agent_type"   : String(), # The type of agent to install
//!   "callback"     : {
//!     "address"    : String(), # The callback address
//!     "identifier" : String(), # The callback identifier
//!   },
//!   "install"      : {
//!     "install_dir"  : String(), # The installation's base directory
//!     "autorecover"  : String(), # Whether the agent can disregard elements of the config in case of failure
//!     "autostart"    : Boolean(), # Whether the agent should be started on boot
//!   },
//!   "kilo"         : {
//!     "modules" : [
//!       {
//!         "group"       : String(), # The artifact's maven group identifier
//!         "artifact"    : String(), # The artifact's identifier
//!         "filename"    : String(), # The artifact's filename
//!         "version"     : String(), # The artifact's version string
//!         "hash"        : String(), # The artifact's SHA256 hash
//!       }
//!     ]
//!   }
//! }
//! ```
//!
//! ## Callbacks Connections
//!
//! If the install/update operation fails, and callbacks are configured, the
//! deployer will establish an encrypted "callback" connection with a server and
//! transfer details on the error.

use crate::config::{validate_config, DeployerConfig};

use anyhow::{bail, Result};
use log::debug;
use rust_embed::RustEmbed;
use serde::Deserialize;

pub mod callback;
pub mod config;
pub mod java;
pub mod rust;
pub mod systemd;

/// Contains embedded resources
#[derive(RustEmbed)]
#[folder = "res"]
pub struct BinaryAssets;

#[derive(Deserialize)]
struct BuildJson {}

fn main() -> Result<()> {
    // Initialize logging
    env_logger::init();

    debug!("Starting automated installer");

    if let Some(build_data) = BinaryAssets::get("build.json") {
        debug!("Parsing {} byte build.json", build_data.len());
        let build: BuildJson = serde_json::from_slice(&build_data)?;
    } else {
        bail!("Missing build.json asset");
    }

    if let Some(config_data) = BinaryAssets::get("deployer.json") {
        debug!("Parsing {} byte deployer.json", config_data.len());
        let config: DeployerConfig = serde_json::from_slice(&config_data)?;

        // Validate the configuration
        validate_config(&config)?;

        // Dispatch appropriate installer
        match config.agent_type.as_str() {
            "nano" => crate::nano::install(&config),
            "rust" => crate::rust::install(&config),
            "java" => crate::java::install(&config),
            _ => Ok(()),
        }?;
    } else {
        bail!("Missing deployer.json asset");
    }

    Ok(())
}
