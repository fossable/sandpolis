//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
syntax = "proto3";

package plugin.device;

option java_package = "org.s7s.plugin.device";

// Request that the receiver scan its local network for
message RQ_FindSubagents {

    // An enumeration of all available communicator types.
    enum CommunicatorType {
        SSH   = 0;
        SNMP  = 1;
        IPMI  = 2;
        HTTP  = 3;
        ONVIF = 4;
        RTSP  = 5;
        WOL   = 6;
    }

    // If specified, the search will be restricted to the given networks (CIDR)
    repeated string network = 1;

    // If specified, the search will be restricted to the given communicator types
    repeated CommunicatorType communicator = 2;
}

message RS_FindSubagents {

    message SshDevice {

        // The device's IP address
        string ip_address = 1;

        // The device's SSH fingerprint
        string fingerprint = 2;
    }

    message SnmpDevice {

        // The device's IP address
        string ip_address = 1;
    }

    message IpmiDevice {

        // The device's IP address
        string ip_address = 1;
    }

    message OnvifDevice {

        // The device's IP address
        string ip_address = 1;
    }

    message HttpDevice {

        // The device's IP address
        string ip_address = 1;

        // Whether HTTPS is supported
        bool secure = 2;
    }

    message RtspDevice {

        // The device's IP address
        string ip_address = 1;
    }

    message WolDevice {

        // The device's IP address
        string ip_address = 1;

        // The device's MAC address
        string mac_address = 2;
    }

    repeated SshDevice ssh_device = 1;

    repeated SnmpDevice snmp_device = 2;

    repeated IpmiDevice ipmi_device = 3;

    repeated HttpDevice http_device = 4;

    repeated OnvifDevice onvif_device = 5;

    repeated RtspDevice rtsp_device = 6;

    repeated WolDevice wol_device = 7;
}

message RQ_RegisterSubagent {

    oneof target {
        string ip_address = 1;
        string mac_address = 2;
    }

    // The uuid of the gateway instance
    string gateway_uuid = 3;
}

enum RS_RegisterSubagent {
    REGISTER_SUBAGENT_OK = 0;
}

// Request an IPMI command be executed
message RQ_IpmiCommand {

    // The IPMI command
    string command = 1;
}

// Request an SNMP walk operation be executed
message RQ_SnmpWalk {

    // The OID to retrieve
    string oid = 1;
}

// Response containing the result of a walk operation
message RS_SnmpWalk {

    message Data {

        // The retrieved OID
        string oid = 1;

        // The OID's type
        string type = 2;

        // The OID's value
        string value = 3;
    }

    repeated Data data = 1;
}

message RQ_SendWolPacket {

}

enum RS_SendWolPacket {
    SEND_WOL_PACKET_OK = 0;
}
