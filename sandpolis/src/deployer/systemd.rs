//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//

use std::fs;
use std::path::Path;
use std::process::Command;

use anyhow::{bail, Result};
use log::{debug, error, info};
use which::which;

/// Stop and delete any services matching the given name.
#[cfg(target_os = "linux")]
pub fn remove_service(name: String) -> Result<()> {
    if let Ok(status) = Command::new("systemctl").arg("stop").arg(name).status() {
        if status.success() {
            debug!("Successfully stopped service");
        }
    }

    fs::remove_file("/usr/lib/systemd/system/sandpolis-agent.service")?;
    return Ok(());
}

#[cfg(target_os = "linux")]
pub fn install_service(exe_path: &Path) -> Result<()> {
    fs::write(
        "/usr/lib/systemd/system/sandpolis-agent.service",
        r#"
        [Unit]
        Description=Sandpolis Agent
        After=network.target

        [Service]
        ExecStart={}
        Restart=always

        [Install]
        WantedBy=multi-user.target
    "#,
    )?;

    return Ok(());
}

/// Check that systemd is actually installed
//#[cfg(target_os = "linux")]
pub fn is_installed() -> bool {
    return which("systemctl").is_ok();
}
