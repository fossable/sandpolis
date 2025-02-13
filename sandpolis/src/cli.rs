use clap::Parser;
use clap::Subcommand;
use std::path::PathBuf;

#[derive(Parser, Debug, Clone)]
#[clap(author, version, about, long_about = None)]
pub struct CommandLine {
    #[cfg(feature = "agent")]
    #[clap(flatten)]
    pub agent: sandpolis_agent::cli::AgentCommandLine,

    #[cfg(feature = "client")]
    #[clap(flatten)]
    pub client: sandpolis_client::cli::ClientCommandLine,

    #[clap(flatten)]
    pub database: sandpolis_database::cli::DatabaseCommandLine,

    #[clap(flatten)]
    pub group: sandpolis_group::cli::GroupCommandLine,

    #[clap(flatten)]
    pub instance: sandpolis_instance::cli::InstanceCommandLine,

    #[clap(flatten)]
    pub network: sandpolis_network::cli::NetworkCommandLine,

    #[cfg(feature = "server")]
    #[clap(flatten)]
    pub server: sandpolis_server::cli::ServerCommandLine,

    #[command(subcommand)]
    pub command: Option<Commands>,
}

#[derive(Subcommand, Debug, Clone)]
pub enum Commands {
    #[cfg(feature = "server")]
    /// Generate a new endpoint certificate signed by the group CA
    GenerateCert {
        /// Group to generate the certificate for
        #[clap(long, default_value = "default")]
        group: String, // TODO GroupName

        /// Output file path
        #[clap(long, default_value = "./endpoint.json")]
        output: PathBuf,
    },

    InstallCert {},
}
