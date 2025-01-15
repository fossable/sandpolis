use serde::{Deserialize, Serialize};
use uuid::Uuid;
use validator::Validate;

/// A group is a set of clients and agents that can interact. Each group has a
/// CA certificate that signs certificates used to connect to the server.
///
/// All servers have a default group.
#[derive(Serialize, Deserialize, Validate, Debug, Clone)]
pub struct GroupData {
    #[validate(length(min = 4, max = 20))]
    pub name: String,
    pub owner: String,
    pub members: Vec<String>,
}

/// Create a new group with the given user as owner
pub struct CreateGroupRequest {
    pub owner: String,
    /// Group name
    pub name: String,
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

/// A _client_ certificate used to authenticate at a group level.
pub struct GroupCertificate {
    pub cert: String,
    pub key: String,
}
