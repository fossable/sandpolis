use bevy::prelude::bevy_main;
use sandpolis::{InstanceState, RuntimeOptions};
use sandpolis_instance::database::DatabaseManager;
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
            let database = DatabaseManager::new(
                options.database.clone(),
                &sandpolis::MODELS,
                sandpolis_instance::database::WriteAuthority::Full,
            )
            .unwrap();

            // Realm certs imported through the GUI are saved into the files
            // dir, so a fresh install holds none and the realm-selection
            // dialog prompts for one; later starts pick up where it left off.
            // A cert that no longer loads is skipped rather than fatal — the
            // user has no shell here, and the dialog is the recovery path.
            let endpoint_certs = match &options.database.storage {
                Some(dir) => sandpolis_instance::realm::load_realm_certs_dir(dir)
                    .unwrap_or_else(|e| {
                        eprintln!("Ignoring saved realm certs: {}", e);
                        Vec::new()
                    }),
                None => Vec::new(),
            };

            let realms = sandpolis_instance::realm::RealmManager::for_endpoint(
                endpoint_certs.clone(),
                database.clone(),
            )
            .unwrap();

            let state = InstanceState::new(
                &options,
                database,
                realms,
                sandpolis::ServerStratum::Global,
            )
            .await
            .unwrap();

            for cert in &endpoint_certs {
                match cert.url() {
                    Ok(url) => sandpolis::client::spawn_server_connection(state.clone(), url),
                    Err(e) => eprintln!("Ignoring saved realm cert: {}", e),
                }
            }

            sandpolis::client::gui::main(options, state).await.unwrap();
        });
}
