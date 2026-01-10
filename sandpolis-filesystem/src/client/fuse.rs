use anyhow::Result;
use fuser::{
    FileAttr, FileType, Filesystem, MountOption, ReplyAttr, ReplyData, ReplyDirectory, ReplyEntry,
    Request,
};
use std::collections::HashMap;
use std::ffi::OsStr;
use std::sync::{Arc, Mutex};
use std::time::{Duration, SystemTime};

const TTL: Duration = Duration::from_secs(1);
const ROOT_INO: u64 = 1;

#[derive(Debug, Clone)]
pub struct FuseNode {
    pub ino: u64,
    pub parent: u64,
    pub name: String,
    pub attr: FileAttr,
    pub data: Vec<u8>,
    pub children: Vec<u64>,
}

impl FuseNode {
    fn new_dir(ino: u64, parent: u64, name: String) -> Self {
        let ts = SystemTime::now();
        Self {
            ino,
            parent,
            name,
            attr: FileAttr {
                ino,
                size: 0,
                blocks: 0,
                atime: ts,
                mtime: ts,
                ctime: ts,
                crtime: ts,
                kind: FileType::Directory,
                perm: 0o755,
                nlink: 2,
                uid: 1000,
                gid: 1000,
                rdev: 0,
                flags: 0,
                blksize: 512,
            },
            data: Vec::new(),
            children: Vec::new(),
        }
    }

    fn new_file(ino: u64, parent: u64, name: String, data: Vec<u8>) -> Self {
        let ts = SystemTime::now();
        let size = data.len() as u64;
        Self {
            ino,
            parent,
            name,
            attr: FileAttr {
                ino,
                size,
                blocks: size.div_ceil(512),
                atime: ts,
                mtime: ts,
                ctime: ts,
                crtime: ts,
                kind: FileType::RegularFile,
                perm: 0o644,
                nlink: 1,
                uid: 1000,
                gid: 1000,
                rdev: 0,
                flags: 0,
                blksize: 512,
            },
            data,
            children: Vec::new(),
        }
    }
}

pub struct SandpolisFilesystem {
    nodes: Arc<Mutex<HashMap<u64, FuseNode>>>,
    next_ino: Arc<Mutex<u64>>,
}

impl SandpolisFilesystem {
    pub fn new() -> Self {
        let mut nodes = HashMap::new();

        let root = FuseNode::new_dir(ROOT_INO, ROOT_INO, "/".to_string());
        nodes.insert(ROOT_INO, root);

        Self {
            nodes: Arc::new(Mutex::new(nodes)),
            next_ino: Arc::new(Mutex::new(2)),
        }
    }

    pub fn mount<P: AsRef<std::path::Path>>(self, mountpoint: P) -> Result<()> {
        let options = vec![
            MountOption::RO,
            MountOption::FSName("sandpolis".to_string()),
        ];

        fuser::mount2(self, mountpoint, &options)?;
        Ok(())
    }

    fn get_node(&self, ino: u64) -> Option<FuseNode> {
        self.nodes.lock().unwrap().get(&ino).cloned()
    }

    fn add_child(&self, parent_ino: u64, child_ino: u64) {
        let mut nodes = self.nodes.lock().unwrap();
        if let Some(parent) = nodes.get_mut(&parent_ino)
            && !parent.children.contains(&child_ino) {
                parent.children.push(child_ino);
            }
    }

    pub fn add_file<P: AsRef<str>>(&self, path: P, data: Vec<u8>) -> Result<u64> {
        let path = path.as_ref();
        let parts: Vec<&str> = path.trim_start_matches('/').split('/').collect();

        if parts.is_empty() || parts[0].is_empty() {
            return Err(anyhow::anyhow!("Invalid path"));
        }

        let mut current_ino = ROOT_INO;
        let mut nodes = self.nodes.lock().unwrap();

        for (i, part) in parts.iter().enumerate() {
            let is_last = i == parts.len() - 1;

            if let Some(current_node) = nodes.get(&current_ino) {
                let child_ino = current_node.children.iter().find(|&&child_ino| {
                    nodes
                        .get(&child_ino)
                        .map(|child| &child.name == part)
                        .unwrap_or(false)
                });

                if let Some(&existing_ino) = child_ino {
                    current_ino = existing_ino;
                } else {
                    let mut next_ino = self.next_ino.lock().unwrap();
                    let new_ino = *next_ino;
                    *next_ino += 1;
                    drop(next_ino);

                    let new_node = if is_last {
                        FuseNode::new_file(new_ino, current_ino, part.to_string(), data.clone())
                    } else {
                        FuseNode::new_dir(new_ino, current_ino, part.to_string())
                    };

                    nodes.insert(new_ino, new_node);

                    if let Some(parent) = nodes.get_mut(&current_ino) {
                        parent.children.push(new_ino);
                    }

                    current_ino = new_ino;
                }
            }
        }

        Ok(current_ino)
    }
}

impl Filesystem for SandpolisFilesystem {
    fn lookup(&mut self, _req: &Request, parent: u64, name: &OsStr, reply: ReplyEntry) {
        if let Some(parent_node) = self.get_node(parent) {
            for &child_ino in &parent_node.children {
                if let Some(child_node) = self.get_node(child_ino)
                    && child_node.name == name.to_string_lossy() {
                        reply.entry(&TTL, &child_node.attr, 0);
                        return;
                    }
            }
        }
        reply.error(2);
    }

    fn getattr(&mut self, _req: &Request<'_>, ino: u64, fh: Option<u64>, reply: ReplyAttr) {
        if let Some(node) = self.get_node(ino) {
            reply.attr(&TTL, &node.attr);
        } else {
            reply.error(2);
        }
    }

    fn read(
        &mut self,
        _req: &Request,
        ino: u64,
        _fh: u64,
        offset: i64,
        size: u32,
        _flags: i32,
        _lock: Option<u64>,
        reply: ReplyData,
    ) {
        if let Some(node) = self.get_node(ino) {
            if node.attr.kind == FileType::RegularFile {
                let offset = offset as usize;
                let size = size as usize;

                if offset < node.data.len() {
                    let end = std::cmp::min(offset + size, node.data.len());
                    reply.data(&node.data[offset..end]);
                } else {
                    reply.data(&[]);
                }
            } else {
                reply.error(21);
            }
        } else {
            reply.error(2);
        }
    }

    fn readdir(
        &mut self,
        _req: &Request,
        ino: u64,
        _fh: u64,
        offset: i64,
        mut reply: ReplyDirectory,
    ) {
        if let Some(node) = self.get_node(ino) {
            if node.attr.kind != FileType::Directory {
                reply.error(20);
                return;
            }

            let mut entries = vec![
                (ino, FileType::Directory, ".".to_string()),
                (node.parent, FileType::Directory, "..".to_string()),
            ];

            for &child_ino in &node.children {
                if let Some(child_node) = self.get_node(child_ino) {
                    entries.push((child_ino, child_node.attr.kind, child_node.name));
                }
            }

            for (i, (child_ino, kind, name)) in entries.iter().enumerate().skip(offset as usize) {
                if reply.add(*child_ino, (i + 1) as i64, *kind, name) {
                    break;
                }
            }
            reply.ok();
        } else {
            reply.error(2);
        }
    }
}

impl Default for SandpolisFilesystem {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_filesystem_creation() {
        let fs = SandpolisFilesystem::new();
        let root = fs.get_node(ROOT_INO).unwrap();
        assert_eq!(root.ino, ROOT_INO);
        assert_eq!(root.attr.kind, FileType::Directory);
    }

    #[test]
    fn test_add_file() {
        let fs = SandpolisFilesystem::new();
        let data = b"Hello, world!".to_vec();
        let ino = fs.add_file("test.txt", data.clone()).unwrap();

        let file_node = fs.get_node(ino).unwrap();
        assert_eq!(file_node.name, "test.txt");
        assert_eq!(file_node.data, data);
        assert_eq!(file_node.attr.kind, FileType::RegularFile);
    }

    #[test]
    fn test_add_nested_file() {
        let fs = SandpolisFilesystem::new();
        let data = b"Nested file content".to_vec();
        let ino = fs.add_file("dir1/dir2/nested.txt", data.clone()).unwrap();

        let file_node = fs.get_node(ino).unwrap();
        assert_eq!(file_node.name, "nested.txt");
        assert_eq!(file_node.data, data);
    }
}
