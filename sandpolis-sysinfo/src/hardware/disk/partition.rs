pub struct PartitionData {
    /// null
    pub identification: String,
    /// null
    pub name: String,
    /// null
    pub description: String,
    /// The partition's UUID
    pub uuid: String,
    /// The partition's total size in bytes
    pub size: u64,
    /// null
    pub major: u32,
    /// null
    pub minor: u32,
    /// The partition's mount point
    pub mount: String,
}
