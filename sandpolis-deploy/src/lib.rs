//! Enables custom agents called "deployers" to be built and deployed. Group certificates and configuration are embedded into the application.

/// The machine that will receive the agent installation.
pub enum DeployTarget {
    Local,
    Ssh(String),
}

message AgentConfig {

    message NetworkTarget {

        // The DNS or IP address
        string address = 1;

        // The port number
        int32 port = 2;
    }

    message LoopConfig {

        // One or more network targets
        repeated NetworkTarget target = 1;

        // The maximum number of connection iterations to attempt. A value of 0 indicates unlimited.
        int32 iteration_limit = 2;

        // The connection timeout in milliseconds
        int32 timeout = 3;

        // The time to wait after an unsuccessful connection attempt in milliseconds
        int32 cooldown = 4;

        // The maximum cooldown value. A value less than or equal to the initial cooldown disables cooldown growth.
        int32 cooldown_limit = 5;

        // The time in milliseconds required to increase the cooldown by one factor of its initial value. Set high to
        // reduce the speed at which the maximum cooldown is reached. A value of 0 disables cooldown growth entirely.
        double cooldown_constant = 6;
    }

    LoopConfig loop_config = 1;

    // When true, the agent will not be able to connect to services outside of
    // the Sandpolis network.
    bool strict_network = 6;

    // Whether the agent installer should be deleted upon successful execution
    bool cleanup = 7;

    // Whether the agent should be automatically started on boot
    bool autostart = 8;

    // Whether the agent will attempt to install with a different configuration
    // in the event that the initial installation fails for any reason. This can
    // reduce the amount of manual remediation required.
    bool recover = 9;

    // Installation paths. Keys are the enum numbers from OsType.
    map<int32, string> install_path = 10;

    // If positive, the agent will poll the server at the configured interval
    // instead of maintaining a persistent connection.
    int32 poll_interval = 13;

    bool memory = 14;
}

// Request to build an agent for the given group.
message RQ_BuildAgent {

    message GeneratorOptions {

        // The payload type
        string payload = 1;
    }

    message PackagerOptions {

        // The output format of this client
        string format = 1;
    }

    message DeploymentOptions {

        // The target hostname
        string ssh_host = 1;

        // The target SSH username
        string ssh_username = 2;

        // The target SSH password
        string ssh_password = 3;

        // The SSH private key
        bytes ssh_private_key = 4;
    }

    // The group ID
    string group = 1;

    // The agent configuration
    //core.instance.AgentConfig config = 2;

    // Options for the generator component
    GeneratorOptions generator_options = 3;

    // Options for the packager component
    PackagerOptions packager_options = 4;

    // Options for the deployment component
    DeploymentOptions deployment_options = 5;
}

enum BuildDeployerResponse {
    Ok,
    Failed,
}

