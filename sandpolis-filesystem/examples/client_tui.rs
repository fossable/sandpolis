use anyhow::Result;
use sandpolis_core::InstanceId;
use sandpolis_filesystem::{FilesystemLayer, client::tui::FilesystemViewerWidget};
use std::env;
use std::path::PathBuf;

#[tokio::main]
async fn main() -> Result<()> {
    let filesystem = FilesystemLayer::new().await?;

    let initial_path = if let Some(path_arg) = env::args().nth(1) {
        PathBuf::from(path_arg)
    } else {
        env::current_dir().unwrap_or_else(|_| PathBuf::from("/"))
    };

    let widget =
        FilesystemViewerWidget::new(InstanceId::new_server(), filesystem, Some(initial_path));

    sandpolis_client::tui::test_widget(widget).await.unwrap();

    Ok(())
}
