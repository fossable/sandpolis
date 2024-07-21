use anyhow::{bail, Result};
use couch_rs::CouchDocument;
use couch_rs::{document::TypedCouchDocument, types::document::DocumentId};
use serde::{Deserialize, Serialize};
use std::{
    collections::HashMap,
    process::{Child, Command, Stdio},
    sync::{Arc, Mutex},
    thread::sleep,
    time::Duration,
};
use tracing::debug;

use super::InstanceId;

#[derive(Serialize, Deserialize, Clone, Debug, CouchDocument)]
pub struct LocalMetadata {
    #[serde(skip_serializing_if = "String::is_empty")]
    pub _id: DocumentId,
    #[serde(skip_serializing_if = "String::is_empty")]
    pub _rev: String,

    pub id: InstanceId,
    pub os_info: os_info::Info,
}

#[derive(Clone)]
pub struct Database {
    address: String,
    db_process: Option<Arc<Mutex<Child>>>,
    pub local: couch_rs::Client,
    remote: HashMap<InstanceId, couch_rs::Client>,
    pub metadata: LocalMetadata,
}

impl Drop for Database {
    fn drop(&mut self) {
        if let Some(process) = self.db_process.as_mut() {
            if let Some(process) = Arc::get_mut(process) {
                let mut process = process.lock().unwrap();

                debug!("Stopping database");
                process.kill().unwrap_or_default();
            }
        }
    }
}

impl Database {
    pub async fn new(address: Option<String>, username: &str, password: &str) -> Result<Self> {
        let (address, db_process) = if let Some(address) = address {
            (address, None)
        } else {
            // Allow external access to the database in debug mode
            let bind_address = if cfg!(debug_assertions) {
                "0.0.0.0"
            } else {
                "127.0.0.1"
            };

            // Replace couchdb configuration
            std::fs::write(
                "/opt/couchdb/etc/local.ini",
                format!(
                    r#"
                        [chttpd]
                        port = 5984
                        bind_address = {bind_address}
                        
                        [admins]
                        {username} = {password}
                    "#
                ),
            )?;

            // Spawn couchdb
            let db_process = Command::new("/opt/couchdb/bin/couchdb")
                .stdout(Stdio::null())
                .stderr(Stdio::null())
                .spawn()?;
            debug!("Spawned new database process");

            (
                "http://127.0.0.1:5984".to_string(),
                Some(Arc::new(Mutex::new(db_process))),
            )
        };

        let client = couch_rs::Client::new(&address, username, password)?;
        // Check connection
        let mut i = 0;
        let metadata = loop {
            i += 1;

            debug!(
                attempt = i,
                address = address,
                "Attempting connection to database"
            );
            match client.db("local").await {
                Ok(local) => {
                    break match local.get::<LocalMetadata>("metadata").await {
                        Ok(metadata) => metadata,
                        Err(e) => {
                            if e.is_not_found() {
                                let mut metadata = LocalMetadata {
                                    _id: "metadata".to_string(),
                                    _rev: "".to_string(),
                                    id: InstanceId::new(&vec![]),
                                    os_info: os_info::get(),
                                };
                                local.save(&mut metadata).await?;
                                metadata
                            } else {
                                bail!("Failed to get database metadata");
                            }
                        }
                    }
                }
                Err(_) => sleep(Duration::from_secs(i)),
            }

            if i >= 5 {
                bail!("The database failed to start");
            }
        };

        debug!(
            attempts = i,
            address = address,
            metadata = ?metadata,
            "Connection to the database was successful"
        );
        Ok(Self {
            address,
            db_process,
            metadata,
            local: client,
            remote: HashMap::new(),
        })
    }

    /// Setup a remote server for database replication.
    pub async fn add_server(
        &mut self,
        address: &str,
        username: &str,
        password: &str,
    ) -> Result<()> {
        debug!(address = address, "Attempting server connection");

        let client = couch_rs::Client::new(&address, username, password)?;

        let metadata = client
            .db("local")
            .await?
            .get::<LocalMetadata>("metadata")
            .await?;

        // Setup replications
        // TODO

        debug!(server_id = ?metadata.id, "Connected to server successfully");

        self.remote.insert(metadata.id, client);
        Ok(())
    }

    // pub async fn add_user(&self, user: &User) -> Result<bool> {
    //     Ok(self
    //         .client
    //         .put(format!("{}/users", self.address))
    //         .send()
    //         .await?
    //         .status()
    //         .is_success())
    // }

    // TODO add _changes

    // pub async fn list_oid(&self, oid: &str) -> Result<()> {
    //     self
    //     .local
    //     .get(format!("{}/instances/_all_docs?startkey=%22/{}/%22&endkey=%22{}/%EF%BF%B0%22&include_docs=true", self.address, oid, oid))
    //     .send()
    //     .await?;

    //     Ok(())
    // }
}
