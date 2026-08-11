/// Interactive server list TUI backed by a throwaway test server.
///
/// The list starts empty; press the "add" key to enter the test server's
/// address (printed below) and log in.
use anyhow::Result;
use sandpolis::{
    InstanceState, MODELS, client::tui::server_list::ServerListWidget, config::Configuration,
    server::test_server,
};
use sandpolis_instance::database::{DatabaseLayer, WriteAuthority};
use sandpolis_server::ServerStratum;

#[tokio::main]
async fn main() -> Result<()> {
    let test_server = test_server().await?;
    println!("Test server listening on 127.0.0.1:{}", test_server.port);

    // Client-side state, kept entirely in memory
    let mut config = Configuration::default();
    config.database.ephemeral = true;
    config.realm.realm_certs = vec![test_server.endpoint_cert.clone()];

    let database = DatabaseLayer::new(config.database.clone(), &MODELS, WriteAuthority::Full)?;
    let state = InstanceState::new(config, database, ServerStratum::Global).await?;

    let widget = ServerListWidget::new(state.server.clone())?;
    sandpolis_client::tui::test_widget(widget).await.unwrap();
    Ok(())
}
