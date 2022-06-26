//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//

use crate::BinaryAssets;
use crate::config::DeployerConfig;
use crate::callback::CallbackResult;
use crate::callback::send_callback;
use crate::systemd;

use std::env;
use std::fs;
use std::io::copy;
use std::io::Write;
use std::path::Path;
use std::collections::HashSet;

use anyhow::{bail, Result};
use log::{debug, error, info};
use which::which;

/// Check that the given directory appears to be an installation created by a
/// deployer.
pub fn is_deployer_installation(path: &Path) -> bool {
    if ! path.is_dir() {
        return false;
    }

    if ! path.join("lib").is_dir() {
        return false;
    }

    if ! path.join("bin").is_dir() {
        return false;
    }

    return true;
}

/// Find existing agent installations
pub fn agent_search(config: &DeployerConfig) -> HashSet<&Path> {
    debug!("Searching for existing java agent installations");

    let mut paths = HashSet::new();

    // First check the location specified in the config
    let config_path = Path::new(&config.install_path);
    if is_deployer_installation(config_path) {
        paths.insert(config_path);
    }

    // Next check standard location according to platform
    let standard_path = match env::consts::OS {
        "windows" => Path::new(""),
        _ => Path::new("/opt/sandpolis-agent"),
    };
    if is_deployer_installation(standard_path) {
        paths.insert(standard_path);
    }

    // Next check the PATH
    match which("sandpolis-agent") {
        Ok(discovered_path) => {
            if is_deployer_installation(&discovered_path) {
            //    paths.insert(discovered_path);
            }
        }
        _ => (),
    }

    return paths;
}

/// Install or reinstall a java (Java) agent
pub fn install(config: &DeployerConfig) -> Result<()> {
    let existing = agent_search(config);

    let install_path: &Path = if existing.len() == 1 {
        debug!(
            "Found an existing installation at: {}",
            existing.iter().next().unwrap().display()
        );
        existing.iter().next().unwrap()
    } else if existing.len() > 1 {
        info!("Multiple existing installations found");
        // TODO
        Path::new(&config.install_path)
    } else {
        debug!("No existing installations found");
        Path::new(&config.install_path)
    };

    debug!("Starting java agent installation");

    // Create base directory
    fs::create_dir_all(install_path)?;

    // Build HTTP client for downloading modules
    let http = reqwest::blocking::Client::new();

    // Install the required modules
    for module in config.java.as_ref().expect("").modules.iter() {
        let mut dest = fs::File::create(install_path.join(format!("{}.jar", module.artifact)))?;

        if let Some(data) = BinaryAssets::get(&module.artifact) {
            dest.write_all(&data)?;
        } else {
            // Download the module instead
            let url = if module.gpr_module.is_some() {
                format!(
                    "https://api.sandpolis.com/v1/download/{}/{}/{}/{}",
                    module.gpr_module.as_ref().unwrap(),
                    module.gpr_package.as_ref().expect(""),
                    module.version.as_ref().expect(""),
                    module.filename
                )
            } else {
                format!(
                    "https://repo1.maven.org/maven2/{}/{}/{}/{}",
                    module.maven_group.as_ref().expect(""),
                    module.artifact,
                    module.version.as_ref().expect(""),
                    module.filename
                )
            };
            copy(&mut http.get(url).send()?, &mut dest)?;
        }
    }

    // Install autostart components if requested
    if config.autostart {
        match env::consts::OS {
            "windows" => {

            }
            "linux" => {
                if systemd::is_installed() {

                }
            }
            _ => {

            }
        };
    }

    // Send a "success" callback result if configured
    if let Some(callback_config) = &config.callback {
        send_callback(&callback_config, &CallbackResult {
            install_path: install_path.display().to_string(),
            identifier: callback_config.identifier.clone(),
        })?;
    }

    return Ok(());
}
