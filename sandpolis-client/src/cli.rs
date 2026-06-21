use clap::Args;
use sandpolis_instance::InstanceId;

fn parse_instance_id(s: &str) -> Result<InstanceId, String> {
    s.parse().map_err(|e| format!("{e}"))
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
