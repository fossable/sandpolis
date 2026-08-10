use clap::Args;
use clap::Parser;
use sandpolis_instance::InstanceId;

fn parse_instance_id(s: &str) -> Result<InstanceId, String> {
    s.parse().map_err(|e| format!("{e}"))
}

/// Client settings. Clients never read a config file — only the global stratum
/// server does — so anything tunable arrives here.
#[derive(Parser, Debug, Clone, Default)]
pub struct ClientCommandLine {
    /// Frame rate for the GUI and TUI.
    #[clap(long)]
    pub fps: Option<u32>,
}

/// Flags shared by client subcommands that act on a specific instance. With
/// `--json` (and `--instance`) the command runs noninteractively and prints a
/// machine-readable result instead of opening a TUI.
///
/// Flatten this into each subcommand that targets an instance.
#[derive(Args, Clone, Debug, Default)]
pub struct TargetArgs {
    /// Emit machine-readable JSON instead of opening a TUI
    #[clap(long)]
    pub json: bool,

    /// Target instance; required for noninteractive (`--json`) operation
    #[clap(long, value_parser = parse_instance_id)]
    pub instance: Option<InstanceId>,
}
