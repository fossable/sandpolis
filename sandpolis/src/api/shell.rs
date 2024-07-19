//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
syntax = "proto3";

package plugin.shell;

option java_package = "org.s7s.plugin.shell";

enum ShellCapability {

    // Any POSIX shell
    SH = 0;

    // Microsoft PowerShell
    PWSH = 1;

    // Microsoft CMD.EXE
    CMD = 2;

    // Bourne Again Shell compatible
    BASH = 3;

    // Z Shell compatible
    ZSH = 4;

    // Korn Shell compatible
    KSH = 5;

    // C Shell compatible
    CSH = 6;
}

// Request to execute a command snippet in a shell
message RQ_Execute {

    // The path to the shell executable
    string shell_path = 1;

    // The command to execute
    string command = 2;

    // An execution timeout in seconds
    int32 timeout = 3;

    // Whether stdout will be ignored
    bool ignore_stdout = 4;

    // Whether stderr will be ignored
    bool ignore_stderr = 5;
}

// Response containing execution results
message RS_Execute {

    // The process's exit code
    int32 exitCode = 1;

    // The process's entire stdout
    string stdout = 2;

    // The process's entire stderr
    string stderr = 3;
}

// Request to locate supported shells on the system
message RQ_ListShells {
}

// Response containing supported shell information
message RS_ListShells {
    message DiscoveredShell {

        // A list of supported shell capabilities
        repeated ShellCapability capability = 1;

        // The location of the shell executable
        string location = 2;

        // A version number if available
        string version = 3;
    }

    repeated DiscoveredShell shell = 1;
}

// Request to start a new shell session
message RQ_ShellStream {

    // The desired stream ID
    int32 stream_id = 1;

    // The path to the shell executable
    string path = 2;

    // TODO request permissions
    // Permission permission = 3;

    // Additional environment variables
    map<string, string> environment = 4;

    // The number of rows to request
    int32 rows = 5;

    // The number of columns to request
    int32 cols = 6;
}

enum RS_ShellStream {
    SHELL_STREAM_OK = 0;
}

// Event containing standard-input to a shell
message EV_ShellStreamInput {

    // The input data
    bytes stdin = 1;

    // Update the number of rows
    int32 rows_changed = 2;

    // Update the number of columns
    int32 cols_changed = 3;
}

// Event containing standard-output and standard-error
message EV_ShellStreamOutput {

    // The process standard-output
    bytes stdout = 1;

    // The process standard-error
    bytes stderr = 2;
}
