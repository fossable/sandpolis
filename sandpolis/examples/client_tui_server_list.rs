use anyhow::Result;
use sandpolis_database::TemporaryDatabase;
use sandpolis_instance::InstanceId;
use sandpolis_network::NetworkLayer;
use std::sync::Arc;

#[tokio::main]
async fn main() -> Result<()> {
    let temp_db = TemporaryDatabase::new()?;
    let widget = ServerListWidget {};
    sandpolis_client::tui::test_widget(&widget).await.unwrap();
    Ok(())
}
