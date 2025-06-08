use anyhow::Result;
use clap::Parser;
use clap::Subcommand;
use colored::Colorize;
use sandpolis_database::DatabaseLayer;
use sandpolis_group::GroupCaCert;
use sandpolis_group::GroupData;
use sandpolis_group::GroupLayerData;
use std::fs::File;
use std::path::PathBuf;
use std::process::ExitCode;
use tracing::info;

use crate::config::Configuration;

#[derive(Parser, Debug, Clone)]
#[clap(author, version, about = "Test")]
pub struct CommandLine {
    #[cfg(feature = "client")]
    #[clap(flatten)]
    pub client: sandpolis_client::cli::ClientCommandLine,

    #[clap(flatten)]
    pub instance: sandpolis_instance::cli::InstanceCommandLine,

    #[clap(flatten)]
    pub network: sandpolis_network::cli::NetworkCommandLine,

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
        #[clap(long)]
        output: Option<PathBuf>,
    },

    // TODO no command
    InstallCert {},

    /// Show versions of all installed layers
    About,

    /// Run a server instance
    #[cfg(feature = "server")]
    #[cfg(any(feature = "agent", feature = "client"))]
    Server,

    /// Run a client instance
    #[cfg(feature = "client")]
    #[cfg(any(feature = "agent", feature = "server"))]
    Client,

    /// Run an agent instance
    #[cfg(feature = "agent")]
    #[cfg(any(feature = "server", feature = "client"))]
    Agent,
}

impl Commands {
    pub fn dispatch(&self, config: &Configuration) -> Result<ExitCode> {
        match self {
            #[cfg(feature = "server")]
            Commands::GenerateCert { group, output } => {
                let db = Database::new(&config.database.storage)?;

                let groups: Collection<GroupData> = db
                    .document::<GroupLayerData>("/group")?
                    .collection("/groups")?;
                let g = groups.get_document(group)?.expect("the group exists");
                let ca: Document<GroupCaCert> = g.get_document("ca")?.expect("the CA exists");

                let cert = ca.data.client_cert()?;

                if let Some(path) = output {
                    info!(path = %path.display(), "Writing endpoint certificate");
                    std::fs::write(path, &serde_json::to_vec(&cert)?)?;
                } else {
                    todo!()
                }
            }
            Commands::InstallCert {} => todo!(),
            Commands::About => {
                for line in fossable::sandpolis_word() {
                    println!("{line}");
                }
                println!("{} {}", "Layer".bold(), "Version".bold());
                for (layer, version) in crate::layers().iter() {
                    println!(
                        "{layer} {}.{}.{}",
                        version.major, version.minor, version.patch
                    );
                }
            }
            _ => panic!("Remaining commands are dispatched by caller"),
        }
        Ok(ExitCode::SUCCESS)
    }
}
