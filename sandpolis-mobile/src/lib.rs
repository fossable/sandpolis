use bevy::prelude::bevy_main;
use sandpolis::{InstanceState, cli::CommandLine, config::Configuration};
use sandpolis_database::DatabaseLayer;

#[bevy_main]
pub fn main() {
    // Get ready to do some cryptography
    rustls::crypto::aws_lc_rs::default_provider()
        .install_default()
        .expect("crypto provider is available");

    tokio::runtime::Builder::new_multi_thread()
        .enable_all()
        .build()
        .unwrap()
        .block_on(async {
            // Set initial configuration
            let config = Configuration::default();

            // Load state
            let state = InstanceState::new(
                config.clone(),
                DatabaseLayer::new(config.database.clone(), &*sandpolis::MODELS).unwrap(),
            )
            .await?;

            sandpolis::client::gui::main(config, state).await.unwrap();
        });
}
