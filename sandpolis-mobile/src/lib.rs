use bevy::prelude::bevy_main;
use sandpolis::cli::CommandLine;

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
            // Load state
            let state = InstanceState::new(
                config.clone(),
                DatabaseLayer::new(config.database.clone(), &*sandpolis::MODELS)?,
            )
            .await?;

            // Set initial configuration
            let config = Configuration::default();

            sandpolis::client::gui::main(config, state).await.unwrap();
        });
}
