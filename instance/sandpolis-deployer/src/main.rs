//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//

use crate::config::{validate_config, DeployerConfig};

use anyhow::{bail, Result};
use log::debug;
use rust_embed::RustEmbed;
use serde::Deserialize;

pub mod java;
pub mod rust;
pub mod callback;
pub mod config;
pub mod systemd;

/// Contains embedded resources
#[derive(RustEmbed)]
#[folder = "res"]
pub struct BinaryAssets;

#[derive(Deserialize)]
struct BuildJson {

}

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
