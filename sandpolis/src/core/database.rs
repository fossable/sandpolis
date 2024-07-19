use std::{
    process::{Child, Command},
    sync::{Arc, Mutex},
};

use anyhow::{bail, Result};
use serde::{Deserialize, Serialize};

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

#[derive(Clone)]
pub struct Database {
    address: String,
    db_process: Option<Arc<Mutex<Child>>>,
    client: reqwest::Client,
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
                client,
            })
        } else {
            // Replace couchdb configuration
            std::fs::write(
                "/opt/couchdb/etc/local.ini",
                format!(
                    r#"
                        [chttpd]
                        port = 5984
                        bind_address = 127.0.0.1
                        
                        [admins]
                        {username} = {password}
                    "#
                ),
            )?;

            // Spawn couchdb
            let db_process = Command::new("/opt/couchdb/bin/couchdb").spawn()?;

            let rs = client
                .post("http://127.0.0.1:5984/_session")
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
                address: "http://127.0.0.1:5984".to_string(),
                db_process: Some(Arc::new(Mutex::new(db_process))),
                client,
            })
        }
    }

    /// Create persistent databases if necessary and reset transient databases.
    pub async fn setup_db(&self) -> Result<()> {
        for persistent_db in vec!["users", "listeners", "groups", "instances", "plugins"] {
            if !self
                .client
                .get(format!("{}/{}", self.address, persistent_db))
                .send()
                .await?
                .status()
                .is_success()
            {
                if !self
                    .client
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
            self.client
                .delete(format!("{}/{}", self.address, transient_db))
                .send()
                .await?
                .status()
                .is_success();

            if !self
                .client
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
        .client
        .get(format!("{}/instances/_all_docs?startkey=%22/{}/%22&endkey=%22{}/%EF%BF%B0%22&include_docs=true", self.address, oid, oid))
        .send()
        .await?;

        Ok(())
    }
}
