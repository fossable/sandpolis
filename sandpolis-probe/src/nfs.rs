//! NFSv3 backend for the probe filesystem interface.
//!
//! Everything NFS-specific lives here: the portmapper handshake, the MOUNT
//! protocol, file handle resolution, and the NFSv3 procedures. Callers go through
//! [`crate::filesystem`] and never see any of it.

/// Default portmapper port.
pub const PORTMAPPER_PORT: u16 = 111;

/// How much of a directory to ask for per READDIRPLUS call. `dircount` bounds the
/// name bytes, `maxcount` the whole reply; servers cap both, so these are just an
/// upper bound that keeps large directories to a few round trips.
#[cfg(feature = "server")]
const READDIR_DIRCOUNT: u32 = 8192;
#[cfg(feature = "server")]
const READDIR_MAXCOUNT: u32 = 32768;

#[cfg(feature = "server")]
mod server {
    use super::*;
    use crate::config::NfsProbeConfig;
    use crate::filesystem::{DirEntry, FileAttr, FileKind, FsUsage, ShareEntry, normalize};
    use anyhow::{Context, Result, anyhow, bail};
    use nfs3_client::nfs3_types::nfs3::{
        self, GETATTR3args, LOOKUP3args, MKDIR3args, READ3args, READDIRPLUS3args, REMOVE3args,
        RENAME3args, RMDIR3args, WRITE3args, diropargs3, fattr3, filename3, ftype3, nfs_fh3,
        sattr3, stable_how,
    };
    use nfs3_client::nfs3_types::rpc::{auth_unix, opaque_auth};
    use nfs3_client::nfs3_types::xdr_codec::Opaque;
    use nfs3_client::net::Connector;
    use nfs3_client::tokio::{TokioConnector, TokioIo};
    use nfs3_client::{MountClient, Nfs3Connection, Nfs3ConnectionBuilder, PortmapperClient};
    use std::collections::HashMap;
    use std::net::{IpAddr, SocketAddr};
    use std::path::Path;
    use tokio::net::TcpStream;
    use tokio::sync::Mutex;

    type Io = TokioIo<TcpStream>;

    /// A mounted NFSv3 export.
    pub struct NfsFs {
        /// The NFS client is `&mut`-only and a mount is a single TCP connection,
        /// so operations are serialized through this.
        inner: Mutex<Inner>,
    }

    struct Inner {
        conn: Nfs3Connection<Io>,
        root: nfs_fh3,
        /// Resolved file handles, keyed by normalized path components. Saves a
        /// LOOKUP chain per operation on a directory the caller is walking.
        handles: HashMap<Vec<String>, nfs_fh3>,
    }

    /// Build the AUTH_UNIX credential the export will be accessed under. NFSv3
    /// has no real authentication — the uid/gid are simply asserted — so this is
    /// about matching the export's `squash` settings, not about secrecy.
    fn credential(config: &NfsProbeConfig) -> opaque_auth<'static> {
        opaque_auth::auth_unix(&auth_unix {
            stamp: 0,
            machinename: Opaque::borrowed(b"sandpolis"),
            uid: config.uid.unwrap_or(0),
            gid: config.gid.unwrap_or(0),
            gids: vec![],
        })
    }

    impl NfsFs {
        /// Mount `config.export` on `ip`.
        pub async fn mount(ip: IpAddr, config: &NfsProbeConfig) -> Result<Self> {
            let credential = credential(config);
            let mut builder =
                Nfs3ConnectionBuilder::new(TokioConnector, ip.to_string(), &config.export)
                    .portmapper_port(config.portmapper_port.unwrap_or(PORTMAPPER_PORT))
                    // Linux nfsd rejects unprivileged source ports unless the
                    // export is marked `insecure`.
                    .connect_from_privileged_port(config.privileged_port.unwrap_or(true))
                    .credential(credential.clone())
                    .verifier(opaque_auth::default());
            if let Some(port) = config.mount_port {
                builder = builder.mount_port(port);
            }
            if let Some(port) = config.nfs_port {
                builder = builder.nfs3_port(port);
            }

            let conn = builder
                .mount()
                .await
                .with_context(|| format!("failed to mount {} on {ip}", config.export))?;
            let root = conn.root_nfs_fh3();

            Ok(Self {
                inner: Mutex::new(Inner {
                    conn,
                    root,
                    handles: HashMap::new(),
                }),
            })
        }

        pub async fn list(&self, path: &Path) -> Result<Vec<DirEntry>> {
            let components = normalize(path)?;
            let mut inner = self.inner.lock().await;
            let dir = inner.resolve(&components).await?;
            inner.readdirplus(dir).await
        }

        pub async fn stat(&self, path: &Path) -> Result<FileAttr> {
            let components = normalize(path)?;
            let mut inner = self.inner.lock().await;
            let fh = inner.resolve(&components).await?;
            let attr = inner.getattr(fh).await?;
            Ok(file_attr(&attr))
        }

        pub async fn read(&self, path: &Path, offset: u64, len: u32) -> Result<Vec<u8>> {
            let components = normalize(path)?;
            let mut inner = self.inner.lock().await;
            let fh = inner.resolve(&components).await?;
            let result = inner
                .conn
                .read(&READ3args {
                    file: fh,
                    offset,
                    count: len,
                })
                .await?;
            match result {
                nfs3::Nfs3Result::Ok(ok) => Ok(ok.data.as_ref().to_vec()),
                nfs3::Nfs3Result::Err((status, _)) => bail!("read failed: {status:?}"),
            }
        }

        pub async fn write(&self, path: &Path, offset: u64, data: &[u8]) -> Result<u32> {
            let components = normalize(path)?;
            let mut inner = self.inner.lock().await;
            let fh = inner.resolve(&components).await?;
            let count = u32::try_from(data.len()).context("write is larger than one NFS call")?;
            let result = inner
                .conn
                .write(&WRITE3args {
                    file: fh,
                    offset,
                    count,
                    // Committed to stable storage before the server replies, so a
                    // successful write needs no follow-up COMMIT.
                    stable: stable_how::FILE_SYNC,
                    data: Opaque::borrowed(data),
                })
                .await?;
            match result {
                nfs3::Nfs3Result::Ok(ok) => Ok(ok.count),
                nfs3::Nfs3Result::Err((status, _)) => bail!("write failed: {status:?}"),
            }
        }

        pub async fn create_dir(&self, path: &Path) -> Result<()> {
            let components = normalize(path)?;
            let (parent, name) = split(&components)?;
            let mut inner = self.inner.lock().await;
            let dir = inner.resolve(parent).await?;
            let result = inner
                .conn
                .mkdir(&MKDIR3args {
                    where_: diropargs3 {
                        dir,
                        name: filename(name),
                    },
                    attributes: sattr3::default(),
                })
                .await?;
            match result {
                nfs3::Nfs3Result::Ok(_) => Ok(()),
                nfs3::Nfs3Result::Err((status, _)) => bail!("mkdir failed: {status:?}"),
            }
        }

        pub async fn remove(&self, path: &Path, recursive: bool) -> Result<()> {
            let components = normalize(path)?;
            let (parent, name) = split(&components)?;
            let mut inner = self.inner.lock().await;

            let parent_fh = inner.resolve(parent).await?;
            let target = inner.lookup(parent_fh.clone(), name).await?;
            let attr = inner.getattr(target).await?;

            if matches!(attr.type_, ftype3::NF3DIR) {
                if recursive {
                    // NFS has no recursive delete; walk the tree depth-first.
                    Box::pin(inner.remove_tree(&components)).await?;
                    return Ok(());
                }
                let result = inner
                    .conn
                    .rmdir(&RMDIR3args {
                        object: diropargs3 {
                            dir: parent_fh,
                            name: filename(name),
                        },
                    })
                    .await?;
                inner.forget(&components);
                return match result {
                    nfs3::Nfs3Result::Ok(_) => Ok(()),
                    nfs3::Nfs3Result::Err((status, _)) => bail!("rmdir failed: {status:?}"),
                };
            }

            let result = inner
                .conn
                .remove(&REMOVE3args {
                    object: diropargs3 {
                        dir: parent_fh,
                        name: filename(name),
                    },
                })
                .await?;
            inner.forget(&components);
            match result {
                nfs3::Nfs3Result::Ok(_) => Ok(()),
                nfs3::Nfs3Result::Err((status, _)) => bail!("remove failed: {status:?}"),
            }
        }

        pub async fn rename(&self, from: &Path, to: &Path) -> Result<()> {
            let from_components = normalize(from)?;
            let to_components = normalize(to)?;
            let (from_parent, from_name) = split(&from_components)?;
            let (to_parent, to_name) = split(&to_components)?;

            let mut inner = self.inner.lock().await;
            let from_dir = inner.resolve(from_parent).await?;
            let to_dir = inner.resolve(to_parent).await?;
            let result = inner
                .conn
                .rename(&RENAME3args {
                    from: diropargs3 {
                        dir: from_dir,
                        name: filename(from_name),
                    },
                    to: diropargs3 {
                        dir: to_dir,
                        name: filename(to_name),
                    },
                })
                .await?;
            inner.forget(&from_components);
            inner.forget(&to_components);
            match result {
                nfs3::Nfs3Result::Ok(_) => Ok(()),
                nfs3::Nfs3Result::Err((status, _)) => bail!("rename failed: {status:?}"),
            }
        }

        pub async fn statfs(&self) -> Result<FsUsage> {
            let mut inner = self.inner.lock().await;
            let root = inner.root.clone();
            let result = inner.conn.fsstat(&nfs3::FSSTAT3args { fsroot: root }).await?;
            match result {
                nfs3::Nfs3Result::Ok(ok) => Ok(FsUsage {
                    total: ok.tbytes,
                    // `fbytes` is free overall, `abytes` free to this user; report
                    // the total minus what's free so "used" matches `df`.
                    used: ok.tbytes.saturating_sub(ok.fbytes),
                    free: ok.abytes,
                }),
                nfs3::Nfs3Result::Err((status, _)) => bail!("fsstat failed: {status:?}"),
            }
        }
    }

    impl Inner {
        /// Resolve normalized components to a file handle, walking LOOKUP from the
        /// export root and caching what it finds.
        async fn resolve(&mut self, components: &[String]) -> Result<nfs_fh3> {
            if components.is_empty() {
                return Ok(self.root.clone());
            }
            if let Some(fh) = self.handles.get(components) {
                return Ok(fh.clone());
            }

            let mut fh = self.root.clone();
            for (index, component) in components.iter().enumerate() {
                let prefix = &components[..=index];
                if let Some(cached) = self.handles.get(prefix) {
                    fh = cached.clone();
                    continue;
                }
                fh = self.lookup(fh, component).await?;
                self.handles.insert(prefix.to_vec(), fh.clone());
            }
            Ok(fh)
        }

        /// Drop cached handles for a path and everything under it, after the
        /// entry it names has been removed or renamed.
        fn forget(&mut self, components: &[String]) {
            self.handles
                .retain(|key, _| !key.starts_with(components));
        }

        async fn lookup(&mut self, dir: nfs_fh3, name: &str) -> Result<nfs_fh3> {
            let result = self
                .conn
                .lookup(&LOOKUP3args {
                    what: diropargs3 {
                        dir,
                        name: filename(name),
                    },
                })
                .await?;
            match result {
                nfs3::Nfs3Result::Ok(ok) => Ok(ok.object),
                nfs3::Nfs3Result::Err((status, _)) => {
                    Err(anyhow!("lookup of {name:?} failed: {status:?}"))
                }
            }
        }

        async fn getattr(&mut self, fh: nfs_fh3) -> Result<fattr3> {
            let result = self.conn.getattr(&GETATTR3args { object: fh }).await?;
            match result {
                nfs3::Nfs3Result::Ok(ok) => Ok(ok.obj_attributes),
                nfs3::Nfs3Result::Err((status, _)) => bail!("getattr failed: {status:?}"),
            }
        }

        /// Read a whole directory, following the cookie until the server reports
        /// EOF.
        async fn readdirplus(&mut self, dir: nfs_fh3) -> Result<Vec<DirEntry>> {
            let mut out = Vec::new();
            let mut cookie = 0u64;
            let mut cookieverf = nfs3::cookieverf3::default();

            loop {
                let result = self
                    .conn
                    .readdirplus(&READDIRPLUS3args {
                        dir: dir.clone(),
                        cookie,
                        cookieverf,
                        dircount: READDIR_DIRCOUNT,
                        maxcount: READDIR_MAXCOUNT,
                    })
                    .await?;

                let ok = match result {
                    nfs3::Nfs3Result::Ok(ok) => ok,
                    nfs3::Nfs3Result::Err((status, _)) => {
                        bail!("readdirplus failed: {status:?}")
                    }
                };

                cookieverf = ok.cookieverf;
                let eof = ok.reply.eof;
                let entries = ok.reply.entries.into_inner();
                if entries.is_empty() {
                    break;
                }

                for entry in entries {
                    cookie = entry.cookie;
                    let name = String::from_utf8_lossy(entry.name.0.as_ref()).into_owned();
                    if name == "." || name == ".." {
                        continue;
                    }
                    let attr = match entry.name_attributes {
                        nfs3::Nfs3Option::Some(attr) => Some(attr),
                        nfs3::Nfs3Option::None => None,
                    };
                    out.push(dir_entry(name, attr.as_ref()));
                }

                if eof {
                    break;
                }
            }

            Ok(out)
        }

        /// Depth-first delete of the subtree at `components`.
        async fn remove_tree(&mut self, components: &[String]) -> Result<()> {
            let dir = self.resolve(components).await?;
            for entry in self.readdirplus(dir.clone()).await? {
                let mut child = components.to_vec();
                child.push(entry.name.clone());
                if entry.kind == FileKind::Dir {
                    Box::pin(self.remove_tree(&child)).await?;
                } else {
                    let result = self
                        .conn
                        .remove(&REMOVE3args {
                            object: diropargs3 {
                                dir: dir.clone(),
                                name: filename(&entry.name),
                            },
                        })
                        .await?;
                    if let nfs3::Nfs3Result::Err((status, _)) = result {
                        bail!("remove of {:?} failed: {status:?}", entry.name);
                    }
                    self.forget(&child);
                }
            }

            let (parent, name) = split(components)?;
            let parent_fh = self.resolve(parent).await?;
            let result = self
                .conn
                .rmdir(&RMDIR3args {
                    object: diropargs3 {
                        dir: parent_fh,
                        name: filename(name),
                    },
                })
                .await?;
            self.forget(components);
            match result {
                nfs3::Nfs3Result::Ok(_) => Ok(()),
                nfs3::Nfs3Result::Err((status, _)) => bail!("rmdir failed: {status:?}"),
            }
        }
    }

    /// List a server's exports over the MOUNT protocol.
    ///
    /// Doesn't mount anything, so it answers even when the configured export is
    /// wrong or absent — which is the point: it's how you find out what to
    /// configure.
    pub async fn enumerate(ip: IpAddr, config: Option<&NfsProbeConfig>) -> Result<Vec<ShareEntry>> {
        let portmapper_port = config
            .and_then(|c| c.portmapper_port)
            .unwrap_or(PORTMAPPER_PORT);

        let mount_port = match config.and_then(|c| c.mount_port) {
            Some(port) => port,
            None => {
                let io = TokioConnector
                    .connect(SocketAddr::new(ip, portmapper_port))
                    .await
                    .with_context(|| format!("failed to reach the portmapper on {ip}"))?;
                PortmapperClient::new(io)
                    .getport(
                        nfs3_client::nfs3_types::mount::PROGRAM,
                        nfs3_client::nfs3_types::mount::VERSION,
                        nfs3_client::nfs3_types::portmap::IPPROTO_TCP,
                    )
                    .await
                    .context("the portmapper does not advertise a mount service")?
            }
        };

        let io = TokioConnector
            .connect(SocketAddr::new(ip, mount_port))
            .await
            .with_context(|| format!("failed to reach the mount service on {ip}:{mount_port}"))?;

        let exports = MountClient::new(io)
            .export()
            .await
            .context("failed to list exports")?;

        Ok(exports
            .into_inner()
            .into_iter()
            .map(|export| {
                let groups: Vec<String> = export
                    .ex_groups
                    .into_inner()
                    .into_iter()
                    .map(|group| String::from_utf8_lossy(group.0.as_ref()).into_owned())
                    .collect();
                ShareEntry {
                    name: String::from_utf8_lossy(export.ex_dir.0.as_ref()).into_owned(),
                    kind: "export".into(),
                    comment: (!groups.is_empty()).then(|| groups.join(", ")),
                }
            })
            .collect())
    }

    /// Split normalized components into (parent, final name).
    fn split(components: &[String]) -> Result<(&[String], &str)> {
        match components.split_last() {
            Some((name, parent)) => Ok((parent, name.as_str())),
            None => bail!("operation requires a path below the export root"),
        }
    }

    fn filename(name: &str) -> filename3<'static> {
        filename3(Opaque::owned(name.as_bytes().to_vec()))
    }

    fn kind(type_: ftype3) -> FileKind {
        match type_ {
            ftype3::NF3REG => FileKind::File,
            ftype3::NF3DIR => FileKind::Dir,
            ftype3::NF3LNK => FileKind::Symlink,
            _ => FileKind::Other,
        }
    }

    fn file_attr(attr: &fattr3) -> FileAttr {
        FileAttr {
            kind: kind(attr.type_),
            size: attr.size,
            modified: Some(i64::from(attr.mtime.seconds)),
            mode: Some(attr.mode),
        }
    }

    /// Build a listing entry. Attributes are optional because READDIRPLUS lets a
    /// server decline to send them per entry.
    fn dir_entry(name: String, attr: Option<&fattr3>) -> DirEntry {
        match attr {
            Some(attr) => DirEntry {
                name,
                kind: kind(attr.type_),
                size: attr.size,
                modified: Some(i64::from(attr.mtime.seconds)),
                mode: Some(attr.mode),
            },
            None => DirEntry {
                name,
                kind: FileKind::Other,
                size: 0,
                modified: None,
                mode: None,
            },
        }
    }
}

#[cfg(feature = "server")]
pub use server::{NfsFs, enumerate};
