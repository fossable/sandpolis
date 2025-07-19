use super::PackageManager;
use crate::{PackageData, PackageManager as PM};
use anyhow::{bail, Result};
use regex::Regex;
use std::collections::HashMap;
use std::fs;
use std::path::{Path, PathBuf};
use std::process::Command;
use tracing::debug;

#[derive(Debug, Clone)]
pub struct Pacman {
    executable: PathBuf,
}

impl Pacman {
    pub fn new() -> Result<Self> {
        let executable = which::which("pacman")
            .map_err(|_| anyhow::anyhow!("pacman not found in PATH"))?;
        Ok(Self { executable })
    }

    pub fn is_available() -> bool {
        which::which("pacman").is_ok()
    }

    async fn exec_command(&self, args: &[&str]) -> Result<String> {
        let output = Command::new(&self.executable)
            .args(args)
            .output()?;

        if !output.status.success() {
            bail!("pacman command failed with exit code: {}", output.status);
        }

        Ok(String::from_utf8_lossy(&output.stdout).to_string())
    }

    async fn exec_command_lines(&self, args: &[&str]) -> Result<Vec<String>> {
        let output = self.exec_command(args).await?;
        Ok(output.lines().map(|s| s.to_string()).collect())
    }
}

impl PackageManager for Pacman {
    fn get_location(&self) -> Result<PathBuf> {
        Ok(self.executable.clone())
    }

    async fn get_version(&self) -> Result<String> {
        let output = self.exec_command(&["-V"]).await?;
        let version_regex = Regex::new(r"Pacman v(.*) -").unwrap();
        
        for line in output.lines() {
            if let Some(captures) = version_regex.captures(line) {
                return Ok(captures.get(1).unwrap().as_str().to_string());
            }
        }
        
        bail!("Could not parse pacman version");
    }

    async fn clean() -> Result<()> {
        debug!("Cleaning package cache");

        let output = Command::new("pacman")
            .args(&["-Sc", "--noconfirm"])
            .output()?;

        if !output.status.success() {
            bail!("pacman clean failed with exit code: {}", output.status);
        }

        Ok(())
    }

    async fn get_installed() -> Result<Vec<PackageData>> {
        let output = Command::new("pacman")
            .args(&["-Q"])
            .output()?;

        if !output.status.success() {
            bail!("pacman -Q failed");
        }

        let lines = String::from_utf8_lossy(&output.stdout);
        let mut packages = Vec::new();

        for line in lines.lines() {
            let parts: Vec<&str> = line.split_whitespace().collect();
            if parts.len() >= 2 {
                let name = parts[0];
                let version = parts[1];
                
                packages.push(PackageData {
                    name: name.to_string(),
                    version: version.to_string(),
                    manager: PM::Pacman,
                    ..Default::default()
                });
            }
        }

        Ok(packages)
    }

    async fn get_metadata(name: String) -> Result<PackageData> {
        let output = Command::new("pacman")
            .args(&["-Qi", &name])
            .output()?;

        if !output.status.success() {
            bail!("pacman -Qi {} failed", name);
        }

        let stdout = String::from_utf8_lossy(&output.stdout);
        let mut package_data = PackageData {
            name: name.clone(),
            manager: PM::Pacman,
            ..Default::default()
        };

        for line in stdout.lines() {
            if let Some((key, value)) = line.split_once(" : ") {
                let key = key.trim();
                let value = value.trim();
                
                match key {
                    "Version" => package_data.version = value.to_string(),
                    "Description" => package_data.description = Some(value.to_string()),
                    "Architecture" => package_data.architecture = Some(value.to_string()),
                    "Installed Size" => {
                        // Parse size like "1.23 MiB" or "456.78 KiB"
                        if let Some(size_str) = value.split_whitespace().next() {
                            if let Ok(size) = size_str.parse::<f64>() {
                                let unit = value.split_whitespace().nth(1).unwrap_or("");
                                let bytes = match unit {
                                    "KiB" => (size * 1024.0) as u64,
                                    "MiB" => (size * 1024.0 * 1024.0) as u64,
                                    "GiB" => (size * 1024.0 * 1024.0 * 1024.0) as u64,
                                    _ => size as u64,
                                };
                                package_data.installed_size = Some(bytes);
                            }
                        }
                    }
                    "URL" => package_data.upstream_url = Some(value.to_string()),
                    "Licenses" => {
                        let licenses: Vec<String> = value.split_whitespace()
                            .map(|s| s.to_string())
                            .collect();
                        package_data.licenses = Some(licenses);
                    }
                    "Depends On" => {
                        if value != "None" {
                            let deps: Vec<String> = value.split_whitespace()
                                .map(|s| s.to_string())
                                .collect();
                            package_data.dependencies = Some(deps);
                        }
                    }
                    "Build Date" => {
                        // Parse date like "Mon 01 Jan 2024 12:00:00 PM UTC"
                        // This is a simplified parser - in production you'd want more robust date parsing
                        package_data.build_time = None; // TODO: Implement proper date parsing
                    }
                    "Install Date" => {
                        // Parse date like "Mon 01 Jan 2024 12:00:00 PM UTC"
                        package_data.install_time = None; // TODO: Implement proper date parsing
                    }
                    _ => {}
                }
            }
        }

        Ok(package_data)
    }

    async fn get_outdated() -> Result<Vec<PackageData>> {
        debug!("Querying for outdated packages");

        let output = Command::new("pacman")
            .args(&["-Suq", "--print-format", "%n"])
            .output()?;

        if !output.status.success() {
            bail!("pacman -Suq failed");
        }

        let lines = String::from_utf8_lossy(&output.stdout);
        let mut packages = Vec::new();

        for line in lines.lines() {
            let name = line.trim();
            if !name.is_empty() {
                packages.push(PackageData {
                    name: name.to_string(),
                    manager: PM::Pacman,
                    ..Default::default()
                });
            }
        }

        Ok(packages)
    }

    async fn install(packages: Vec<String>) -> Result<()> {
        debug!("Installing {} packages", packages.len());

        let mut args = vec!["-S", "--noconfirm"];
        let package_refs: Vec<&str> = packages.iter().map(|s| s.as_str()).collect();
        args.extend(package_refs);

        let output = Command::new("pacman")
            .args(&args)
            .output()?;

        if !output.status.success() {
            bail!("pacman install failed with exit code: {}", output.status);
        }

        Ok(())
    }

    async fn refresh() -> Result<()> {
        debug!("Refreshing package database");

        let output = Command::new("pacman")
            .args(&["-Sy"])
            .output()?;

        if !output.status.success() {
            bail!("pacman refresh failed with exit code: {}", output.status);
        }

        Ok(())
    }

    async fn remove(packages: Vec<String>) -> Result<()> {
        debug!("Removing {} packages", packages.len());

        let mut args = vec!["-R", "--noconfirm"];
        let package_refs: Vec<&str> = packages.iter().map(|s| s.as_str()).collect();
        args.extend(package_refs);

        let output = Command::new("pacman")
            .args(&args)
            .output()?;

        if !output.status.success() {
            bail!("pacman remove failed with exit code: {}", output.status);
        }

        Ok(())
    }

    async fn upgrade(packages: Vec<String>) -> Result<()> {
        debug!("Upgrading {} packages", packages.len());

        let mut args = vec!["-S", "--noconfirm"];
        let package_refs: Vec<&str> = packages.iter().map(|s| s.as_str()).collect();
        args.extend(package_refs);

        let output = Command::new("pacman")
            .args(&args)
            .output()?;

        if !output.status.success() {
            bail!("pacman upgrade failed with exit code: {}", output.status);
        }

        Ok(())
    }
}

/// Pacman package metadata parsed from the local database
#[derive(Debug, Clone)]
pub struct PackageMetadata {
    pub name: String,
    pub version: String,
    pub base: String,
    pub description: String,
    pub url: String,
    pub architecture: String,
    pub build_date: u64,
    pub install_date: u64,
    pub packager: String,
    pub size: u64,
    pub license: String,
    pub validation: String,
    pub depends: Vec<String>,
    pub opt_depends: HashMap<String, String>,
    pub files: Vec<String>,
}

impl PackageMetadata {
    /// Parse package metadata from a pacman database directory
    pub fn from_directory<P: AsRef<Path>>(directory: P) -> Result<Self> {
        let directory = directory.as_ref();
        
        // Read "desc" file
        let desc_path = directory.join("desc");
        let desc_content = fs::read_to_string(&desc_path)
            .map_err(|e| anyhow::anyhow!("Failed to read desc file: {}", e))?;
        let desc_lines: Vec<&str> = desc_content.lines().collect();

        // Helper function to find value after a key
        let find_value = |key: &str| -> Result<String> {
            let index = desc_lines.iter().position(|&line| line == key)
                .ok_or_else(|| anyhow::anyhow!("Key {} not found", key))?;
            desc_lines.get(index + 1)
                .ok_or_else(|| anyhow::anyhow!("Value for key {} not found", key))
                .map(|s| s.to_string())
        };

        let name = find_value("%NAME%")?;
        let version = find_value("%VERSION%")?;
        let base = find_value("%BASE%")?;
        let description = find_value("%DESC%")?;
        let url = find_value("%URL%")?;
        let architecture = find_value("%ARCH%")?;
        let build_date = find_value("%BUILDDATE%")?.parse::<u64>()?;
        let install_date = find_value("%INSTALLDATE%")?.parse::<u64>()?;
        let packager = find_value("%PACKAGER%")?;
        let size = find_value("%SIZE%")?.parse::<u64>()?;
        let license = find_value("%LICENSE%")?;
        let validation = find_value("%VALIDATION%")?;

        // Parse dependencies
        let mut depends = Vec::new();
        if let Some(depends_idx) = desc_lines.iter().position(|&line| line == "%DEPENDS%") {
            let mut idx = depends_idx + 1;
            while idx < desc_lines.len() && !desc_lines[idx].is_empty() {
                depends.push(desc_lines[idx].to_string());
                idx += 1;
            }
        }

        // Parse optional dependencies
        let mut opt_depends = HashMap::new();
        if let Some(optdepends_idx) = desc_lines.iter().position(|&line| line == "%OPTDEPENDS%") {
            let mut idx = optdepends_idx + 1;
            while idx < desc_lines.len() && !desc_lines[idx].is_empty() {
                let line = desc_lines[idx];
                if let Some((key, value)) = line.split_once(':') {
                    opt_depends.insert(key.to_string(), value.to_string());
                }
                idx += 1;
            }
        }

        // Read "files" file
        let files_path = directory.join("files");
        let mut files = Vec::new();
        if files_path.exists() {
            let files_content = fs::read_to_string(&files_path)
                .map_err(|e| anyhow::anyhow!("Failed to read files file: {}", e))?;
            let files_lines: Vec<&str> = files_content.lines().collect();
            
            if let Some(files_idx) = files_lines.iter().position(|&line| line == "%FILES%") {
                let mut idx = files_idx + 1;
                while idx < files_lines.len() && !files_lines[idx].is_empty() {
                    let file = files_lines[idx];
                    // Ignore directories (they end with '/')
                    if !file.ends_with('/') {
                        files.push(file.to_string());
                    }
                    idx += 1;
                }
            }
        }

        Ok(PackageMetadata {
            name,
            version,
            base,
            description,
            url,
            architecture,
            build_date,
            install_date,
            packager,
            size,
            license,
            validation,
            depends,
            opt_depends,
            files,
        })
    }
}