use serde::{Deserialize, Serialize};

#[cfg(feature = "client")]
pub mod client;
pub mod streams;

#[derive(Clone, Serialize, Deserialize)]
pub enum WakeAction {
    Poweroff,
    Reboot,
}

/// Initiate a power state change on the local system.
#[cfg(feature = "agent")]
pub async fn change_power_state(action: &WakeAction) -> anyhow::Result<()> {
    use anyhow::bail;
    use tokio::process::Command;

    let (systemctl_verb, shutdown_arg) = match action {
        WakeAction::Poweroff => ("poweroff", "-h"),
        WakeAction::Reboot => ("reboot", "-r"),
    };

    // Prefer systemctl when available, falling back to the classic `shutdown`.
    let status = Command::new("systemctl").arg(systemctl_verb).status().await;
    if let Ok(status) = status
        && status.success()
    {
        return Ok(());
    }

    let status = Command::new("shutdown")
        .arg(shutdown_arg)
        .arg("now")
        .status()
        .await?;
    if !status.success() {
        bail!("failed to {systemctl_verb}: shutdown exited with {status}");
    }
    Ok(())
}
