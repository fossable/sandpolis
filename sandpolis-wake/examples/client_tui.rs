use anyhow::Result;
use sandpolis_instance::network::{ConnectionData, NetworkLayer, NetworkLayerData};
use sandpolis_instance::test_db;
use sandpolis_wake::{WakeLayer, client::tui::WakeWidget};

#[tokio::main]
async fn main() -> Result<()> {
    let database = test_db!(NetworkLayerData, ConnectionData);
    let widget = WakeWidget {
        wake: WakeLayer {
            network: NetworkLayer::new(database).await?,
        },
    };
    sandpolis_client::tui::test_widget(widget).await.unwrap();
    Ok(())
}
