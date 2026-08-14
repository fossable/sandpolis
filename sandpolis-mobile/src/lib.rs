use bevy::prelude::bevy_main;
use sandpolis::{InstanceState, RuntimeOptions};
use sandpolis_instance::database::DatabaseLayer;
use std::path::PathBuf;

/// Get Android app's files directory using JNI
fn get_android_files_dir() -> Result<PathBuf, Box<dyn std::error::Error>> {
    use jni::JavaVM;
    use ndk_context::android_context;

    let ctx = android_context();
    let vm = unsafe { JavaVM::from_raw(ctx.vm().cast()) };

    vm.attach_current_thread(|env| {
        let context = unsafe { jni::objects::JObject::from_raw(env, ctx.context().cast()) };
        let files_dir = env.call_method(context, jni::jni_str!("getFilesDir"), jni::jni_sig!(() -> java.io.File), &[])?.l()?;
        let path = env.call_method(files_dir, jni::jni_str!("getAbsolutePath"), jni::jni_sig!(() -> java.lang.String), &[])?.l()?;
        let path_jstring = env.cast_local::<jni::objects::JString>(path)?;
        let path_string: String = env.get_string(&path_jstring)?.into();
        Ok(PathBuf::from(path_string))
    })
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
            let mut options = RuntimeOptions::embedded();

            // Use Android's app-specific data directory for database storage
            match get_android_files_dir() {
                Ok(files_dir) => {
                    options.database.storage = Some(files_dir);
                }
                Err(e) => {
                    eprintln!("Failed to get Android files directory: {}", e);
                    // Fallback to ephemeral database
                    options.database.storage = None;
                }
            }

            // Load state
            let database = DatabaseLayer::new(
                options.database.clone(),
                &sandpolis::MODELS,
                sandpolis_instance::database::WriteAuthority::Full,
            )
            .unwrap();

            // The app holds no realm certificate until the user logs in, so it
            // starts with just the default realm its own data lives in.
            let realms =
                sandpolis_instance::realm::Realms::for_client(Vec::new(), database.clone())
                    .unwrap();

            let state = InstanceState::new(
                &options,
                database,
                realms,
                sandpolis::ServerStratum::Global,
            )
            .await
            .unwrap();

            sandpolis::client::gui::main(options, state).await.unwrap();
        });
}
