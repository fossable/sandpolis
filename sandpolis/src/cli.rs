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
        #[clap(long)]
        output: Option<PathBuf>,
    },

    // TODO no command
    InstallCert {},

    /// Show versions of all installed layers
    About,
}

impl Commands {
    pub fn dispatch(&self) -> Result<ExitCode> {
        match self {
            #[cfg(feature = "server")]
            Commands::GenerateCert { group, output } => {
                // let groups: Collection<GroupData> = db.collection("/groups")?;
                // let g = groups.get_document(&group)?.expect("the group exists");
                // let ca: Document<GroupCaCert> = g.get_document("ca")?.expect("the CA exists");

                // let cert = ca.data.client_cert()?;

                // info!(path = %output.display(), "Writing endpoint certificate");
                // let mut output = File::create(output)?;
                // output.write_all(&serde_json::to_vec(&cert)?)?;
                todo!()
            }
            Commands::InstallCert {} => todo!(),
            Commands::About => {
                println!();
                for (layer, version) in crate::layers().iter() {
                    println!("{layer}");
                }
            }
        }
        Ok(ExitCode::SUCCESS)
    }
}
