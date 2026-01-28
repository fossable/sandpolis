use bevy::prelude::bevy_main;
use sandpolis::{InstanceState, config::Configuration};
use sandpolis_instance::database::DatabaseLayer;
use std::path::PathBuf;

/// Get Android app's files directory using JNI
fn get_android_files_dir() -> Result<PathBuf, Box<dyn std::error::Error>> {
    use jni::JavaVM;
    use ndk_context::android_context;

    let ctx = android_context();
    let vm = unsafe { JavaVM::from_raw(ctx.vm().cast()) }?;
    let mut env = vm.attach_current_thread()?;

    // Get the Context object
    let context = unsafe { jni::objects::JObject::from_raw(ctx.context().cast()) };

    // Call getFilesDir() on the context
    let files_dir = env.call_method(context, "getFilesDir", "()Ljava/io/File;", &[])?;

    // Call getAbsolutePath() on the File object
    let path = env.call_method(
        files_dir.l()?,
        "getAbsolutePath",
        "()Ljava/lang/String;",
        &[],
    )?;

    // Convert Java String to Rust String
    let path_string: String = env.get_string(&path.l()?.into())?.into();

    Ok(PathBuf::from(path_string))
}

#[bevy_main]
pub fn main() {
    // Get ready to do some cryptography (ignore error if already installed)
    let _ = rustls::crypto::aws_lc_rs::default_provider().install_default();

    tokio::runtime::Builder::new_multi_thread()
        .enable_all()
        .build()
        .unwrap()
        .block_on(async {
            // Set initial configuration with Android app data directory
            let mut config = Configuration::default();

            // Use Android's app-specific data directory for database storage
            match get_android_files_dir() {
                Ok(files_dir) => {
                    config.database.storage = Some(files_dir);
                }
                Err(e) => {
                    eprintln!("Failed to get Android files directory: {}", e);
                    // Fallback to ephemeral database
                    config.database.ephemeral = true;
                }
            }

            // Load state
            let state = InstanceState::new(
                config.clone(),
                DatabaseLayer::new(config.database.clone(), &sandpolis::MODELS).unwrap(),
            )
            .await
            .unwrap();

            sandpolis::client::gui::main(config, state).await.unwrap();
        });
}
