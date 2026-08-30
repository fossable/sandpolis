//! libvirt probe: reaches a device's libvirt daemon so the health subsystem can
//! list and control its virtual machines via [`crate::service`].
//!
//! Everything shells out to `virsh` (precedent: the snapshot store and
//! `qemu-img`), which speaks every libvirt transport — including remote
//! `qemu+ssh://` URIs — without linking the libvirt C library.

#![cfg(feature = "server")]

use crate::config::LibvirtProbeConfig;
use crate::service::{DomainInfo, ServiceState};
use anyhow::{Context, Result, bail};
use tokio::process::Command;

/// The connection URI to hand virsh: the configured URI with the configured
/// username and private key folded in, and interactive prompts disabled.
pub fn effective_uri(config: &LibvirtProbeConfig) -> Result<String> {
    let mut uri = url::Url::parse(&config.uri)
        .with_context(|| format!("invalid libvirt URI '{}'", config.uri))?;

    if uri.scheme().contains("+ssh") {
        if let Some(username) = &config.username
            && uri.username().is_empty()
        {
            uri.set_username(username)
                .map_err(|_| anyhow::anyhow!("cannot set username on '{}'", config.uri))?;
        }
        let mut query = uri.query_pairs_mut();
        if let Some(key) = &config.private_key_path {
            query.append_pair("keyfile", key);
        }
        // Fail instead of prompting; nobody is at the server's terminal.
        query.append_pair("no_tty", "1");
        drop(query);
    }

    Ok(uri.to_string())
}

async fn run_virsh(config: &LibvirtProbeConfig, args: &[&str]) -> Result<String> {
    let uri = effective_uri(config)?;
    let output = Command::new("virsh")
        .arg("-c")
        .arg(&uri)
        .arg("--quiet")
        .args(args)
        .output()
        .await
        .context("failed to execute virsh (is libvirt installed?)")?;

    if !output.status.success() {
        bail!(
            "virsh {} failed: {}",
            args.first().unwrap_or(&""),
            String::from_utf8_lossy(&output.stderr).trim()
        );
    }
    Ok(String::from_utf8_lossy(&output.stdout).to_string())
}

/// Parse `virsh list --all` output into domains.
///
/// Each line is `<id> <name> <state...>` where id is `-` for inactive domains
/// and the state text may contain spaces ("shut off"). With `--quiet` there is
/// no header, but tolerate one anyway.
fn parse_list(output: &str) -> Vec<DomainInfo> {
    output
        .lines()
        .map(str::trim)
        .filter(|line| {
            !line.is_empty() && !line.starts_with("Id") && !line.chars().all(|c| c == '-')
        })
        .filter_map(|line| {
            let mut parts = line.split_whitespace();
            let _id = parts.next()?;
            let name = parts.next()?.to_string();
            let state = parts.collect::<Vec<_>>().join(" ");
            Some(DomainInfo {
                name,
                uuid: None,
                state: match state.as_str() {
                    "running" => ServiceState::Running,
                    "paused" => ServiceState::Paused,
                    "shut off" => ServiceState::Stopped,
                    _ => ServiceState::Other,
                },
                status: Some(state),
            })
        })
        .collect()
}

/// All domains the daemon knows, running or not.
pub async fn list(config: &LibvirtProbeConfig) -> Result<Vec<DomainInfo>> {
    let output = run_virsh(config, &["list", "--all"]).await?;
    let mut domains = parse_list(&output);
    for domain in &mut domains {
        // Best-effort; the name is what actions address anyway.
        if let Ok(uuid) = run_virsh(config, &["domuuid", &domain.name]).await {
            let uuid = uuid.trim();
            if !uuid.is_empty() {
                domain.uuid = Some(uuid.to_string());
            }
        }
    }
    Ok(domains)
}

pub async fn start(config: &LibvirtProbeConfig, name: &str) -> Result<()> {
    run_virsh(config, &["start", name]).await.map(|_| ())
}

/// Request a graceful shutdown. The domain may take a while to comply; callers
/// re-list to observe progress.
pub async fn shutdown(config: &LibvirtProbeConfig, name: &str) -> Result<()> {
    run_virsh(config, &["shutdown", name]).await.map(|_| ())
}

/// Pull the plug.
pub async fn destroy(config: &LibvirtProbeConfig, name: &str) -> Result<()> {
    run_virsh(config, &["destroy", name]).await.map(|_| ())
}

pub async fn reboot(config: &LibvirtProbeConfig, name: &str) -> Result<()> {
    run_virsh(config, &["reboot", name]).await.map(|_| ())
}

#[cfg(test)]
mod tests {
    use super::*;

    fn config(uri: &str, username: Option<&str>, key: Option<&str>) -> LibvirtProbeConfig {
        LibvirtProbeConfig {
            uri: uri.to_string(),
            username: username.map(String::from),
            private_key_path: key.map(String::from),
        }
    }

    #[test]
    fn effective_uri_passes_local_through() {
        assert_eq!(
            effective_uri(&config("test:///default", None, None)).unwrap(),
            "test:///default"
        );
        assert_eq!(
            effective_uri(&config("qemu:///system", Some("admin"), None)).unwrap(),
            "qemu:///system"
        );
    }

    #[test]
    fn effective_uri_decorates_ssh() {
        let uri = effective_uri(&config(
            "qemu+ssh://host/system",
            Some("admin"),
            Some("/keys/id_ed25519"),
        ))
        .unwrap();
        assert!(uri.starts_with("qemu+ssh://admin@host/system?"));
        assert!(uri.contains("keyfile=%2Fkeys%2Fid_ed25519"));
        assert!(uri.contains("no_tty=1"));
    }

    #[test]
    fn effective_uri_keeps_existing_username() {
        let uri =
            effective_uri(&config("qemu+ssh://root@host/system", Some("admin"), None)).unwrap();
        assert!(uri.starts_with("qemu+ssh://root@host/system"));
    }

    #[test]
    fn parses_quiet_list_output() {
        let domains = parse_list(" 1    test    running\n -    vm2     shut off\n");
        assert_eq!(domains.len(), 2);
        assert_eq!(domains[0].name, "test");
        assert_eq!(domains[0].state, ServiceState::Running);
        assert_eq!(domains[1].name, "vm2");
        assert_eq!(domains[1].state, ServiceState::Stopped);
        assert_eq!(domains[1].status.as_deref(), Some("shut off"));
    }

    #[test]
    fn parses_headered_list_output() {
        let output = " Id   Name   State\n----------------------\n 1    test   running\n";
        let domains = parse_list(output);
        assert_eq!(domains.len(), 1);
        assert_eq!(domains[0].name, "test");
    }
}
