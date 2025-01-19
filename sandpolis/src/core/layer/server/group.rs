use std::ops::Deref;

use regex::Regex;
use serde::{Deserialize, Serialize};
use validator::{Validate, ValidationErrors};

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

impl Deref for GroupName {
    type Target = String;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl From<String> for GroupName {
    fn from(value: String) -> Self {
        GroupName(value)
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
pub struct GroupCaCertificate {
    pub cert: String,
    pub key: String,
}

pub struct GroupServerCert {
    pub cert: String,
    pub key: String,
}

// TODO EndpointCertificate?
/// A _client_ certificate used to authenticate at a group level.
pub struct GroupClientCert {
    pub cert: String,
    pub key: String,
}
