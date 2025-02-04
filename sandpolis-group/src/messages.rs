use super::GroupName;

/// Create a new group with the given user as owner
pub struct CreateGroupRequest {
    pub owner: String,
    pub name: GroupName,
}

pub enum CreateGroupResponse {
    Ok,
}

/// Delete a group by name.
pub struct DeleteGroupRequest {
    pub name: GroupName,
}

pub enum DeleteGroupResponse {
    Ok,
    NotFound,
}
