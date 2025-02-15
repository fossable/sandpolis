use anyhow::Result;
use std::path::Path;
use tokio::fs::File;
use tokio::io::AsyncWriteExt;

/// Wipe free space on the given filesystem. This can significantly reduce the
/// size of subsequent snapshots. Don't use with software-based encryption schemes
/// because it might make snapshots needlessly larger.
pub async fn wipe_free<P>(path: P) -> Result<()>
where
    P: AsRef<Path>,
{
    let path = path.as_ref().join(".blank");
    let mut file = File::create(&path).await?;
    let zero = [0u8; 4096];

    match file.write_all(&zero).await {
        Ok(_) => {}
        Err(_) => {}
    }

    tokio::fs::remove_file(&path).await?;
    Ok(())
}
