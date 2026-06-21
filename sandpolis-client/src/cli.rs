use clap::Args;
use sandpolis_instance::InstanceId;

fn parse_instance_id(s: &str) -> Result<InstanceId, String> {
    s.parse().map_err(|e| format!("{e}"))
}

/// Flags shared by client subcommands that act on a specific instance. With
/// `--json` (and `--instance`) the command runs noninteractively and prints a
/// machine-readable result instead of opening a TUI.
///
/// Both flags are `global`, so they're accepted before or after the subcommand
/// (e.g. `sandpolis agent restart --json --instance <id>`). Flatten this once at
/// the top level rather than per-subcommand.
#[derive(Args, Clone, Debug, Default)]
pub struct TargetArgs {
    /// Emit machine-readable JSON instead of opening a TUI
    #[clap(long, global = true)]
    pub json: bool,

    /// Target instance; required for noninteractive (`--json`) operation
    #[clap(long, global = true, value_parser = parse_instance_id)]
    pub instance: Option<InstanceId>,
}
