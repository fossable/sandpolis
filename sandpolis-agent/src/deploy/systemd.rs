//! The systemd unit a deployment installs on its target.
//!
//! These run on the *target* over SSH, which is why this is gated on the server
//! feature rather than the agent one — an agent manages its own host's units
//! through `sandpolis-health`, not through here.

use crate::PollConfig;
use crate::deploy::{DATA_PATH, INSTALL_PATH, REALM_FILE};

/// Where the unit file goes on the target.
pub const UNIT_PATH: &str = "/etc/systemd/system/sandpolis-agent.service";

/// The unit's name, as `systemctl` wants it.
pub const UNIT_NAME: &str = "sandpolis-agent.service";

/// Render the unit file.
///
/// The polling schedule is a command line option rather than something the
/// certificate carries, so this is where a deployment records it.
pub fn unit_file(poll: Option<&PollConfig>) -> String {
    let poll = match poll {
        Some(poll) => format!(
            " --poll '{}' --poll-timeout {}",
            // Cron expressions have no quotes to escape, and the schedule is
            // rejected on the way in, but a stray one would end the argument.
            poll.schedule.replace('\'', ""),
            poll.timeout_secs
        ),
        None => String::new(),
    };

    format!(
        "[Unit]\n\
         Description=Sandpolis Agent\n\
         After=network-online.target\n\
         Wants=network-online.target\n\
         \n\
         [Service]\n\
         ExecStart={INSTALL_PATH} agent --realm {REALM_FILE} --data {DATA_PATH}{poll}\n\
         Restart=always\n\
         RestartSec=5\n\
         \n\
         [Install]\n\
         WantedBy=multi-user.target\n"
    )
}

#[cfg(test)]
mod test {
    use super::*;

    /// The unit that shipped with the old deploy crate had a literal `{}` where
    /// `ExecStart` should have named the binary, so it could never have started
    /// anything.
    #[test]
    fn unit_names_the_installed_binary() {
        let unit = unit_file(None);
        // The subcommand is what makes this an agent; a bare invocation prints
        // help and exits.
        assert!(unit.contains(&format!("ExecStart={INSTALL_PATH} agent ")));
        assert!(unit.contains(REALM_FILE));
        assert!(!unit.contains("{}"));
    }

    /// A continuously connected agent is the absence of a schedule, so the unit
    /// says nothing about polling at all.
    #[test]
    fn continuous_agent_has_no_poll_flags() {
        let unit = unit_file(None);
        assert!(!unit.contains("--poll"));
    }

    /// Polling is what the deployment recorded, so it has to survive into the
    /// command the unit runs.
    #[test]
    fn polling_agent_carries_its_schedule() {
        let unit = unit_file(Some(&PollConfig {
            schedule: "0 */5 * * * *".into(),
            timeout_secs: 45,
        }));
        assert!(
            unit.contains("--poll '0 */5 * * * *' --poll-timeout 45"),
            "{unit}"
        );
    }
}
