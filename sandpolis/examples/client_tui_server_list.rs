use anyhow::Result;
use sandpolis::{client::tui::server_list::ServerListWidget, server::test_server};
use sandpolis_database::TemporaryDatabase;
use sandpolis_group::{GroupLayer, config::GroupConfig};
use sandpolis_instance::{InstanceId, InstanceLayer};
use sandpolis_network::{NetworkLayer, config::NetworkLayerConfig};
use sandpolis_server::ServerLayer;
use std::sync::Arc;

#[tokio::main]
async fn main() -> Result<()> {
    let test_server = test_server().await?;

    let temp_db = TemporaryDatabase::new()?;
    let widget = ServerListWidget::new(ServerLayer::new(
        temp_db.db.document("/server")?,
        NetworkLayer::new(
            NetworkLayerConfig {
                servers: Some(vec![
                    format!("127.0.0.1:{}", test_server.port).parse().unwrap(),
                ]),
                poll: None,
            },
            temp_db.db.document("/network")?,
            GroupLayer::new(
                GroupConfig {
                    certificate: Some(test_server.endpoint_cert),
                },
                temp_db.db.document("/group")?,
                InstanceLayer::new(temp_db.db.document("/instance")?)?,
            )?,
        )?,
    )?);
    sandpolis_client::tui::test_widget(widget).await.unwrap();
    Ok(())
}
