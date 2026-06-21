use anyhow::Result;
use sandpolis_agent::AgentLayer;
use sandpolis_agent::wake::client::tui::WakeWidget;
use sandpolis_instance::network::{ConnectionData, NetworkLayerData};
use sandpolis_instance::test_db;

#[tokio::main]
async fn main() -> Result<()> {
    let database = test_db!(NetworkLayerData, ConnectionData);
    let widget = WakeWidget {
        agent: AgentLayer::new(database).await?,
    };
    sandpolis_client::tui::test_widget(widget).await.unwrap();
    Ok(())
}
