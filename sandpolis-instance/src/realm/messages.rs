use super::RealmName;

// TODO these probably need to move into 'server' to avoid cyclic dependency on 'network'

/// Create a new realm with the given user as owner
pub struct CreateRealmRequest {
    pub owner: String,
    pub name: RealmName,
}

pub enum CreateRealmResponse {
    Ok,
}

/// Delete a realm by name.
pub struct DeleteRealmRequest {
    pub name: RealmName,
}

pub enum DeleteRealmResponse {
    Ok,
    NotFound,
}
