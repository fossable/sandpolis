//! Server side of the boot stream: answers announcing boot agents with the
//! hold state and releases held ones once the flag clears.

use super::BootAgentData;
use super::streams::*;
use anyhow::Result;
use sandpolis_instance::InstanceId;
use sandpolis_instance::database::{RealmDatabase, ResidentVec};
use sandpolis_instance::network::StreamResponder;
use sandpolis_macros::Stream;
use std::sync::OnceLock;
use std::time::Duration;
use tokio::sync::mpsc::Sender;
use tracing::debug;

/// How often a held boot stream re-checks the hold flag.
const HOLD_POLL_INTERVAL: Duration = Duration::from_secs(1);

pub struct BootServerContext {
    /// Per-agent boot rows, written by this server.
    pub boot: ResidentVec<BootAgentData>,
}

impl BootServerContext {
    pub fn new(realm: RealmDatabase) -> Result<Self> {
        Ok(Self {
            boot: realm.resident_vec(())?,
        })
    }
}

/// Held in a static so [`BootStreamResponder`] can be constructed by the
/// stateless `inventory` factory — the deploy subsystem's arrangement.
static CONTEXT: OnceLock<BootServerContext> = OnceLock::new();

/// Install the server context. Called once at startup.
pub fn install(context: BootServerContext) {
    let _ = CONTEXT.set(context);
}

/// Whether a boot hold is currently set for the agent.
fn hold_set(context: &BootServerContext, agent: InstanceId) -> bool {
    context.boot.iter().any(|row| {
        let row = row.read();
        row._instance_id == agent && row.hold
    })
}

/// Server side of the boot stream.
#[derive(Stream, Default)]
pub struct BootStreamResponder;

impl StreamResponder for BootStreamResponder {
    type In = BootStreamRequest;
    type Out = BootStreamResponse;

    async fn on_message(&self, request: Self::In, sender: Sender<Self::Out>) -> Result<()> {
        let BootStreamRequest::Announce { agent } = request;

        let Some(context) = CONTEXT.get() else {
            sender.send(BootStreamResponse::Proceed).await?;
            return Ok(());
        };

        if !hold_set(context, agent) {
            sender.send(BootStreamResponse::Proceed).await?;
            return Ok(());
        }

        debug!(%agent, "Holding boot agent");
        sender.send(BootStreamResponse::Hold).await?;

        // Watch for the hold to clear. A closed sender means the agent
        // disconnected, which also ends the watch.
        tokio::spawn(async move {
            while !sender.is_closed() {
                tokio::time::sleep(HOLD_POLL_INTERVAL).await;
                if !hold_set(context, agent) {
                    debug!(%agent, "Releasing boot agent");
                    let _ = sender.send(BootStreamResponse::Release).await;
                    break;
                }
            }
        });
        Ok(())
    }
}
