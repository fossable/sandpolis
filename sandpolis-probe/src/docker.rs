//! Docker probe: reaches a device's Docker daemon so the health subsystem can
//! list and control its containers via [`crate::service`].

#![cfg(feature = "server")]

use crate::config::DockerProbeConfig;
use crate::service::{ContainerInfo, ServiceState};
use anyhow::{Context, Result};
use bollard::query_parameters::{
    ListContainersOptionsBuilder, RestartContainerOptionsBuilder, StartContainerOptions,
    StopContainerOptionsBuilder,
};
use bollard::{API_DEFAULT_VERSION, Docker};
use std::path::Path;

/// Seconds to wait for the daemon before giving up on a request.
const TIMEOUT: u64 = 30;

/// Seconds a container gets to exit gracefully before the daemon kills it.
const STOP_TIMEOUT: i32 = 10;

/// A handle on one device's Docker daemon (a cheap-to-clone HTTP client).
#[derive(Clone)]
pub struct DockerEngine {
    docker: Docker,
}

impl DockerEngine {
    /// Connect according to the probe's configured host URL.
    pub fn connect(config: &DockerProbeConfig) -> Result<Self> {
        let host = config.host.as_str();
        let docker = if host.is_empty() {
            Docker::connect_with_local_defaults()
        } else if host.starts_with("unix://") {
            Docker::connect_with_unix(host, TIMEOUT, API_DEFAULT_VERSION)
        } else if let (Some(key), Some(cert), Some(ca)) =
            (&config.tls_key, &config.tls_cert, &config.tls_ca_cert)
        {
            if config.tls_verify == Some(false) {
                tracing::warn!(
                    host,
                    "tls_verify=false is not supported; the server certificate \
                     will be verified against the configured CA"
                );
            }
            Docker::connect_with_ssl(
                host,
                Path::new(key),
                Path::new(cert),
                Path::new(ca),
                TIMEOUT,
                API_DEFAULT_VERSION,
            )
        } else {
            Docker::connect_with_http(host, TIMEOUT, API_DEFAULT_VERSION)
        }
        .with_context(|| format!("failed to connect to Docker at '{host}'"))?;

        Ok(Self { docker })
    }

    /// All containers on the daemon, running or not.
    pub async fn list(&self) -> Result<Vec<ContainerInfo>> {
        let containers = self
            .docker
            .list_containers(Some(
                ListContainersOptionsBuilder::default().all(true).build(),
            ))
            .await
            .context("failed to list containers")?;

        Ok(containers
            .into_iter()
            .map(|c| {
                let state = c
                    .state
                    .map(|s| s.to_string().to_lowercase())
                    .unwrap_or_default();
                ContainerInfo {
                    id: c.id.unwrap_or_default().chars().take(12).collect(),
                    name: c
                        .names
                        .unwrap_or_default()
                        .first()
                        .map(|n| n.trim_start_matches('/').to_string())
                        .unwrap_or_default(),
                    image: c.image.unwrap_or_default(),
                    state: match state.as_str() {
                        "running" => ServiceState::Running,
                        "paused" => ServiceState::Paused,
                        "exited" | "created" | "dead" => ServiceState::Stopped,
                        _ => ServiceState::Other,
                    },
                    status: c.status,
                }
            })
            .collect())
    }

    pub async fn start(&self, id: &str) -> Result<()> {
        self.docker
            .start_container(id, None::<StartContainerOptions>)
            .await
            .with_context(|| format!("failed to start container {id}"))
    }

    pub async fn stop(&self, id: &str) -> Result<()> {
        self.docker
            .stop_container(
                id,
                Some(
                    StopContainerOptionsBuilder::default()
                        .t(STOP_TIMEOUT)
                        .build(),
                ),
            )
            .await
            .with_context(|| format!("failed to stop container {id}"))
    }

    pub async fn restart(&self, id: &str) -> Result<()> {
        self.docker
            .restart_container(
                id,
                Some(
                    RestartContainerOptionsBuilder::default()
                        .t(STOP_TIMEOUT)
                        .build(),
                ),
            )
            .await
            .with_context(|| format!("failed to restart container {id}"))
    }
}
