use anyhow::Result;
use sandpolis_power::client::tui::PowerWidget;

#[tokio::main]
async fn main() -> Result<()> {
    let widget = PowerWidget {
        instance: todo!(),
        power: todo!(),
    };
    sandpolis_client::tui::test_widget(widget).await.unwrap();
    Ok(())
}
