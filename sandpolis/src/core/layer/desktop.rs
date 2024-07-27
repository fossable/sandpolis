//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
syntax = "proto3";

package plugin.desktop;

option java_package = "org.s7s.plugin.desktop";

// Request for a listing of available desktops
message RQ_DesktopList {
}

// Response containing all available desktops
message RS_DesktopList {

    message Desktop {

        // The desktop name
        string name = 1;

        // The desktop width in pixels
        int32 width = 2;

        // The desktop height in pixels
        int32 height = 3;
    }

    repeated Desktop desktop = 1;
}

message RQ_DesktopStream {

    enum ColorMode {
        // Each pixel encoded in three bytes
        RGB888 = 0;

        // Each pixel encoded in two bytes
        RGB565 = 1;

        // Each pixel encoded in one byte
        RGB332 = 2;
    }

    enum CompressionMode {
        NONE = 0;

        ZLIB = 1;
    }

    // The requested stream ID
    int32 stream_id = 1;

    // The desktop to capture
    string desktop_uuid = 2;

    // The screen scale factor
    double scale_factor = 3;
}

enum RS_DesktopStream {
    DESKTOP_STREAM_OK = 0;
}

message EV_DesktopStreamInput {

    enum PointerButton {
        PRIMARY   = 0;
        MIDDLE    = 1;
        SECONDARY = 2;
        BACK      = 3;
        FORWARD   = 4;
    }

    //
    string key_pressed = 1;

    //
    string key_released = 2;

    //
    string key_typed = 3;

    //
    PointerButton pointer_pressed = 4;

    //
    PointerButton pointer_released = 5;

    // The X coordinate of the pointer
    int32 pointer_x = 6;

    // The Y coordinate of the pointer
    int32 pointer_y = 7;

    // Scale factor
    double scale_factor = 8;

    // Clipboard data
    string clipboard = 9;
}

message EV_DesktopStreamOutput {

    // The width of the destination block in pixels
    int32 width = 1;

    // The height of the destination block in pixels
    int32 height = 2;

    // The X coordinate of the destination block's top left corner
    int32 dest_x = 3;

    // The Y coordinate of the destination block's top left corner
    int32 dest_y = 4;

    // The X coordinate of the source block's top left corner
    int32 source_x = 5;

    // The Y coordinate of the source block's top left corner
    int32 source_y = 6;

    // The pixel data encoded according to the session's parameters
    bytes pixel_data = 7;

    // Clipboard data
    string clipboard = 8;
}

message RQ_CaptureScreenshot {
    // The desktop to capture
    string desktop_uuid = 1;
}

message RS_CaptureScreenshot {
    bytes data = 1;
}
