pub struct ProcessData {
    /// null
    pub name: String,
    /// null
    pub path: String,
    /// null
    pub command: String,
    /// null
    pub working_directory: String,
    /// null
    pub user: String,
    /// null
    pub user_id: String,
    /// null
    pub group: String,
    /// null
    pub group_id: String,
    /// null
    pub state: String,
    /// The process's process ID
    pub pid: u32,
    /// null
    pub parent_pid: u32,
    /// null
    pub thread_count: u32,
    /// null
    pub priority: u32,
    /// null
    pub virtual_size: u64,
    /// The resident memory size in bytes
    pub resident_set_size: u64,
    /// The number of milliseconds the process has executed in kernel mode
    pub kernel_time: u64,
    /// The number of milliseconds the process has executed in user mode
    pub user_time: u64,
    /// The epoch timestamp of the process start time
    pub start_time: u64,
    /// The number of bytes the process has read from disk
    pub bytes_read: u64,
    /// The number of bytes the process has written to disk
    pub bytes_written: u64,
    /// The number of file handles that the process owns
    pub handle_count: u64,
}
