// doc_comment! {
//     include_str!("../README.md")
// }

use sandpolis_realm::RealmName;

pub enum SshCredentials {
    Password(String),
    // TODO
}

/// The machine that will receive the agent installation.
pub enum DeployTarget {
    Local,
    Ssh(SshCredentials),
}

pub struct DeployConfig {
    realm: RealmName,
    // message LoopConfig {

    //     // One or more network targets
    //     repeated NetworkTarget target = 1;

    //     // The maximum number of connection iterations to attempt. A value of 0 indicates
    // unlimited.     int32 iteration_limit = 2;

    //     // The connection timeout in milliseconds
    //     int32 timeout = 3;

    //     // The time to wait after an unsuccessful connection attempt in milliseconds
    //     int32 cooldown = 4;

    //     // The maximum cooldown value. A value less than or equal to the initial cooldown
    // disables cooldown growth.     int32 cooldown_limit = 5;

    //     // The time in milliseconds required to increase the cooldown by one factor of its
    // initial value. Set high to     // reduce the speed at which the maximum cooldown is
    // reached. A value of 0 disables cooldown growth entirely.     double cooldown_constant =
    // 6; }

    // LoopConfig loop_config = 1;

    // // Whether the agent installer should be deleted upon successful execution
    // bool cleanup = 7;

    // // Whether the agent should be automatically started on boot
    // bool autostart = 8;

    // // Installation paths. Keys are the enum numbers from OsType.
    // map<int32, string> install_path = 10;

    // // If positive, the agent will poll the server at the configured interval
    // // instead of maintaining a persistent connection.
    // int32 poll_interval = 13;

    // bool memory = 14;
}

// Request to build an agent for the given realm.
pub struct BuildAgentRequest(DeployConfig);

enum BuildDeployerResponse {
    Ok,
    Failed,
}
