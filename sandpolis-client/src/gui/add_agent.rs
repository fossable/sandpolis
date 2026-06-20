//! "Add agent" dialog and core-layer toolbar registration.
//!
//! The Server and Agent layers are "core" layers without their own layer crate /
//! plugin, so their toolbar actions are registered here by [`CoreLayerToolbarPlugin`]:
//! the Server layer opens the existing login dialog, and the Agent layer opens the
//! minimal [`AddAgentDialogState`] dialog defined below.
//!
//! Agent deployment has no backend yet (SSH deploy is a roadmap item), so the
//! dialog collects a target address and its submit is a clearly-marked TODO.

use crate::gui::input::LoginDialogState;
use crate::gui::ui::controller::{LayerClientInfo, RegisterLayerClient};
use crate::gui::ui::panel::modal_scrim;
use crate::gui::ui::text_input::text_input;
use bevy::text::EditableText;
use crate::gui::ui::theme::{Role, Theme, ThemedBg, ThemedBorder};
use crate::gui::ui::widgets::{button, heading, muted, text};
use bevy::input_focus::{FocusCause, InputFocus};
use bevy::prelude::*;
use bevy_ui_widgets::Activate;
use sandpolis_instance::{InstanceType, LayerName};
use tracing::info;

/// State of the "add agent" dialog.
#[derive(Resource, Default)]
pub struct AddAgentDialogState {
    pub show: bool,
    pub address: String,
    pub error_message: Option<String>,
}

/// Modal root marker.
#[derive(Component)]
pub struct AddAgentRoot;

/// Target-address input marker.
#[derive(Component)]
pub struct AddAgentAddressInput;

/// Registers core-layer toolbar actions and the add-agent dialog systems.
pub struct CoreLayerToolbarPlugin;

impl Plugin for CoreLayerToolbarPlugin {
    fn build(&self, app: &mut App) {
        app.init_resource::<AddAgentDialogState>()
            .add_systems(
                Update,
                (manage_add_agent, focus_add_agent_input, sync_add_agent_inputs),
            )
            // Server layer: open the existing login dialog.
            .register_layer_client(
                LayerClientInfo::new("Server", "Server instances in the cluster")
                    .with_visible_instance_types(&[InstanceType::Server])
                    .with_toolbar_action("Login to server", "toolbar/login.svg", |commands| {
                        commands.queue(|world: &mut World| {
                            if let Some(mut state) = world.get_resource_mut::<LoginDialogState>() {
                                state.show = true;
                            }
                        });
                    }),
            )
            // Agent layer: open the add-agent dialog.
            .register_layer_client(
                LayerClientInfo::new(
                    LayerName::from("Agent"),
                    "Managed instances running the agent",
                )
                .with_toolbar_action("Add agent", "toolbar/add_agent.svg", |commands| {
                    commands.queue(|world: &mut World| {
                        if let Some(mut state) = world.get_resource_mut::<AddAgentDialogState>() {
                            state.show = true;
                        }
                    });
                }),
            );
    }
}

/// Spawn/despawn the add-agent modal.
pub fn manage_add_agent(
    mut commands: Commands,
    theme: Res<Theme>,
    state: Res<AddAgentDialogState>,
    root: Query<Entity, With<AddAgentRoot>>,
    mut focus: ResMut<InputFocus>,
) {
    let exists = !root.is_empty();
    if state.show && !exists {
        commands
            .spawn((AddAgentRoot, modal_scrim()))
            .with_children(|scrim| {
                scrim
                    .spawn((
                        Node {
                            flex_direction: FlexDirection::Column,
                            width: Val::Px(360.0),
                            padding: UiRect::all(Val::Px(16.0)),
                            row_gap: Val::Px(6.0),
                            border: UiRect::all(Val::Px(1.0)),
                            ..default()
                        },
                        BackgroundColor(theme.color(Role::Panel)),
                        ThemedBg(Role::Panel),
                        BorderColor::all(theme.color(Role::Border)),
                        ThemedBorder(Role::Border),
                    ))
                    .with_children(|p| {
                        p.spawn(heading(&theme, "Add Agent"));
                        p.spawn(muted(&theme, "Agent address", theme.metrics.font_sm));
                        p.spawn((AddAgentAddressInput, text_input(&theme)));
                        p.spawn(text(
                            &theme,
                            "Agent deployment is not yet implemented.",
                            theme.metrics.font_sm,
                            Role::Warn,
                        ));
                        p.spawn(Node {
                            column_gap: Val::Px(8.0),
                            ..default()
                        })
                        .with_children(|row| {
                            row.spawn(button(&theme, "Add")).observe(on_add_agent_submit);
                            row.spawn(button(&theme, "Cancel"))
                                .observe(on_add_agent_cancel);
                        });
                    });
            });
    } else if !state.show && exists {
        for entity in &root {
            commands.entity(entity).despawn();
        }
        focus.clear();
    }
}

/// Focus the address field when the dialog opens.
pub fn focus_add_agent_input(
    inputs: Query<Entity, Added<AddAgentAddressInput>>,
    mut focus: ResMut<InputFocus>,
) {
    if let Ok(entity) = inputs.single() {
        focus.set(entity, FocusCause::Navigated);
    }
}

/// Copy the address input into [`AddAgentDialogState`].
pub fn sync_add_agent_inputs(
    mut state: ResMut<AddAgentDialogState>,
    address: Query<&EditableText, With<AddAgentAddressInput>>,
) {
    if let Ok(input) = address.single() {
        let value = input.value().to_string();
        if state.address != value {
            state.address = value;
        }
    }
}

fn on_add_agent_submit(_activate: On<Activate>, mut state: ResMut<AddAgentDialogState>) {
    // TODO: wire to agent deployment once the deploy backend exists.
    info!(address = %state.address, "Add agent requested (deployment not yet implemented)");
    state.error_message = None;
    state.show = false;
    state.address.clear();
}

fn on_add_agent_cancel(_activate: On<Activate>, mut state: ResMut<AddAgentDialogState>) {
    state.show = false;
    state.address.clear();
    state.error_message = None;
}
