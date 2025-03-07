use anyhow::Result;
use sandpolis_database::TemporaryDatabase;
use sandpolis_instance::InstanceId;
use sandpolis_network::NetworkLayer;
use sandpolis_power::{PowerLayer, client::tui::PowerWidget};
use std::sync::Arc;

#[tokio::main]
async fn main() -> Result<()> {
    let temp_db = TemporaryDatabase::new()?;
    let widget = PowerWidget {
        instance: InstanceId::default(),
        power: PowerLayer {
            network: NetworkLayer {
                data: temp_db.db.document("/network")?,
                servers: Arc::new(Vec::new()),
            },
        },
    };
    sandpolis_client::tui::test_widget(&widget).await.unwrap();
    Ok(())
}
