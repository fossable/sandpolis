use super::PackageManager;
use crate::package::{PackageData, PackageManager as PM};
use anyhow::{Result, bail};
use std::path::PathBuf;
use std::process::Command;
use tracing::debug;

#[derive(Debug, Clone)]
pub struct Nix {
    executable: PathBuf,
}

impl Nix {
    pub fn new() -> Result<Self> {
        let executable =
            which::which("nix").map_err(|_| anyhow::anyhow!("nix not found in PATH"))?;
        Ok(Self { executable })
    }

    pub fn is_available() -> bool {
        which::which("nix").is_ok()
    }

    async fn exec_command(&self, args: &[&str]) -> Result<String> {
        let output = Command::new(&self.executable).args(args).output()?;

        if !output.status.success() {
            bail!("nix command failed with exit code: {}", output.status);
        }

        Ok(String::from_utf8_lossy(&output.stdout).to_string())
    }
}

impl PackageManager for Nix {
    fn get_location(&self) -> Result<PathBuf> {
        Ok(self.executable.clone())
    }

    async fn get_version(&self) -> Result<String> {
        let output = self.exec_command(&["--version"]).await?;
        // Output looks like: "nix (Nix) 2.18.1"
        let first = output.lines().next().unwrap_or("");
        Ok(first
            .split_whitespace()
            .last()
            .unwrap_or(first)
            .to_string())
    }

    async fn clean(&self) -> Result<()> {
        debug!("Running nix store garbage collection");

        let output = Command::new("nix-collect-garbage").output()?;

        if !output.status.success() {
            bail!(
                "nix-collect-garbage failed with exit code: {}",
                output.status
            );
        }

        Ok(())
    }

    async fn get_installed(&self) -> Result<Vec<PackageData>> {
        let stdout = self
            .exec_command(&[
                "--extra-experimental-features",
                "nix-command flakes",
                "profile",
                "list",
                "--json",
            ])
            .await?;
        let json: serde_json::Value = serde_json::from_str(&stdout)?;

        let mut packages = Vec::new();
        if let Some(elements) = json.get("elements") {
            // Nix 2.20+ format: { "elements": { "name": {...} } }
            if let Some(map) = elements.as_object() {
                for (name, entry) in map {
                    packages.push(parse_profile_entry(name, entry));
                }
            // Older format: { "elements": [ {...} ] }
            } else if let Some(arr) = elements.as_array() {
                for entry in arr {
                    let name = entry
                        .get("attrPath")
                        .and_then(|v| v.as_str())
                        .or_else(|| entry.get("name").and_then(|v| v.as_str()))
                        .unwrap_or("")
                        .to_string();
                    packages.push(parse_profile_entry(&name, entry));
                }
            }
        }

        Ok(packages)
    }

    async fn get_metadata(&self, name: String) -> Result<PackageData> {
        let stdout = self
            .exec_command(&[
                "--extra-experimental-features",
                "nix-command flakes",
                "search",
                "nixpkgs",
                &name,
                "--json",
            ])
            .await?;
        let json: serde_json::Value = serde_json::from_str(&stdout)?;

        let mut package_data = PackageData {
            name: name.clone(),
            manager: PM::Nix,
            ..Default::default()
        };

        // Pick the first match (search returns a map keyed by attr path)
        if let Some(map) = json.as_object() {
            if let Some((_, entry)) = map.iter().next() {
                if let Some(v) = entry.get("version").and_then(|v| v.as_str()) {
                    package_data.version = v.to_string();
                    package_data.latest_available = Some(v.to_string());
                }
                if let Some(d) = entry.get("description").and_then(|v| v.as_str()) {
                    package_data.description = Some(d.to_string());
                }
                if let Some(p) = entry.get("pname").and_then(|v| v.as_str()) {
                    package_data.name = p.to_string();
                }
            }
        }

        Ok(package_data)
    }

    async fn get_outdated(&self) -> Result<Vec<PackageData>> {
        // `nix profile list` doesn't directly report outdated packages.
        // A check would require `nix profile upgrade --dry-run` or comparing
        // current store paths against the latest available — not yet
        // implemented here.
        bail!("Not implemented for nix");
    }

    async fn install(&self, packages: Vec<String>) -> Result<()> {
        debug!("Installing {} nix packages", packages.len());

        let mut args = vec![
            "--extra-experimental-features",
            "nix-command flakes",
            "profile",
            "install",
        ];
        // Each package becomes nixpkgs#<name>
        let qualified: Vec<String> = packages
            .iter()
            .map(|p| {
                if p.contains('#') {
                    p.clone()
                } else {
                    format!("nixpkgs#{}", p)
                }
            })
            .collect();
        args.extend(qualified.iter().map(|s| s.as_str()));
        self.exec_command(&args).await?;
        Ok(())
    }

    async fn refresh(&self) -> Result<()> {
        debug!("Refreshing nix channels / flake registry");

        // For non-flake setups, refresh channels.
        let channel_status = Command::new("nix-channel").args(&["--update"]).status();

        if let Ok(status) = channel_status {
            if !status.success() {
                bail!("nix-channel --update failed with exit code: {}", status);
            }
        }

        Ok(())
    }

    async fn remove(&self, packages: Vec<String>) -> Result<()> {
        debug!("Removing {} nix packages", packages.len());

        let mut args = vec![
            "--extra-experimental-features",
            "nix-command flakes",
            "profile",
            "remove",
        ];
        args.extend(packages.iter().map(|s| s.as_str()));
        self.exec_command(&args).await?;
        Ok(())
    }

    async fn upgrade(&self, packages: Vec<String>) -> Result<()> {
        debug!("Upgrading {} nix packages", packages.len());

        let mut args = vec![
            "--extra-experimental-features",
            "nix-command flakes",
            "profile",
            "upgrade",
        ];
        if packages.is_empty() {
            // Upgrade everything
            args.push(".*");
        } else {
            args.extend(packages.iter().map(|s| s.as_str()));
        }
        self.exec_command(&args).await?;
        Ok(())
    }
}

fn parse_profile_entry(name: &str, entry: &serde_json::Value) -> PackageData {
    let mut pkg = PackageData {
        name: name.to_string(),
        manager: PM::Nix,
        ..Default::default()
    };

    // Store paths give us the version when present.
    if let Some(store_paths) = entry.get("storePaths").and_then(|v| v.as_array()) {
        if let Some(first) = store_paths.iter().filter_map(|v| v.as_str()).next() {
            if let Some(version) = parse_version_from_store_path(first) {
                pkg.version = version;
            }
        }
    }

    if let Some(url) = entry.get("originalUrl").and_then(|v| v.as_str()) {
        pkg.repository = Some(url.to_string());
    } else if let Some(url) = entry.get("url").and_then(|v| v.as_str()) {
        pkg.repository = Some(url.to_string());
    }

    if let Some(attr) = entry.get("attrPath").and_then(|v| v.as_str()) {
        pkg.name = attr.to_string();
    }

    pkg
}

/// Extract a version from a Nix store path like
/// "/nix/store/abc...-firefox-128.0.3" → "128.0.3"
fn parse_version_from_store_path(path: &str) -> Option<String> {
    let basename = path.rsplit('/').next()?;
    // Skip the 32-char hash + "-"
    let after_hash = basename.split_once('-')?.1;
    // The version is the trailing part starting with a digit
    let mut parts = after_hash.rsplitn(2, '-');
    let last = parts.next()?;
    if last.chars().next().map(|c| c.is_ascii_digit()).unwrap_or(false) {
        Some(last.to_string())
    } else {
        None
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn version_from_store_path() {
        assert_eq!(
            parse_version_from_store_path(
                "/nix/store/abc123xyz-firefox-128.0.3"
            ),
            Some("128.0.3".to_string())
        );
        assert_eq!(
            parse_version_from_store_path("/nix/store/h-coreutils-9.5"),
            Some("9.5".to_string())
        );
        assert_eq!(
            parse_version_from_store_path("/nix/store/h-no-version"),
            None
        );
    }
}
