use anyhow::Result;
use sandpolis::client::tui::server_list::ServerListWidget;
use sandpolis_database::TemporaryDatabase;
use sandpolis_group::{GroupLayer, config::GroupConfig, server::generate_test_group};
use sandpolis_instance::{InstanceId, InstanceLayer};
use sandpolis_network::{NetworkLayer, config::NetworkLayerConfig};
use sandpolis_server::ServerLayer;
use std::sync::Arc;

#[tokio::main]
async fn main() -> Result<()> {
    let temp_db = TemporaryDatabase::new()?;
    let tmp_certs = generate_test_group()?;
    let widget = ServerListWidget::new(ServerLayer::new(
        temp_db.db.document("/server")?,
        NetworkLayer::new(
            NetworkLayerConfig {
                servers: Some(vec!["127.0.0.1:1234".parse().unwrap()]),
                poll: None,
            },
            temp_db.db.document("/network")?,
            GroupLayer::new(
                GroupConfig {
                    certificate: Some(tmp_certs.into_path().join("client.cert")),
                },
                temp_db.db.document("/group")?,
                InstanceLayer::new(temp_db.db.document("/instance")?)?,
            )?,
        )?,
    )?);
    sandpolis_client::tui::test_widget(&widget).await.unwrap();
    Ok(())
}
