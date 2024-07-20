use std::{
    process::{Child, Command, Stdio},
    sync::{Arc, Mutex},
    thread::sleep,
    time::Duration,
};

use anyhow::{bail, Result};
use serde::{Deserialize, Serialize};
use tracing::debug;

#[derive(Serialize)]
struct PostSession {
    username: String,
    password: String,
}

#[derive(Deserialize)]
struct RsPostSession {
    ok: bool,
    name: String,
}

#[derive(Serialize)]
enum ReplicationAuth {
    #[serde(rename = "basic")]
    Basic { username: String, password: String },
}

#[derive(Serialize)]
struct ReplicationTarget {
    url: String,
    auth: ReplicationAuth,
}

#[derive(Serialize)]
struct PostReplication {
    _id: String,
    source: String,
    create_target: bool,
    continuous: bool,
    target: ReplicationTarget,
}

#[derive(Clone)]
pub struct Database {
    address: String,
    db_process: Option<Arc<Mutex<Child>>>,
    pub local: reqwest::Client,
    remote: Vec<reqwest::Client>,
}

impl Database {
    pub async fn new(address: Option<String>, username: &str, password: &str) -> Result<Self> {
        let client = reqwest::Client::builder().cookie_store(true).build()?;

        if let Some(address) = address {
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
                remote: Vec::new(),
            })
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
                remote: Vec::new(),
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

    pub async fn list_oid(&self, oid: &str) -> Result<()> {
        self
        .local
        .get(format!("{}/instances/_all_docs?startkey=%22/{}/%22&endkey=%22{}/%EF%BF%B0%22&include_docs=true", self.address, oid, oid))
        .send()
        .await?;

        Ok(())
    }
}
