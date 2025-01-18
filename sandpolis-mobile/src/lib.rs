use bevy::prelude::bevy_main;
use sandpolis::{agent::AgentCommandLine, client::ClientCommandLine, CommandLine};

#[bevy_main]
fn main() {
    sandpolis::client::main(CommandLine {
        // server_args: todo!(),
        client_args: ClientCommandLine {},
        agent_args: AgentCommandLine {
            read_only: false,
            poll: None,
            agent_socket: "/tmp".into(),
        },
        server: None,
        certificate: None,
        debug: false,
        trace: false,
        storage: "/tmp".into(),
    });
}
