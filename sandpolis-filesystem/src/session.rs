use crate::FilesystemLayer;
use axum::extract::State;
use axum::extract::{self, WebSocketUpgrade};
use axum::http::StatusCode;
use notify::Watcher;
use sandpolis_network::RequestResult;
use serde::{Deserialize, Serialize};
use std::path::PathBuf;

/// Start a new stream that will receive filesystem updates.
#[derive(Serialize, Deserialize)]
pub struct FsSessionRequest {
    /// The initial working directory
    pub path: PathBuf,

    /// Compute MIME types (might be expensive)
    pub mime_types: bool,

    /// Explicitly request polling watcher
    pub polling: bool,
}

#[derive(Serialize, Deserialize)]
pub enum FsSessionResponse {
    Ok(u64),
    PathNotFound,
}

#[derive(Serialize, Deserialize)]
pub enum FsSessionEvent {
    Created,
    Deleted,
    Modified,
    SetCwd,
}

#[derive(Serialize, Deserialize)]
pub struct FsMountRequest {
    /// The initial working directory
    pub path: PathBuf,
}

// Updates to a directory stream.
// message EV_DirectoryStream {

//     message DirectoryEntry {

//         // Indicates the file type
//         // TODO fuser type
//         Type type = 1;

//         // The file's name
//         string name = 2;

//         // TODO fuser type FileAttr

//         // The file's MIME type
//         string mime_type = 7;

//         enum UpdateType {

//             // Indicates an entry has been added
//             ENTRY_CREATE = 0;

//             // Indicates an entry has been removed
//             ENTRY_DELETE = 1;

//             // Indicates an entry has been modified
//             ENTRY_MODIFY = 2;

//             // Indicates some updates were dropped and the listing should be refreshed
//             OVERFLOW = 3;
//         }

//         UpdateType update_type = 8;
//     }

//     // The directory's absolute path
//     string path = 1;

//     // Listing updates
//     repeated DirectoryEntry entry = 2;
// }

#[derive(Serialize, Deserialize)]
pub enum FsMountEvent {
    /// Look up a directory entry by name and get its attributes.
    Lookup {
        /// Inode number of the parent directory.
        parent: i32,

        /// The name to look up
        name: String,
    },
}

/// Delete one or more files from the filesystem.
#[derive(Serialize, Deserialize)]
pub struct FsDeleteRequest {
    /// Absolute paths to delete
    pub targets: Vec<PathBuf>,

    /// Whether to recursively delete directories
    pub recursive: bool,
}

#[derive(Serialize, Deserialize)]
pub enum FsDeleteResponse {
    Ok,
    PathNotFound,
}

struct FilesystemSession {
    cwd: PathBuf,
    watcher: Box<dyn Watcher>,
}

#[axum_macros::debug_handler]
pub async fn session(
    state: State<FilesystemLayer>,
    ws: WebSocketUpgrade,
    extract::Json(request): extract::Json<FsSessionRequest>,
) -> Result<(), StatusCode> {
    // let session = ShellSession::new(request).map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    // ws.on_upgrade(move |socket| session.run(socket));

    Ok(())
}

#[axum_macros::debug_handler]
pub async fn delete(
    state: State<FilesystemLayer>,
    extract::Json(request): extract::Json<FsDeleteRequest>,
) -> RequestResult<FsDeleteResponse> {
    todo!()
}
