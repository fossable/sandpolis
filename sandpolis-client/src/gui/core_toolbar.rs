//! Core-layer toolbar registration.
//!
//! The Server layer has no layer crate of its own, so its toolbar action is
//! registered here: a button that opens the existing login dialog. The Agent
//! layer is registered by `sandpolis_agent::client::gui::AgentClientPlugin`,
//! which owns the deploy dialog and the stream behind it — neither of which this
//! crate can reach, since `sandpolis-agent` depends on it rather than the other
//! way around.

use crate::gui::input::LoginDialogState;
use crate::gui::ui::controller::{LayerClientInfo, RegisterLayerClient};
use bevy::prelude::*;
use sandpolis_instance::InstanceType;

/// Registers the Server layer's client.
pub struct CoreLayerToolbarPlugin;

impl Plugin for CoreLayerToolbarPlugin {
    fn build(&self, app: &mut App) {
        app.register_layer_client(
            LayerClientInfo::new("Server", "Server instances in the cluster")
                .with_visible_instance_types(&[InstanceType::Server])
                .with_toolbar_action("Login to server", "toolbar/login.svg", |commands| {
                    commands.queue(|world: &mut World| {
                        if let Some(mut state) = world.get_resource_mut::<LoginDialogState>() {
                            state.show = true;
                        }
                    });
                }),
        );
    }
}
