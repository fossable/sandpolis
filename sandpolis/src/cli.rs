use anyhow::Result;
use clap::Parser;
use clap::Subcommand;
use colored::Colorize;
#[cfg(feature = "server")]
use sandpolis_core::RealmName;
use sandpolis_database::DatabaseLayer;
use sandpolis_realm::RealmClusterCert;
use sandpolis_realm::RealmData;
use sandpolis_realm::RealmLayerData;
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
    /// Generate a new realm certificate for use with a client instance
    NewClientCert {
        /// Name of a realm that exists on the server
        #[clap(long, default_value = "default")]
        realm: RealmName,

        /// Output file path or none for STDOUT
        #[clap(long)]
        output: Option<PathBuf>,
    },

    /// Generate a new realm certificate for use with an agent instance
    NewAgentCert {
        /// Name of a realm that exists on the server
        #[clap(long, default_value = "default")]
        realm: RealmName,

        /// Output file path or none for STDOUT
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
            Commands::NewClientCert { realm, output } => {
                let db = Database::new(&config.database.storage)?;

                let realms: Collection<RealmData> = db
                    .document::<RealmLayerData>("/realm")?
                    .collection("/realms")?;
                let g = realms.get_document(realm)?.expect("the realm exists");
                let ca: Document<RealmClusterCert> = g.get_document("ca")?.expect("the CA exists");

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
