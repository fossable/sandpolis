use anyhow::Result;
use sandpolis_agent::AgentManager;
use sandpolis_agent::wake::client::tui::WakeWidget;
use sandpolis_instance::network::{ConnectionData, NetworkManagerData};
use sandpolis_instance::test_db;

#[tokio::main]
async fn main() -> Result<()> {
    let database = test_db!(NetworkManagerData, ConnectionData);
    let widget = WakeWidget {
        agent: AgentManager::new(database).await?,
    };
    sandpolis_client::tui::test_widget(widget).await.unwrap();
    Ok(())
}
