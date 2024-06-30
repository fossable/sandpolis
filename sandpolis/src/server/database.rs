use crate::Banner;
use crate::ServerContext;
use crate::User;
use anyhow::{bail, Result};
use serde::{Deserialize, Serialize};
use std::sync::RwLock;

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
pub struct DbConnection {
    address: String,
    client: reqwest::Client,
}

impl DbConnection {
    pub async fn new(address: &str, username: &str, password: &str) -> Result<DbConnection> {
        let client = reqwest::Client::builder().cookie_store(true).build()?;

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
            bail!("Account error");
        }

        Ok(DbConnection {
            address: address.to_string(),
            client: client,
        })
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

    pub async fn add_user(&self, user: &User) -> Result<bool> {
        Ok(self
            .client
            .put(format!("{}/users", self.address))
            .send()
            .await?
            .status()
            .is_success())
    }

    /// Load the server context from the database.
    pub async fn load_context(self) -> Result<ServerContext> {
        Ok(ServerContext {
            db: self,
            iid: String::from(""),
            users: RwLock::new(vec![]),
            connections: RwLock::new(vec![]),
            groups: RwLock::new(vec![]),
            servers: RwLock::new(vec![]),
            banner: RwLock::new(Banner {}),
        })
    }

    pub async fn list_oid(&self, oid: &str) -> Result<()> {
        self
        .client
        .get(format!("{}/instances/_all_docs?startkey=%22/{}/%22&endkey=%22{}/%EF%BF%B0%22&include_docs=true", self.address, oid, oid))
        .send()
        .await?;

        Ok(())
    }
}
