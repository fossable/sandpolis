//! Integration tests for the NFS backend behind the probe filesystem interface.
//!
//! These run against a real NFSv3 server ([`nfs3_server`]) held in-process, which
//! serves portmap, mount, and NFS on one port. That exercises the actual wire
//! protocol — handle resolution, READDIRPLUS cookies, XDR — rather than mocking
//! it out.

use nfs3_server::memfs::{MemFs, MemFsConfig};
use nfs3_server::tcp::{NFSTcp, NFSTcpListener};
use sandpolis_probe::config::NfsProbeConfig;
use sandpolis_probe::filesystem::FileKind;
use sandpolis_probe::nfs::{NfsFs, enumerate};
use std::net::{IpAddr, Ipv4Addr};
use std::path::Path;

const LOCALHOST: IpAddr = IpAddr::V4(Ipv4Addr::LOCALHOST);

/// Start a server on an ephemeral port with a small tree, returning its port.
///
/// The listener is leaked rather than joined: it stops when the process does,
/// and a test that finished doesn't care.
async fn serve() -> u16 {
    let mut config = MemFsConfig::default();
    config.add_dir("/docs");
    config.add_file("/docs/readme.txt", b"hello probe".to_vec());
    config.add_file("/docs/empty.txt", b"".to_vec());
    config.add_dir("/docs/nested");
    config.add_file("/docs/nested/deep.txt", b"deep".to_vec());
    config.add_file("/top.txt", b"top level".to_vec());

    let fs = MemFs::new(config).expect("failed to build the in-memory filesystem");
    let listener = NFSTcpListener::bind("127.0.0.1:0", fs)
        .await
        .expect("failed to bind the NFS server");
    let port = listener.get_listen_port();

    tokio::spawn(async move {
        let _ = listener.handle_forever().await;
    });

    port
}

/// A config pointing at a server on `port`. Mount and NFS ports are pinned to it
/// because this server multiplexes all three programs onto one socket.
fn config(port: u16) -> NfsProbeConfig {
    NfsProbeConfig {
        export: "/".into(),
        portmapper_port: Some(port),
        mount_port: Some(port),
        nfs_port: Some(port),
        uid: Some(0),
        gid: Some(0),
        // A test can't bind a privileged source port, and doesn't need to.
        privileged_port: Some(false),
    }
}

async fn mount(port: u16) -> NfsFs {
    NfsFs::mount(LOCALHOST, &config(port))
        .await
        .expect("failed to mount the export")
}

/// Entry names in a directory, sorted so assertions don't depend on server order.
async fn names(fs: &NfsFs, path: &str) -> Vec<String> {
    let mut names: Vec<String> = fs
        .list(Path::new(path))
        .await
        .expect("failed to list")
        .into_iter()
        .map(|entry| entry.name)
        .collect();
    names.sort();
    names
}

#[tokio::test]
async fn lists_directories() {
    let fs = mount(serve().await).await;

    assert_eq!(names(&fs, "/").await, vec!["docs", "top.txt"]);
    assert_eq!(
        names(&fs, "/docs").await,
        vec!["empty.txt", "nested", "readme.txt"]
    );
    // Nested paths resolve by walking LOOKUP from the export root.
    assert_eq!(names(&fs, "/docs/nested").await, vec!["deep.txt"]);

    // "." and ".." are filtered out of listings.
    assert!(!names(&fs, "/docs").await.iter().any(|n| n == "." || n == ".."));
}

#[tokio::test]
async fn reports_entry_kinds_and_sizes() {
    let fs = mount(serve().await).await;

    let entries = fs.list(Path::new("/docs")).await.unwrap();
    let readme = entries.iter().find(|e| e.name == "readme.txt").unwrap();
    assert_eq!(readme.kind, FileKind::File);
    assert_eq!(readme.size, "hello probe".len() as u64);
    assert!(readme.modified.is_some());
    assert!(readme.mode.is_some());

    let nested = entries.iter().find(|e| e.name == "nested").unwrap();
    assert_eq!(nested.kind, FileKind::Dir);
}

#[tokio::test]
async fn stats_files_and_directories() {
    let fs = mount(serve().await).await;

    let file = fs.stat(Path::new("/docs/readme.txt")).await.unwrap();
    assert_eq!(file.kind, FileKind::File);
    assert_eq!(file.size, "hello probe".len() as u64);

    let dir = fs.stat(Path::new("/docs")).await.unwrap();
    assert_eq!(dir.kind, FileKind::Dir);

    // The export root itself stats without a LOOKUP.
    assert_eq!(fs.stat(Path::new("/")).await.unwrap().kind, FileKind::Dir);
}

#[tokio::test]
async fn reads_file_contents() {
    let fs = mount(serve().await).await;

    let data = fs.read(Path::new("/docs/readme.txt"), 0, 1024).await.unwrap();
    assert_eq!(data, b"hello probe");

    // Reading at an offset returns the tail.
    let tail = fs.read(Path::new("/docs/readme.txt"), 6, 1024).await.unwrap();
    assert_eq!(tail, b"probe");

    // An empty file reads as no bytes rather than failing.
    assert!(
        fs.read(Path::new("/docs/empty.txt"), 0, 1024)
            .await
            .unwrap()
            .is_empty()
    );
}

#[tokio::test]
async fn writes_file_contents() {
    let fs = mount(serve().await).await;

    let written = fs
        .write(Path::new("/docs/empty.txt"), 0, b"written")
        .await
        .unwrap();
    assert_eq!(written, b"written".len() as u32);

    let data = fs.read(Path::new("/docs/empty.txt"), 0, 1024).await.unwrap();
    assert_eq!(data, b"written");
}

#[tokio::test]
async fn creates_and_removes_directories() {
    let fs = mount(serve().await).await;

    fs.create_dir(Path::new("/docs/fresh")).await.unwrap();
    assert!(names(&fs, "/docs").await.contains(&"fresh".to_string()));
    assert_eq!(
        fs.stat(Path::new("/docs/fresh")).await.unwrap().kind,
        FileKind::Dir
    );

    fs.remove(Path::new("/docs/fresh"), false).await.unwrap();
    assert!(!names(&fs, "/docs").await.contains(&"fresh".to_string()));
}

#[tokio::test]
async fn removes_a_tree_recursively() {
    let fs = mount(serve().await).await;

    // A non-recursive remove of a populated directory must fail rather than
    // silently doing nothing.
    assert!(fs.remove(Path::new("/docs/nested"), false).await.is_err());

    fs.remove(Path::new("/docs/nested"), true).await.unwrap();
    assert_eq!(names(&fs, "/docs").await, vec!["empty.txt", "readme.txt"]);
}

#[tokio::test]
async fn removes_files() {
    let fs = mount(serve().await).await;

    fs.remove(Path::new("/docs/readme.txt"), false).await.unwrap();
    assert!(!names(&fs, "/docs").await.contains(&"readme.txt".to_string()));
}

#[tokio::test]
async fn renames_within_and_across_directories() {
    let fs = mount(serve().await).await;

    fs.rename(Path::new("/docs/readme.txt"), Path::new("/docs/renamed.txt"))
        .await
        .unwrap();
    let listing = names(&fs, "/docs").await;
    assert!(listing.contains(&"renamed.txt".to_string()));
    assert!(!listing.contains(&"readme.txt".to_string()));

    // Across directories, and the cached handle for the old path must not
    // survive the move.
    fs.rename(Path::new("/docs/renamed.txt"), Path::new("/moved.txt"))
        .await
        .unwrap();
    assert!(names(&fs, "/").await.contains(&"moved.txt".to_string()));
    assert_eq!(
        fs.read(Path::new("/moved.txt"), 0, 1024).await.unwrap(),
        b"hello probe"
    );
}

#[tokio::test]
async fn reports_filesystem_usage() {
    let fs = mount(serve().await).await;

    let usage = fs.statfs().await.unwrap();
    assert!(usage.total > 0, "expected a non-zero total");
    assert!(usage.used <= usage.total);
}

#[tokio::test]
async fn enumerates_exports_without_mounting() {
    let port = serve().await;

    let shares = enumerate(LOCALHOST, Some(&config(port))).await.unwrap();
    assert!(!shares.is_empty(), "expected at least one export");
    assert!(shares.iter().all(|share| share.kind == "export"));
}

#[tokio::test]
async fn enumerates_through_the_portmapper() {
    let port = serve().await;

    // Without a pinned mount port the mount service is resolved via GETPORT,
    // which is the path a real deployment takes.
    let config = NfsProbeConfig {
        mount_port: None,
        ..config(port)
    };
    assert!(!enumerate(LOCALHOST, Some(&config)).await.unwrap().is_empty());
}

#[tokio::test]
async fn rejects_paths_escaping_the_export() {
    let fs = mount(serve().await).await;

    // Normalization must refuse to climb out of the export, whatever the caller
    // sends over the stream.
    assert!(fs.list(Path::new("/../etc")).await.is_err());
    assert!(fs.read(Path::new("/docs/../../etc/passwd"), 0, 16).await.is_err());
}

#[tokio::test]
async fn reports_missing_paths_as_errors() {
    let fs = mount(serve().await).await;

    assert!(fs.stat(Path::new("/nope.txt")).await.is_err());
    assert!(fs.list(Path::new("/docs/nope")).await.is_err());
}

#[tokio::test]
async fn fails_to_mount_a_missing_export() {
    let port = serve().await;

    let config = NfsProbeConfig {
        export: "/not-exported".into(),
        ..config(port)
    };
    assert!(NfsFs::mount(LOCALHOST, &config).await.is_err());
}
