use anyhow::{bail, Result};
use clap::Parser;
use std::{path::PathBuf, str::FromStr};

fn parse_storage_dir(value: &str) -> Result<PathBuf> {
    let path = PathBuf::from_str(value)?;

    // If it's not a directory, create it
    if !std::fs::exists(&path)? {
        std::fs::create_dir_all(&path)?;
    }
    if !std::fs::metadata(&path)?.is_dir() {
        bail!("Storage directory must be a directory");
    }
    Ok(path)
}

fn default_storage_dir() -> PathBuf {
    "/tmp".into()
}

#[derive(Parser, Debug, Clone)]
pub struct DatabaseCommandLine {
    /// Storage directory
    #[clap(long, value_parser = parse_storage_dir, default_value = default_storage_dir().into_os_string())]
    pub storage: PathBuf,
}
