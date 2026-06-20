use clap::Parser;

#[derive(Parser, Debug, Clone)]
pub struct AgentCommandLine {
    /// Run the agent in polling mode using this cron schedule (e.g.
    /// "0 */5 * * * *" to check in every five minutes) instead of staying
    /// continuously connected. Overrides the `poll` config.
    #[clap(long)]
    pub poll: Option<String>,

    /// How long the agent stays connected during each polling check-in, in
    /// seconds. Only meaningful together with `--poll`.
    #[clap(long)]
    pub poll_timeout: Option<u64>,
}
