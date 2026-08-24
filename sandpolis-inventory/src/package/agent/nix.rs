use super::PackageManager;
use crate::package::{PackageData, PackageManager as PM};
use crate::version::vercmp;
use anyhow::{Result, bail};
use std::cmp::Ordering;
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
        Ok(first.split_whitespace().last().unwrap_or(first).to_string())
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
        if let Some(map) = json.as_object()
            && let Some((_, entry)) = map.iter().next()
        {
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

        Ok(package_data)
    }

    async fn get_latest_available(&self, packages: &mut [PackageData]) -> Result<()> {
        // One eval per package: profiles are small and per-entry failures
        // shouldn't spoil the rest. The first eval after the registry's TTL
        // expires re-fetches the nixpkgs tarball, which can take a while but
        // is absorbed by the collection interval.
        for package in packages.iter_mut() {
            let Some(installable) = nixpkgs_installable(package) else {
                continue;
            };
            match self
                .exec_command(&[
                    "--extra-experimental-features",
                    "nix-command flakes",
                    "eval",
                    "--raw",
                    &installable,
                ])
                .await
            {
                Ok(output) => {
                    let version = output.trim();
                    if !version.is_empty() {
                        package.latest_available = Some(version.to_string());
                    }
                }
                Err(error) => {
                    debug!(package = %package.name, %error, "No available version from nixpkgs");
                }
            }
        }
        Ok(())
    }

    async fn get_outdated(&self) -> Result<Vec<PackageData>> {
        let mut packages = self.get_installed().await?;
        self.get_latest_available(&mut packages).await?;
        Ok(packages
            .into_iter()
            .filter(|p| {
                !p.version.is_empty()
                    && p.latest_available
                        .as_deref()
                        .is_some_and(|latest| vercmp(latest, &p.version) == Ordering::Greater)
            })
            .collect())
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
        let channel_status = Command::new("nix-channel").args(["--update"]).status();

        if let Ok(status) = channel_status
            && !status.success()
        {
            bail!("nix-channel --update failed with exit code: {}", status);
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
        manager: PM::Nix,
        ..Default::default()
    };

    // Store paths give us the pname and version. Multi-output packages list
    // paths like "-hello-2.12.1-man", so prefer the first path that actually
    // yields a version.
    let parsed: Vec<(String, Option<String>)> = entry
        .get("storePaths")
        .and_then(|v| v.as_array())
        .into_iter()
        .flatten()
        .filter_map(|v| v.as_str())
        .filter_map(parse_store_path)
        .collect();
    let mut pname = None;
    if let Some((n, v)) = parsed.iter().find(|(_, v)| v.is_some()) {
        pname = Some(n.clone());
        pkg.version = v.clone().unwrap_or_default();
    } else if let Some((n, _)) = parsed.first() {
        pname = Some(n.clone());
    }

    if let Some(url) = entry.get("originalUrl").and_then(|v| v.as_str()) {
        pkg.repository = Some(url.to_string());
    } else if let Some(url) = entry.get("url").and_then(|v| v.as_str()) {
        pkg.repository = Some(url.to_string());
    }

    // Prefer the attr path (minus the flake-output prefix) since it stays a
    // valid nixpkgs attribute; fall back to the store-path pname, then the
    // profile element's key.
    pkg.name = match entry.get("attrPath").and_then(|v| v.as_str()) {
        Some(attr) => strip_attr_prefix(attr).to_string(),
        None => pname.unwrap_or_else(|| name.to_string()),
    };

    pkg
}

/// Split a Nix store path like "/nix/store/abc...-firefox-128.0.3" into its
/// pname and version ("firefox", Some("128.0.3")). A trailing alphabetic
/// segment after the version (a multi-output suffix like "-man") is dropped.
fn parse_store_path(path: &str) -> Option<(String, Option<String>)> {
    let basename = path.rsplit('/').next()?;
    // Skip the 32-char hash + "-"
    let after_hash = basename.split_once('-')?.1;
    let parts: Vec<&str> = after_hash.split('-').collect();

    // The version starts at the first digit-leading segment after the pname.
    let Some(split) = parts
        .iter()
        .skip(1)
        .position(|p| p.starts_with(|c: char| c.is_ascii_digit()))
        .map(|i| i + 1)
    else {
        return Some((after_hash.to_string(), None));
    };

    let mut version_parts = &parts[split..];
    if version_parts.len() > 1
        && version_parts
            .last()
            .is_some_and(|p| p.chars().all(|c| c.is_ascii_alphabetic()))
    {
        version_parts = &version_parts[..version_parts.len() - 1];
    }
    Some((parts[..split].join("-"), Some(version_parts.join("-"))))
}

/// Remove the flake-output prefix ("legacyPackages.<system>." or
/// "packages.<system>.") from a profile entry's attr path.
fn strip_attr_prefix(attr: &str) -> &str {
    for prefix in ["legacyPackages.", "packages."] {
        if let Some(rest) = attr.strip_prefix(prefix)
            && let Some((_system, after)) = rest.split_once('.')
        {
            return after;
        }
    }
    attr
}

/// Whether a profile entry's flake ref points at nixpkgs.
fn is_nixpkgs_ref(url: &str) -> bool {
    let url = url.to_ascii_lowercase();
    url == "flake:nixpkgs"
        || url.starts_with("flake:nixpkgs/")
        || url == "nixpkgs"
        || url.starts_with("nixpkgs/")
        || url.contains("nixos/nixpkgs")
}

/// The `nix eval` installable that answers "what version of this package does
/// nixpkgs currently carry", or None when the package didn't come from
/// nixpkgs.
fn nixpkgs_installable(package: &PackageData) -> Option<String> {
    if package.name.is_empty() || !package.repository.as_deref().is_some_and(is_nixpkgs_ref) {
        return None;
    }
    Some(format!("nixpkgs#{}.version", package.name))
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn store_path_parsing() {
        assert_eq!(
            parse_store_path("/nix/store/abc123xyz-firefox-128.0.3"),
            Some(("firefox".to_string(), Some("128.0.3".to_string())))
        );
        assert_eq!(
            parse_store_path("/nix/store/h-coreutils-9.5"),
            Some(("coreutils".to_string(), Some("9.5".to_string())))
        );
        assert_eq!(
            parse_store_path("/nix/store/h-hello-2.12.1-man"),
            Some(("hello".to_string(), Some("2.12.1".to_string())))
        );
        assert_eq!(
            parse_store_path("/nix/store/h-python3-3.11.9"),
            Some(("python3".to_string(), Some("3.11.9".to_string())))
        );
        assert_eq!(
            parse_store_path("/nix/store/h-no-version"),
            Some(("no-version".to_string(), None))
        );
    }

    #[test]
    fn attr_prefix_stripping() {
        assert_eq!(strip_attr_prefix("legacyPackages.x86_64-linux.hello"), "hello");
        assert_eq!(
            strip_attr_prefix("legacyPackages.aarch64-darwin.python312Packages.requests"),
            "python312Packages.requests"
        );
        assert_eq!(strip_attr_prefix("packages.x86_64-linux.default"), "default");
        assert_eq!(strip_attr_prefix("hello"), "hello");
    }

    #[test]
    fn profile_entry_parsing() {
        let pkg = parse_profile_entry(
            "hello",
            &json!({
                "attrPath": "legacyPackages.x86_64-linux.hello",
                "originalUrl": "flake:nixpkgs",
                "storePaths": [
                    "/nix/store/abc123-hello-2.12.1-man",
                    "/nix/store/def456-hello-2.12.1"
                ]
            }),
        );
        assert_eq!(pkg.name, "hello");
        assert_eq!(pkg.version, "2.12.1");
        assert_eq!(pkg.repository.as_deref(), Some("flake:nixpkgs"));

        // Older array format entries may lack an attr path.
        let pkg = parse_profile_entry(
            "0",
            &json!({
                "url": "github:NixOS/nixpkgs/nixos-24.05",
                "storePaths": ["/nix/store/abc123-ripgrep-14.1.0"]
            }),
        );
        assert_eq!(pkg.name, "ripgrep");
        assert_eq!(pkg.version, "14.1.0");
    }

    #[test]
    fn nixpkgs_ref_detection() {
        assert!(is_nixpkgs_ref("flake:nixpkgs"));
        assert!(is_nixpkgs_ref("flake:nixpkgs/nixos-24.05"));
        assert!(is_nixpkgs_ref("nixpkgs"));
        assert!(is_nixpkgs_ref("github:NixOS/nixpkgs/nixos-unstable"));
        assert!(!is_nixpkgs_ref("github:user/repo"));
        assert!(!is_nixpkgs_ref("path:/home/user/flake"));
        assert!(!is_nixpkgs_ref(""));
    }

    #[test]
    fn installable_for_package() {
        let pkg = PackageData {
            name: "hello".to_string(),
            repository: Some("flake:nixpkgs".to_string()),
            ..Default::default()
        };
        assert_eq!(
            nixpkgs_installable(&pkg),
            Some("nixpkgs#hello.version".to_string())
        );

        let other_flake = PackageData {
            name: "tool".to_string(),
            repository: Some("github:user/repo".to_string()),
            ..Default::default()
        };
        assert_eq!(nixpkgs_installable(&other_flake), None);

        let no_repo = PackageData {
            name: "hello".to_string(),
            ..Default::default()
        };
        assert_eq!(nixpkgs_installable(&no_repo), None);

        let no_name = PackageData {
            repository: Some("flake:nixpkgs".to_string()),
            ..Default::default()
        };
        assert_eq!(nixpkgs_installable(&no_name), None);
    }
}
