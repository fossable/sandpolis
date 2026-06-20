//! Debug "Instance" layer.
//!
//! A diagnostic layer that shows every instance/node regardless of type (no
//! per-type visibility filtering), so duplicate or phantom nodes are all
//! visible at once. Its node controller shows metadata about the clicked node
//! only: the instance id, its decoded types, whether it is the local instance,
//! the cluster id, OS info, and every `ConnectionData` row that references it
//! (the connection ids, sockets, timestamps and byte counters).
//!
//! The layer's toolbar exposes a "View database" action that will open a
//! generic database browser. That browser is not implemented yet — see
//! [`open_database_browser`].

use crate::gui::queries;
use crate::gui::ui::bind::bind_text;
use crate::gui::ui::controller::NodeController;
use crate::gui::ui::theme::{Role, Theme};
use crate::gui::ui::widgets::{heading, text};
use bevy::prelude::*;
use sandpolis_instance::network::NetworkLayer;
use sandpolis_instance::{InstanceId, InstanceLayer};

/// The debug Instance layer's node controller.
///
/// Holds clones of the layers it reads (the [`NodeController::build`] signature
/// does not receive resources), matching how other controllers carry their own
/// data handles.
pub struct InstanceController {
    pub network: NetworkLayer,
    pub instance: InstanceLayer,
}

impl NodeController for InstanceController {
    fn title(&self) -> &str {
        "Instance"
    }

    fn build(&self, commands: &mut Commands, body: Entity, instance: InstanceId, theme: &Theme) {
        let is_local = instance == self.instance.instance_id;
        let cluster = self.instance.cluster_id;
        let types = instance.types();
        let os = queries::query_instance_metadata(instance)
            .ok()
            .map(|m| m.os_type);

        // Captured by the live connection list below.
        let network = self.network.clone();

        commands.entity(body).with_children(|p| {
            p.spawn(heading(theme, "Identity"));
            p.spawn(text(
                theme,
                format!("ID: {instance}"),
                theme.metrics.font_md,
                Role::Text,
            ));
            p.spawn(text(
                theme,
                format!("Types: {types:?}"),
                theme.metrics.font_md,
                Role::Text,
            ));
            p.spawn(text(
                theme,
                format!("Local: {is_local}"),
                theme.metrics.font_md,
                Role::Text,
            ));
            p.spawn(text(
                theme,
                format!("Cluster: {cluster}"),
                theme.metrics.font_md,
                Role::Text,
            ));
            if let Some(os) = os {
                p.spawn(text(
                    theme,
                    format!("OS: {os}"),
                    theme.metrics.font_md,
                    Role::Text,
                ));
            }

            p.spawn(heading(theme, "Connections"));
            // Live-updating: re-read the connection rows referencing this
            // instance every frame so newly established / torn-down connections
            // are reflected without rebuilding the panel.
            p.spawn((
                text(theme, "", theme.metrics.font_sm, Role::TextMuted),
                bind_text(move || describe_connections(&network, instance)),
            ));
        });
    }
}

/// Summarize every `ConnectionData` row referencing `instance` (as either
/// endpoint) for the controller's live connection list.
fn describe_connections(network: &NetworkLayer, instance: InstanceId) -> String {
    let mut lines = Vec::new();
    for connection in network.connections.iter() {
        let cd = connection.read();
        if cd._instance_id == instance || cd.remote_instance == instance {
            lines.push(format!(
                "{} -> {}\n  local={:?} remote={:?}\n  established={} disconnected={:?}\n  r/w={}/{}B",
                cd._instance_id,
                cd.remote_instance,
                cd.local_socket,
                cd.remote_socket,
                cd.established,
                cd.disconnected,
                cd.read_bytes,
                cd.write_bytes,
            ));
        }
    }

    if lines.is_empty() {
        "No connection rows reference this instance.".to_string()
    } else {
        format!("{} connection row(s):\n{}", lines.len(), lines.join("\n"))
    }
}

/// Toolbar callback for the Instance layer's "View database" action.
///
/// TODO(instance-layer): open a generic database browser that scans every
/// registered `#[data]` table (mirroring `sandpolis::MODELS`) and renders the
/// rows. Stubbed for now.
pub fn open_database_browser(_commands: &mut Commands) {
    warn!("Instance layer: database browser is not implemented yet");
}
