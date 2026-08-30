//! Small helpers for reading block device topology out of `/sys` and `/dev`,
//! shared by the partition and block device collectors. Everything degrades to
//! "no data" on platforms without sysfs.

use std::collections::HashMap;
use std::path::Path;

/// Read a sysfs attribute file, trimming the trailing newline.
pub(crate) fn read_trimmed<P: AsRef<Path>>(path: P) -> Option<String> {
    std::fs::read_to_string(path)
        .ok()
        .map(|s| s.trim().to_string())
}

/// Read a sysfs attribute file as an integer.
pub(crate) fn read_u64<P: AsRef<Path>>(path: P) -> Option<u64> {
    read_trimmed(path)?.parse().ok()
}

/// Map device names (e.g. `sda1`) to partition UUIDs by resolving the udev
/// symlinks under `/dev/disk/by-partuuid`. Empty when udev hasn't populated
/// the directory (containers, non-Linux).
pub(crate) fn partuuid_map() -> HashMap<String, String> {
    let mut map = HashMap::new();
    let Ok(entries) = std::fs::read_dir("/dev/disk/by-partuuid") else {
        return map;
    };
    for entry in entries.flatten() {
        let uuid = entry.file_name().to_string_lossy().to_string();
        if let Ok(target) = std::fs::canonicalize(entry.path())
            && let Some(name) = target.file_name()
        {
            map.insert(name.to_string_lossy().to_string(), uuid);
        }
    }
    map
}

/// Map device paths (e.g. `/dev/sda1`) to their first mountpoint according to
/// `/proc/mounts`.
pub(crate) fn mount_map() -> HashMap<String, String> {
    let mut map = HashMap::new();
    let Ok(mounts) = std::fs::read_to_string("/proc/mounts") else {
        return map;
    };
    for line in mounts.lines() {
        let mut fields = line.split_whitespace();
        if let (Some(device), Some(mount)) = (fields.next(), fields.next())
            && device.starts_with("/dev/")
        {
            // Mount paths escape spaces as \040; undo the common ones.
            let mount = mount.replace("\\040", " ").replace("\\011", "\t");
            map.entry(device.to_string()).or_insert(mount);
        }
    }
    map
}
