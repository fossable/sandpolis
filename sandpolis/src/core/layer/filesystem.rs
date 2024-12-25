//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
syntax = "proto3";

package plugin.filesystem;

option java_package = "org.s7s.plugin.filesystem";

// Start a new stream that will receive file/directory updates.
message RQ_DirectoryStream {

    int32 stream_id = 1;

    // The initial directory path
    string path = 2;

    // Indicates whether file sizes should be included
    bool include_sizes = 3;

    // Indicates whether creation timestamps should be included
    bool include_create_timestamps = 4;

    // Indicates whether modification timestamps should be included
    bool include_modify_timestamps = 5;

    // Indicates whether access timestamps should be included
    bool include_access_timestamps = 6;

    // Indicates whether MIME types should be included
    bool include_mime_types = 7;

    // Indicates whether file owners should be included
    bool include_owners = 8;

    // Indicates whether file groups should be included
    bool include_groups = 9;
}

// Response to a directory stream request.
enum RS_DirectoryStream {
    DIRECTORY_STREAM_OK = 0;
    DIRECTORY_STREAM_FAILED_PATH_NOT_EXISTS = 1;
}

// Updates to a directory stream.
message EV_DirectoryStream {

    message DirectoryEntry {

        enum Type {

            REGULAR_FILE = 0;
            DIRECTORY = 1;
            FIFO = 2;
            SOCKET = 3;
            CHARACTER = 4;
            BLOCK = 5;
            SYMLINK = 6;
            HARDLINK = 7;
        }

        // Indicates the file type
        Type type = 1;

        // The file's name
        string name = 2;

        // The file's creation time
        int64 create_timestamp = 3;

        // The file's modification time
        int64 modify_timestamp = 4;

        // The file's access time
        int64 access_timestamp = 5;

        // The file's size in bytes or number of elements if directory
        int64 size = 6;

        // The file's MIME type
        string mime_type = 7;

        enum UpdateType {

            // Indicates an entry has been added
            ENTRY_CREATE = 0;

            // Indicates an entry has been removed
            ENTRY_DELETE = 1;

            // Indicates an entry has been modified
            ENTRY_MODIFY = 2;

            // Indicates some updates were dropped and the listing should be refreshed
            OVERFLOW = 3;
        }

        UpdateType update_type = 8;
    }

    // The directory's absolute path
    string path = 1;

    // Listing updates
    repeated DirectoryEntry entry = 2;
}

message RQ_MountStreamFuse {

    int64 stream_id = 1;

    // The directory's absolute path
    string path = 2;
}

enum RS_MountStreamFuse {
    MOUNT_STREAM_OK = 0;
}

message EV_MountStreamFuse {

    /**
     * Look up a directory entry by name and get its attributes.
     *
     * Valid replies:
     *   fuse_reply_entry
     *   fuse_reply_err
     */
    message lookup {

        // Inode number of the parent directory.
        int32 parent = 1;

        // The name to look up
        string name = 2;
    }
}

/// Delete one or more files from the filesystem.
pub struct FilesystemDeleteRequest {

    /// Absolute paths to delete
    pub targets: Vec<PathBuf>,

    /// Whether to recursively delete directories
    pub recursive: bool,
}

pub enum FilesystemDeleteResponse {
    Ok,
}
