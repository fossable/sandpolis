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

#[derive(Serialize, Deserialize, CouchDocument)]
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
        if let Some(address) = address {
            let client = couch_rs::Client::new(&address, username, password)?;
            let rs = client
                .post(format!("{}/_session", address))
                .header("Content-Type", "application/json")
                .json(&PostSession {
                    username: username.to_string(),
                    password: password.to_string(),
                })
                .send()
                .await?
                .json::<RsPostSession>()
                .await?;

            if !rs.ok {
                bail!("Login error");
            }

            Ok(Self {
                address: address.to_string(),
                db_process: None,
                local: client,
                remote: HashMap::new(),
            })
        } else {
            let client = couch_rs::Client::new("http://127.0.0.1:5984", username, password)?;

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

            // Try until the database accepts our connection
            let mut i = 0;
            let rs = loop {
                i += 1;

                debug!(attempt = i, "Attempting connection to local database");
                match client
                    .post("http://127.0.0.1:5984/_session")
                    .header("Content-Type", "application/json")
                    .json(&PostSession {
                        username: username.to_string(),
                        password: password.to_string(),
                    })
                    .send()
                    .await
                {
                    Ok(response) => {
                        break response.json::<RsPostSession>().await?;
                    }
                    Err(_) => sleep(Duration::from_secs(1)),
                }

                if i >= 10 {
                    bail!("The database failed to start");
                }
            };

            if !rs.ok {
                bail!("Login error");
            }

            debug!(
                attempts = i,
                "Connection to the local database was successful"
            );
            Ok(Self {
                address: "http://127.0.0.1:5984".to_string(),
                db_process: Some(Arc::new(Mutex::new(db_process))),
                local: client,
                remote: HashMap::new(),
            })
        }
    }

    /// Setup a remote server for database replication.
    pub async fn add_server(&mut self, address: &str) -> Result<()> {
        let client = reqwest::Client::builder().cookie_store(true).build()?;

        let rs = client
            .post(format!("{}/_session", address))
            .header("Content-Type", "application/json")
            .json(&PostSession {
                username: "".to_string(),
                password: "".to_string(),
            })
            .send()
            .await?
            .json::<RsPostSession>()
            .await?;

        if !rs.ok {
            bail!("Login error");
        }

        // Setup replication
        client
            .post(format!("{}/_session", address))
            .header("Content-Type", "application/json")
            .json(&PostReplication {
                _id: todo!(),
                source: todo!(),
                create_target: todo!(),
                continuous: todo!(),
                target: todo!(),
            })
            .send()
            .await?;

        self.remote.push(client);
        Ok(())
    }

    pub async fn metadata(&self) -> Result<LocalMetadata> {
        match self
            .local
            .db("local")
            .await?
            .get::<LocalMetadata>("instance")
            .await
        {
            Ok(metadata) => Ok(metadata),
            Err(e) => if e.is_not_found() {},
        }
    }

    /// Create persistent databases if necessary and reset transient databases.
    pub async fn setup_db(&self) -> Result<()> {
        for persistent_db in vec!["users", "listeners", "groups", "instances", "plugins"] {
            if !self
                .local
                .get(format!("{}/{}", self.address, persistent_db))
                .send()
                .await?
                .status()
                .is_success()
            {
                if !self
                    .local
                    .put(format!("{}/{}", self.address, persistent_db))
                    .send()
                    .await?
                    .status()
                    .is_success()
                {
                    bail!("Failed to create database");
                }
            }
        }

        for transient_db in vec!["network", "connections", "streams"] {
            self.local
                .delete(format!("{}/{}", self.address, transient_db))
                .send()
                .await?
                .status()
                .is_success();

            if !self
                .local
                .put(format!("{}/{}", self.address, transient_db))
                .send()
                .await?
                .status()
                .is_success()
            {
                bail!("Failed to create database");
            }
        }

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

    pub async fn list_oid(&self, oid: &str) -> Result<()> {
        self
        .local
        .get(format!("{}/instances/_all_docs?startkey=%22/{}/%22&endkey=%22{}/%EF%BF%B0%22&include_docs=true", self.address, oid, oid))
        .send()
        .await?;

        Ok(())
    }
}
