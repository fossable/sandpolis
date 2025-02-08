use notify::Watcher;
use std::path::PathBuf;
pub mod routes;

struct FilesystemSession {
    cwd: PathBuf,
    watcher: Box<dyn Watcher>,
}
