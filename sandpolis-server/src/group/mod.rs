//! Group layer

use anyhow::Result;
use regex::Regex;
use serde::{Deserialize, Serialize};
use std::fmt::Display;
use std::str::FromStr;
use std::{ops::Deref, path::Path};
use validator::{Validate, ValidationErrors};
use x509_parser::prelude::FromDer;
use x509_parser::prelude::X509Certificate;

use super::user::UserName;

/// A group is a set of clients and agents that can interact. Each group has a
/// CA certificate that signs certificates used to connect to the server.
///
/// All servers have a default group.
#[derive(Serialize, Deserialize, Validate, Debug, Clone)]
pub struct GroupData {
    pub name: GroupName,
    pub owner: String,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct GroupName(String);

impl Display for GroupName {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(&self.0)
    }
}

impl Deref for GroupName {
    type Target = String;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl FromStr for GroupName {
    type Err = anyhow::Error;

    fn from_str(s: &str) -> Result<Self> {
        let name = GroupName(s.to_string());
        name.validate()?;
        Ok(name)
    }
}

impl Validate for GroupName {
    fn validate(&self) -> Result<(), ValidationErrors> {
        if Regex::new("^[a-z0-9]{4,32}$").unwrap().is_match(&self.0) {
            Ok(())
        } else {
            Err(ValidationErrors::new())
        }
    }
}

/// Create a new group with the given user as owner
pub struct CreateGroupRequest {
    pub owner: UserName,
    pub name: GroupName,
}

pub enum CreateGroupResponse {
    Ok,
}

/// Delete a group by name.
pub struct DeleteGroupRequest {
    pub name: String,
}

pub enum DeleteGroupResponse {
    Ok,
    NotFound,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct GroupCaCert {
    pub cert: String,
    pub key: String,
}

pub struct GroupServerCert {
    pub cert: String,
    pub key: String,
}

/// A _client_ certificate (not as in "client" instance) used to authenticate with
/// a server instance.
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct GroupClientCert {
    pub ca: String,
    pub cert: String,
    pub key: String,
}

impl GroupClientCert {
    /// Read the certificate from a file.
    pub fn read<P>(path: P) -> Result<Self>
    where
        P: AsRef<Path>,
    {
        Ok(serde_json::from_slice(&std::fs::read(path)?)?)
    }

    pub fn ca(&self) -> Result<reqwest::Certificate> {
        Ok(reqwest::Certificate::from_pem(self.ca.as_bytes())?)
    }

    pub fn identity(&self) -> Result<reqwest::Identity> {
        // Combine cert and key together
        let mut bundle = Vec::new();
        bundle.extend_from_slice(&self.cert.as_bytes());
        bundle.extend_from_slice(&self.key.as_bytes());
        Ok(reqwest::Identity::from_pem(&bundle)?)
    }

    /// Return when the certificate was generated.
    pub fn creation_time(&self) -> Result<i64> {
        let pem = pem::parse(&self.cert.as_bytes())?;
        Ok(X509Certificate::from_der(pem.contents())?
            .1
            .validity
            .not_before
            .timestamp())
    }

    pub fn name(&self) -> Result<GroupName> {
        let pem = pem::parse(&self.cert.as_bytes())?;
        let name = X509Certificate::from_der(pem.contents())?
            .1
            .subject()
            .iter_common_name()
            .next()
            .ok_or_else(|| anyhow::anyhow!("no common name"))?
            .to_owned()
            .as_str()
            .map_err(|_| anyhow::anyhow!("invalid common name"))?
            .parse()?;

        Ok(name)
    }
}
