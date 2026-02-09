use sandpolis_instance::network::StreamResponder;
use sandpolis_macros::Stream;
use serde::{Deserialize, Serialize};
use std::path::PathBuf;

/// Start a new stream that will receive filesystem updates.
#[derive(Serialize, Deserialize)]
pub enum FsSessionRequest {
    Init {
        /// The initial working directory
        path: PathBuf,

        /// Compute MIME types (might be expensive)
        mime_types: bool,

        /// Explicitly request polling watcher
        polling: bool,
    },
    Mount {
        /// The initial working directory
        path: PathBuf,
    },
    /// Look up a directory entry by name and get its attributes.
    FuseLookup {
        /// Inode number of the parent directory.
        parent: i32,

        /// The name to look up
        name: String,
    },
    /// Delete one or more files from the filesystem.
    Delete {
        /// Absolute paths to delete
        targets: Vec<PathBuf>,

        /// Whether to recursively delete directories
        recursive: bool,
    },
}

#[derive(Serialize, Deserialize)]
pub enum FsSessionResponse {
    Created,
    Deleted,
    Modified,
    SetCwd,
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

#[cfg(feature = "agent")]
#[derive(Stream)]
struct FsSessionStreamResponder {
    cwd: PathBuf,
    watcher: notify::RecommendedWatcher,
}

#[cfg(feature = "agent")]
impl StreamResponder for FsSessionStreamResponder {
    type In = FsSessionRequest;
    type Out = FsSessionResponse;
}
