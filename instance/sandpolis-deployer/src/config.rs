//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//

use anyhow::{bail, Result};
use serde::{Deserialize, Serialize};

#[derive(Deserialize)]
pub struct JavaAgentModule {
    pub gpr_module: Option<String>,

    pub gpr_package: Option<String>,

    /// The artifact's group
    pub maven_group: Option<String>,

    /// The artifact's identifier
    pub artifact: String,

    /// The artifact's filename
    pub filename: String,

    /// The artifact's version string
    pub version: Option<String>,

    /// The artifact's integrity hash
    pub hash: Option<String>,
}

#[derive(Deserialize)]
pub struct JavaAgentConfig {
    /// A list of all required modules
    pub modules: Vec<JavaAgentModule>,
}

#[derive(Deserialize)]
pub struct CallbackConfig {
    /// The callback address
    pub address: String,

    /// The callback identifier
    pub identifier: String,
}

#[derive(Deserialize)]
pub struct DeployerConfig {
    /// The type of agent to install
    pub agent_type: String,

    /// The filesystem path where the agent should be installed
    pub install_path: String,

    /// Whether the installer is allowed to disregard elements of the config in
    /// order to recover from errors.
    pub autorecover: bool,

    pub autostart: bool,

    pub java: Option<JavaAgentConfig>,

    pub callback: Option<CallbackConfig>,
}

/// Validate the configuration
pub fn validate_config(config: &DeployerConfig) -> Result<()> {
    // Check agent type
    if ! vec!["rust", "java"].contains(&config.agent_type.as_str()) {
        bail!("Invalid agent type");
    }

    // TODO

    return Ok(());
}
