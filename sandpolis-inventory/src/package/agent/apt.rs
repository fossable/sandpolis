use super::PackageManager;
use crate::package::{PackageData, PackageManager as PM};
use anyhow::{bail, Result};
use std::path::PathBuf;
use std::process::Command;
use tracing::debug;

#[derive(Debug, Clone)]
pub struct Apt {
    executable: PathBuf,
}

impl Apt {
    pub fn new() -> Result<Self> {
        let executable = which::which("apt")
            .map_err(|_| anyhow::anyhow!("apt not found in PATH"))?;
        Ok(Self { executable })
    }

    pub fn is_available() -> bool {
        which::which("apt").is_ok()
    }

    async fn exec_command(&self, args: &[&str]) -> Result<String> {
        let output = Command::new(&self.executable)
            .args(args)
            .output()?;

        if !output.status.success() {
            bail!("apt command failed with exit code: {}", output.status);
        }

        Ok(String::from_utf8_lossy(&output.stdout).to_string())
    }
}

impl PackageManager for Apt {
    fn get_location(&self) -> Result<PathBuf> {
        Ok(self.executable.clone())
    }

    async fn get_version(&self) -> Result<String> {
        let output = self.exec_command(&["--version"]).await?;
        Ok(output.lines().next().unwrap_or("").to_string())
    }

    async fn clean(&self) -> Result<()> {
        self.exec_command(&["clean"]).await?;
        Ok(())
    }

    async fn get_installed(&self) -> Result<Vec<PackageData>> {
        let lines = self.exec_command(&["list", "--installed"]).await?;
        let mut packages = Vec::new();

        for line in lines.lines().skip(1) { // Skip header line
            let parts: Vec<&str> = line.split_whitespace().collect();
            if parts.len() >= 3 {
                let name_version = parts[0];
                if let Some(slash_pos) = name_version.find('/') {
                    let name = &name_version[..slash_pos];
                    let version = parts[1];
                    
                    packages.push(PackageData {
                        name: name.to_string(),
                        version: version.to_string(),
                        manager: PM::Apt,
                        description: None,
                        latest_available: None,
                        latest_upstream: None,
                        architecture: if parts.len() > 2 { Some(parts[2].to_string()) } else { None },
                        package_size: None,
                        installed_size: None,
                        upstream_url: None,
                        remote_location: None,
                        repository: None,
                        files: None,
                        explicit: None,
                        licenses: None,
                        dependencies: None,
                        usages: None,
                        build_time: None,
                        install_time: None,
                        ..Default::default()
                    });
                }
            }
        }

        Ok(packages)
    }

    async fn get_metadata(&self, name: String) -> Result<PackageData> {
        let stdout = self.exec_command(&["show", &name]).await?;
        let mut package_data = PackageData {
            name: name.clone(),
            manager: PM::Apt,
            ..Default::default()
        };

        for line in stdout.lines() {
            if let Some((key, value)) = line.split_once(": ") {
                match key {
                    "Version" => package_data.version = value.to_string(),
                    "Description" => package_data.description = Some(value.to_string()),
                    "Architecture" => package_data.architecture = Some(value.to_string()),
                    "Installed-Size" => {
                        if let Ok(size) = value.parse::<u64>() {
                            package_data.installed_size = Some(size * 1024); // Convert KB to bytes
                        }
                    }
                    "Size" => {
                        if let Ok(size) = value.parse::<u64>() {
                            package_data.package_size = Some(size);
                        }
                    }
                    "Homepage" => package_data.upstream_url = Some(value.to_string()),
                    "Depends" => {
                        let deps: Vec<String> = value.split(", ")
                            .map(|dep| dep.split_whitespace().next().unwrap_or("").to_string())
                            .filter(|s| !s.is_empty())
                            .collect();
                        package_data.dependencies = Some(deps);
                    }
                    _ => {}
                }
            }
        }

        Ok(package_data)
    }

    async fn get_outdated(&self) -> Result<Vec<PackageData>> {
        let lines = self.exec_command(&["list", "--upgradable"]).await?;
        let mut packages = Vec::new();

        for line in lines.lines().skip(1) { // Skip header line
            let parts: Vec<&str> = line.split_whitespace().collect();
            if parts.len() >= 3 {
                let name_version = parts[0];
                if let Some(slash_pos) = name_version.find('/') {
                    let name = &name_version[..slash_pos];
                    let version = parts[1];
                    
                    packages.push(PackageData {
                        name: name.to_string(),
                        version: version.to_string(),
                        manager: PM::Apt,
                        ..Default::default()
                    });
                }
            }
        }

        Ok(packages)
    }

    async fn install(&self, packages: Vec<String>) -> Result<()> {
        debug!("Installing {} packages", packages.len());

        let mut args = vec!["-y", "install"];
        args.extend(packages.iter().map(|s| s.as_str()));
        self.exec_command(&args).await?;
        Ok(())
    }

    async fn refresh(&self) -> Result<()> {
        debug!("Refreshing package database");
        self.exec_command(&["update"]).await?;
        Ok(())
    }

    async fn remove(&self, packages: Vec<String>) -> Result<()> {
        debug!("Removing {} packages", packages.len());

        let mut args = vec!["-y", "remove"];
        args.extend(packages.iter().map(|s| s.as_str()));
        self.exec_command(&args).await?;
        Ok(())
    }

    async fn upgrade(&self, packages: Vec<String>) -> Result<()> {
        debug!("Upgrading {} packages", packages.len());

        let mut args = vec!["-y", "upgrade"];
        args.extend(packages.iter().map(|s| s.as_str()));
        self.exec_command(&args).await?;
        Ok(())
    }
}