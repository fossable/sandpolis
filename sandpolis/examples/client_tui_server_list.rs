/// Interactive server list TUI backed by a throwaway test server.
///
/// The list starts empty; press the "add" key to enter the test server's
/// address (printed below) and log in.
use anyhow::Result;
use sandpolis::{
    InstanceState, MODELS, RuntimeOptions, client::tui::server_list::ServerListWidget,
    server::test_server,
};
use sandpolis_instance::database::{DatabaseManager, WriteAuthority};
use sandpolis_instance::realm::{RealmCert, RealmManager};
use sandpolis_server::ServerStratum;

#[tokio::main]
async fn main() -> Result<()> {
    let test_server = test_server().await?;
    println!("Test server listening on 127.0.0.1:{}", test_server.port);

    // Client-side state, kept entirely in memory
    let mut options = RuntimeOptions::embedded();
    options.database.storage = None;

    // The test server hands out a realm cert, which is the whole trust
    // bootstrap a client needs.
    let cert = RealmCert::read_pem(&test_server.endpoint_cert)?;

    let database = DatabaseManager::new(options.database.clone(), &MODELS, WriteAuthority::Full)?;
    let realms = RealmManager::for_endpoint(vec![cert], database.clone())?;
    let state = InstanceState::new(&options, database, realms, ServerStratum::Global).await?;

    let widget = ServerListWidget::new(state.server.clone())?;
    sandpolis_client::tui::test_widget(widget).await.unwrap();
    Ok(())
}
