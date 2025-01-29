pub struct GroupData {
    /// Unsigned int64 group ID
    pub gid: u64,
    /// Canonical local group name
    pub name: String,
    /// Unique group ID
    pub group_sid: Option<String>,
    /// Remarks or comments associated with the group
    pub comment: Option<String>,
}
