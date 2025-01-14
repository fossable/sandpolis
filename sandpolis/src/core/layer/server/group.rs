use uuid::Uuid;
use validator::Validate;

pub type GroupId = Uuid;

/// A group is a set of clients and agents that can interact. Each group has a
/// CA certificate that signs certificates used to connect to the server.
///
/// All servers have a default group.
#[derive(Validate)]
pub struct GroupData {
    pub id: GroupId,
    #[validate(length(min = 4, max = 20))]
    pub name: String,
    pub members: Vec<String>,
    pub ca: String,
    pub key: String,
}

/// Create a new group with the given user as owner
pub struct CreateGroupRequest {
    pub owner: String,
    /// Group name
    pub name: String,
}

pub enum CreateGroupResponse {
    Ok(GroupId),
}

pub struct DeleteGroupRequest {
    pub id: GroupId,
}

pub enum DeleteGroupResponse {
    Ok,
}
