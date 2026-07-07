use native_db::ToKey;
use native_model::Model;
use sandpolis_macros::data;

/// The set of Linux namespace inodes a process belongs to.
#[data(instance)]
pub struct NamespaceData {
    /// cgroup namespace inode
    pub cgroup_namespace: String,
    /// ipc namespace inode
    pub ipc_namespace: String,
    /// mnt namespace inode
    pub mnt_namespace: String,
    /// net namespace inode
    pub net_namespace: String,
    /// pid namespace inode
    pub pid_namespace: String,
    /// user namespace inode
    pub user_namespace: String,
    /// uts namespace inode
    pub uts_namespace: String,
}
