
// Request that the agent alter its power state.
message PostPowerRequest {
    enum PowerState {
        POWEROFF = 0;
        REBOOT = 1;
    }

    PowerState new_state = 1;
}

enum PostPowerResponse {
    POST_POWER_OK = 0;
}

// Request that the boot agent be started.
//
// Sources      : client, server
// Destinations : agent
//
message RQ_LaunchBootAgent {

    // The UUID of the partition containing the boot agent
    string target_uuid = 1;
}

enum RS_LaunchBootAgent {
    LAUNCH_BOOT_AGENT_OK = 0;
    LAUNCH_BOOT_AGENT_ACCESS_DENIED = 1;
}

// Request a boot agent be uninstalled from the system.
//
// Sources      : client, server
// Destinations : agent
//
message RQ_UninstallBootAgent {

    // The UUID of the partition containing the boot agent
    string target_uuid = 1;
}

enum RS_UninstallBootAgent {
    UNINSTALL_BOOT_AGENT_OK = 0;
    UNINSTALL_BOOT_AGENT_ACCESS_DENIED = 1;
}

// Response listing boot agent installations.
//
// Sources      : client, server
// Destinations : agent
//
message RS_FindBootAgents {

    message BootAgentInstallation {

        // The partition UUID
        repeated string partition_uuid = 1;

        // The partition size in bytes
        int64 size = 2;

        // The boot agent's version string
        string version = 3;
    }

    repeated BootAgentInstallation installation = 1;
}

// Request a boot agent be installed on the system.
//
// Sources      : client, server
// Destinations : agent
//
message RQ_InstallBootAgent {

    // The UUID of the target partition
    string partition_uuid = 1;

    // The MAC address of the network interface to use for connections
    string interface_mac = 3;

    // Whether DHCP will be used
    bool use_dhcp = 4;

    // A static IP address as an alternative to DHCP
    string static_ip = 5;

    // The netmask corresponding to the static IP
    string netmask = 6;

    // The gateway IP
    string gateway_ip = 7;
}

enum RS_InstallBootAgent {
    INSTALL_BOOT_AGENT_OK = 0;
    INSTALL_BOOT_AGENT_ACCESS_DENIED = 1;
}

// Response listing boot agent installation candidates.
//
// Sources      : agent
// Destinations : client, server
// Request      : RQ_FindBootAgents
//
message RS_FindBootAgentCandidates {

    message DeviceCandidate {

        // The device's UUID
        string device_uuid = 1;

        // The size of the device in bytes
        int64 device_size = 2;

        // The device name
        string device_name = 3;
    }

    message PartitionCandidate {

        // The partition's UUID
        string partition_uuid = 1;

        // The size of the partition in bytes
        int64 partition_size = 2;

        // The device name upon which the partition exists
        string device_name = 3;

        // The amount of free space remaining on the device's ESP
        int64 esp_free_space = 4;
    }

    repeated DeviceCandidate device_candidate = 1;

    repeated PartitionCandidate partition_candidate = 2;
}
