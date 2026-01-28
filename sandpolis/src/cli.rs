use crate::config::Configuration;
use anyhow::Result;
use clap::Parser;
use clap::Subcommand;
use colored::Colorize;
use sandpolis_instance::realm::RealmName;
use std::path::PathBuf;
use std::process::ExitCode;
use tracing::info;

#[derive(Parser, Debug, Clone)]
#[clap(author, version, about = "Test")]
pub struct CommandLine {
    #[cfg(feature = "client")]
    #[clap(flatten)]
    pub client: sandpolis_client::cli::ClientCommandLine,

    #[clap(flatten)]
    pub instance: sandpolis_instance::cli::InstanceCommandLine,

    #[clap(flatten)]
    pub network: sandpolis_instance::network::cli::NetworkCommandLine,

    #[clap(flatten)]
    pub database: sandpolis_instance::database::cli::DatabaseCommandLine,

    #[clap(flatten)]
    pub realm: sandpolis_instance::realm::cli::RealmCommandLine,

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

    #[cfg(feature = "server")]
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
    #[allow(unused_variables)]
    pub async fn dispatch(self, config: &Configuration) -> Result<ExitCode> {
        match self {
            #[cfg(feature = "server")]
            Commands::NewClientCert { realm, output } => {
                use sandpolis_instance::realm::RealmClusterCert;

                let database = sandpolis_instance::database::DatabaseLayer::new(
                    config.database.clone(),
                    &crate::MODELS,
                )?;

                let db = database.realm(realm.parse()?)?;
                let r = db.r_transaction()?;

                let cluster_certs: Vec<RealmClusterCert> =
                    r.scan().primary()?.all()?.collect::<Result<Vec<_>, _>>()?;

                let Some(cluster_cert) = cluster_certs.first() else {
                    return Err(anyhow::anyhow!("No cluster cert found"));
                };

                let client_cert = cluster_cert.client_cert()?;

                if let Some(path) = output {
                    info!(path = %path.display(), "Writing endpoint certificate");
                    std::fs::write(path, &serde_json::to_vec(&client_cert)?)?;
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
