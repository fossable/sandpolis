//! The on-disk snapshot store, built on `qemu-img`.
//!
//! Layout: `<root>/<agent instance id>/<partition uuid>/<snapshot uuid>.qcow2`.
//!
//! Each snapshot is one qcow2 layer with zstd-compressed clusters. The base is
//! standalone; every later layer names its predecessor as a (relative) backing
//! file, so `qemu-img convert -B` stores only the clusters that changed. The
//! working copy a transfer runs against is a sparse raw staging file in the
//! same directory, reconstructed from the chain and deleted after the commit.

use anyhow::{Context, Result, bail};
use sandpolis_instance::InstanceId;
use std::path::{Path, PathBuf};
use tokio::process::Command;
use tracing::debug;

#[derive(Clone)]
pub struct SnapshotStore {
    root: PathBuf,
}

impl SnapshotStore {
    pub fn new(root: PathBuf) -> Self {
        Self { root }
    }

    /// Directory holding one partition's snapshot chain.
    pub fn dir(&self, agent: InstanceId, partition_uuid: &str) -> PathBuf {
        self.root.join(agent.to_string()).join(partition_uuid)
    }

    /// Path of one stored layer.
    pub fn layer_path(&self, agent: InstanceId, partition_uuid: &str, snapshot: &str) -> PathBuf {
        self.dir(agent, partition_uuid)
            .join(format!("{snapshot}.qcow2"))
    }

    /// Create an empty sparse staging file (the comparison image for a base
    /// capture, before the partition size is known).
    pub async fn new_staging(&self, agent: InstanceId, partition_uuid: &str) -> Result<PathBuf> {
        let dir = self.dir(agent, partition_uuid);
        tokio::fs::create_dir_all(&dir).await?;
        let staging = dir.join(format!(".staging-{}.raw", uuid::Uuid::now_v7()));
        tokio::fs::File::create(&staging).await?;
        Ok(staging)
    }

    /// Reconstruct a snapshot into a sparse raw staging file. The backing chain
    /// resolves through the relative names recorded at commit time.
    pub async fn reconstruct(
        &self,
        agent: InstanceId,
        partition_uuid: &str,
        snapshot: &str,
    ) -> Result<PathBuf> {
        let dir = self.dir(agent, partition_uuid);
        let staging = dir.join(format!(".staging-{}.raw", uuid::Uuid::now_v7()));
        let staging_name = staging.file_name().unwrap().to_string_lossy().to_string();

        run_qemu_img(
            &dir,
            &[
                "convert",
                "-O",
                "raw",
                &format!("{snapshot}.qcow2"),
                &staging_name,
            ],
        )
        .await
        .context("Failed to reconstruct snapshot")?;
        Ok(staging)
    }

    /// Convert a staged raw image into a new qcow2 layer, returning its size on
    /// disk. The staging file is deleted afterwards.
    pub async fn commit(
        &self,
        agent: InstanceId,
        partition_uuid: &str,
        staging: &Path,
        snapshot: &str,
        parent: Option<&str>,
    ) -> Result<u64> {
        let dir = self.dir(agent, partition_uuid);
        let staging_name = staging
            .file_name()
            .and_then(|n| n.to_str())
            .context("Staging path has no name")?
            .to_string();
        let layer = format!("{snapshot}.qcow2");
        let backing;
        let mut args = vec![
            "convert",
            "-O",
            "qcow2",
            "-c",
            "-o",
            "compression_type=zstd",
        ];
        if let Some(parent) = parent {
            backing = format!("{parent}.qcow2");
            args.extend_from_slice(&["-B", &backing, "-F", "qcow2"]);
        }
        args.push(&staging_name);
        args.push(&layer);

        let result = run_qemu_img(&dir, &args)
            .await
            .context("Failed to commit snapshot layer");
        let _ = tokio::fs::remove_file(staging).await;
        result?;

        Ok(tokio::fs::metadata(dir.join(&layer)).await?.len())
    }

    /// Delete a stored layer. Only safe on leaves; the caller checks nothing
    /// backs onto it.
    pub async fn delete_layer(
        &self,
        agent: InstanceId,
        partition_uuid: &str,
        snapshot: &str,
    ) -> Result<()> {
        tokio::fs::remove_file(self.layer_path(agent, partition_uuid, snapshot)).await?;
        Ok(())
    }

    /// Report the qemu-img version, verifying the binary is reachable at all.
    pub async fn check_qemu() -> Result<String> {
        let output = Command::new("qemu-img").arg("--version").output().await?;
        if !output.status.success() {
            bail!("qemu-img --version exited with {}", output.status);
        }
        Ok(String::from_utf8_lossy(&output.stdout).trim().to_string())
    }
}

/// Run qemu-img in `dir`, so backing files stay relative and the chain remains
/// relocatable.
async fn run_qemu_img(dir: &Path, args: &[&str]) -> Result<()> {
    debug!(?args, ?dir, "Running qemu-img");
    let output = Command::new("qemu-img")
        .args(args)
        .current_dir(dir)
        .output()
        .await
        .context("Failed to run qemu-img; is it installed?")?;
    if !output.status.success() {
        bail!(
            "qemu-img {} failed: {}",
            args.first().unwrap_or(&""),
            String::from_utf8_lossy(&output.stderr).trim()
        );
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::{Seek, SeekFrom, Write};

    /// Round-trip a base + incremental chain through commit and reconstruct.
    #[tokio::test]
    async fn test_commit_reconstruct_roundtrip() -> Result<()> {
        let root = tempfile::tempdir()?;
        let store = SnapshotStore::new(root.path().to_path_buf());
        let agent = InstanceId::new(sandpolis_instance::InstanceType::Agent);
        let partition = "cafe0000-0000-0000-0000-000000000001";

        // Base image: 4 MiB with a marker at 1 MiB
        let staging = store.new_staging(agent, partition).await?;
        {
            let mut f = std::fs::OpenOptions::new().write(true).open(&staging)?;
            f.set_len(4 << 20)?;
            f.seek(SeekFrom::Start(1 << 20))?;
            f.write_all(b"base marker")?;
        }
        let stored = store
            .commit(agent, partition, &staging, "base", None)
            .await?;
        assert!(stored > 0);
        assert!(!staging.exists());

        // Incremental: change a different region
        let staging = store.reconstruct(agent, partition, "base").await?;
        {
            let mut f = std::fs::OpenOptions::new().write(true).open(&staging)?;
            f.seek(SeekFrom::Start(2 << 20))?;
            f.write_all(b"incremental marker")?;
        }
        store
            .commit(agent, partition, &staging, "second", Some("base"))
            .await?;

        // The incremental layer resolves to base content + its own change
        let rebuilt = store.reconstruct(agent, partition, "second").await?;
        let data = std::fs::read(&rebuilt)?;
        assert_eq!(data.len(), 4 << 20);
        assert_eq!(&data[1 << 20..(1 << 20) + 11], b"base marker");
        assert_eq!(&data[2 << 20..(2 << 20) + 18], b"incremental marker");

        // Deleting the leaf leaves the base reconstructible
        store.delete_layer(agent, partition, "second").await?;
        store.reconstruct(agent, partition, "base").await?;
        Ok(())
    }
}
