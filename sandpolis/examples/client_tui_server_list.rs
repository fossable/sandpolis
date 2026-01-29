use anyhow::Result;
use sandpolis::{client::tui::server_list::ServerListWidget, server::test_server};
use sandpolis_instance::database::TemporaryDatabase;
use sandpolis_instance::network::{NetworkLayer, config::NetworkLayerConfig};
use sandpolis_instance::realm::{RealmLayer, config::RealmConfig};
use sandpolis_instance::{InstanceId, InstanceLayer};
use sandpolis_server::ServerLayer;
use std::sync::Arc;

#[tokio::main]
async fn main() -> Result<()> {
    let test_server = test_server().await?;

    let temp_db = TemporaryDatabase::new()?;
    let widget = ServerListWidget::new(
        ServerLayer::new(
            temp_db.db.document("/server")?,
            NetworkLayer::new(
                NetworkLayerConfig {
                    servers: Some(vec![
                        format!("127.0.0.1:{}", test_server.port).parse().unwrap(),
                    ]),
                    poll: None,
                },
                temp_db.db.document("/network")?,
                RealmLayer::new(
                    RealmConfig {
                        certificate: Some(test_server.endpoint_cert),
                    },
                    temp_db.db.document("/realm")?,
                    InstanceLayer::new(temp_db.db.document("/instance")?)?,
                )?,
            )?,
        )
        .await?,
    )?;
    sandpolis_client::tui::test_widget(widget).await.unwrap();
    Ok(())
}
