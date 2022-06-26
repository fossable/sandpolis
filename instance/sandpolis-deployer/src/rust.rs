//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//

use crate::config::DeployerConfig;
use anyhow::{bail, Result};
use log::{debug, error, info};
use std::fs::{create_dir_all, File};
use std::io::Write;

/// Install or reinstall a rust agent
pub fn install(config: &DeployerConfig) -> Result<()> {
    debug!("Starting rust agent installation");

    // Create the agent directory
    create_dir_all(config.install_path.as_str())?;

    if let Some(executable) = crate::BinaryAssets::get("agent-rust") {
        let exe_path = format!("{}/agent-rust", config.install_path);

        File::create(exe_path)?.write_all(&executable)?;
    } else {
        // Download the module
        //download_artifact("https://repo1.maven.org/maven2/%s/%s/%s/%s");
    }

    return Ok(());
}
