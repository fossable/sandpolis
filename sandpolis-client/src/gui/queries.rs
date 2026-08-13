use anyhow::Result;
use sandpolis_instance::InstanceId;
use sandpolis_instance::InstanceLayer;
use sandpolis_instance::network::NetworkLayer;

/// Instance metadata returned from database queries
#[derive(Clone, Debug)]
pub struct InstanceMetadata {
    pub instance_id: InstanceId,
    pub os_type: os_info::Type,
    pub hostname: Option<String>,
    pub is_server: bool,
}

/// Network edge between two instances
#[derive(Clone, Debug)]
pub struct NetworkEdge {
    pub from: InstanceId,
    pub to: InstanceId,
}

// ============================================================================
// Instance Queries
// ============================================================================

/// Query all instances from the database
/// This is the initial query run on startup to spawn all nodes
pub fn query_all_instances(
    instance_layer: &InstanceLayer,
    network_layer: &NetworkLayer,
) -> Result<Vec<InstanceId>> {
    let mut instance_ids = vec![instance_layer.instance_id];

    // Get all connections and extract unique remote instance IDs
    for connection in network_layer.connections.iter() {
        let conn = connection.read();
        if !instance_ids.contains(&conn.remote_instance) {
            instance_ids.push(conn.remote_instance);
        }
    }

    Ok(instance_ids)
}

/// Query metadata for a specific instance
pub fn query_instance_metadata(id: InstanceId) -> Result<InstanceMetadata> {
    // TODO: Query instance data from database
    // For now, return current system's OS info
    let os_info = os_info::get();
    Ok(InstanceMetadata {
        instance_id: id,
        os_type: os_info.os_type(),
        hostname: None, // TODO: Get hostname from database or gethostname crate
        is_server: id.is_server(),
    })
}

// ============================================================================
// Network Queries
// ============================================================================

/// Query network topology (edges between instances)
/// Returns list of connections for the current layer
pub fn query_network_topology(network_layer: &NetworkLayer) -> Result<Vec<NetworkEdge>> {
    let mut edges = Vec::new();

    // Get all connections and build edges
    for connection in network_layer.connections.iter() {
        let conn = connection.read();
        // Create edge from local instance to remote instance
        edges.push(NetworkEdge {
            from: conn._instance_id,
            to: conn.remote_instance,
        });
    }

    Ok(edges)
}

/// Network statistics for an instance
#[derive(Clone, Debug)]
pub struct NetworkStats {
    pub latency_ms: Option<u64>,
    pub throughput_bps: Option<u64>,
}

/// Query network stats for a specific instance
pub fn query_network_stats(_network_layer: &NetworkLayer, _id: InstanceId) -> Result<NetworkStats> {
    // TODO: Query from network resident
    Ok(NetworkStats {
        latency_ms: None,
        throughput_bps: None,
    })
}

// ============================================================================
// Filesystem Queries (Phase 3)
// ============================================================================

/// Filesystem usage statistics
#[derive(Clone, Debug)]
pub struct FilesystemUsage {
    pub total: u64,
    pub used: u64,
    pub free: u64,
}

/// Query filesystem usage for an instance
pub fn query_filesystem_usage(_id: InstanceId) -> Result<FilesystemUsage> {
    // TODO: Query from filesystem resident
    Ok(FilesystemUsage {
        total: 0,
        used: 0,
        free: 0,
    })
}

/// File/directory entry
#[derive(Clone, Debug)]
pub struct FileEntry {
    pub name: String,
    pub is_dir: bool,
    pub size: u64,
}

/// Query directory contents
pub fn query_directory_contents(
    _id: InstanceId,
    _path: &std::path::Path,
) -> Result<Vec<FileEntry>> {
    // TODO: Query from filesystem resident
    Ok(vec![])
}

// ============================================================================
// Inventory Queries (Phase 3)
// ============================================================================

/// Hardware information
#[derive(Clone, Debug)]
pub struct HardwareInfo {
    pub cpu_model: Option<String>,
    pub cpu_cores: Option<u32>,
    pub memory_total: Option<u64>,
}

/// Query hardware info for an instance
pub fn query_hardware_info(_id: InstanceId) -> Result<HardwareInfo> {
    // TODO: Query from inventory resident
    Ok(HardwareInfo {
        cpu_model: None,
        cpu_cores: None,
        memory_total: None,
    })
}

/// Memory statistics
#[derive(Clone, Debug)]
pub struct MemoryStats {
    pub total: u64,
    pub used: u64,
    pub free: u64,
}

/// Query memory stats for an instance
pub fn query_memory_stats(_id: InstanceId) -> Result<MemoryStats> {
    // TODO: Query from inventory resident
    Ok(MemoryStats {
        total: 0,
        used: 0,
        free: 0,
    })
}

// ============================================================================
// Shell Queries (Phase 4)
// ============================================================================

/// Shell session information
#[derive(Clone, Debug)]
pub struct ShellSession {
    pub session_id: String,
    pub active: bool,
}

/// Query shell sessions for an instance
pub fn query_shell_sessions(_id: InstanceId) -> Result<Vec<ShellSession>> {
    // TODO: Query from shell layer
    Ok(vec![])
}

/// Query output for a specific shell session
pub fn query_session_output(_session_id: &str) -> Result<String> {
    // TODO: Query from shell layer
    Ok(String::new())
}

// ============================================================================
// Package Queries (Phase 4)
// ============================================================================

/// Package information
#[derive(Clone, Debug)]
pub struct Package {
    pub name: String,
    pub version: String,
    pub installed: bool,
}

/// Query packages for an instance
pub fn query_packages(_id: InstanceId) -> Result<Vec<Package>> {
    // TODO: Query from package layer
    Ok(vec![])
}

// ============================================================================
// File Transfer Queries (Phase 3)
// ============================================================================

/// Active file transfer
#[derive(Clone, Debug)]
pub struct FileTransfer {
    pub from: InstanceId,
    pub to: InstanceId,
    pub filename: String,
    pub progress: f32, // 0.0 to 1.0
}

/// Query active file transfers
pub fn query_active_transfers() -> Result<Vec<FileTransfer>> {
    // TODO: Query from filesystem layer
    Ok(vec![])
}
