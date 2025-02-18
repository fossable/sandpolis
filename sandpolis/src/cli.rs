use anyhow::Result;
use clap::Parser;
use clap::Subcommand;
use std::path::PathBuf;
use std::process::ExitCode;

#[derive(Parser, Debug, Clone)]
#[clap(author, version, about, long_about = None)]
pub struct CommandLine {
    #[cfg(feature = "client")]
    #[clap(flatten)]
    pub client: sandpolis_client::cli::ClientCommandLine,

    #[clap(flatten)]
    pub instance: sandpolis_instance::cli::InstanceCommandLine,

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

    // TODO no command
    InstallCert {},

    /// Show versions of all installed layers
    About,
}

impl Commands {
    pub fn dispatch(&self) -> Result<ExitCode> {
        match self {
            Commands::GenerateCert { group, output } => todo!(),
            Commands::InstallCert {} => todo!(),
            Commands::About => todo!(),
        }
    }
}
