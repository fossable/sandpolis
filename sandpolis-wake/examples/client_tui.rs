use anyhow::Result;
use sandpolis_core::InstanceId;
use sandpolis_network::NetworkLayer;
use sandpolis_wake::{WakeLayer, client::tui::WakeWidget};
use std::sync::Arc;

#[tokio::main]
async fn main() -> Result<()> {
    let temp_db = TemporaryDatabase::new()?;
    let widget = WakeWidget {
        instance: InstanceId::default(),
        wake: WakeLayer {
            network: NetworkLayer {
                data: temp_db.db.document("/network")?,
                servers: Arc::new(Vec::new()),
            },
        },
    };
    sandpolis_client::tui::test_widget(&widget).await.unwrap();
    Ok(())
}
