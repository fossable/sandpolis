//! GUI components for the Shell layer.
//!
//! Provides a minimal native terminal node controller (scrollback + single-line
//! prompt) and the layer's client plugin. This matches today's echo-only behavior
//! (shell session routing is itself unimplemented); a real VT100/PTY terminal is
//! deferred.

use bevy::prelude::*;
use sandpolis_client::gui::ui::Activate;
use sandpolis_client::gui::ui::bind::bind_text;
use sandpolis_client::gui::ui::controller::{
    LayerClientInfo, NodeController, RegisterLayerClient,
};
use sandpolis_client::gui::ui::text_input::{TextInput, TextSubmit, text_input};
use sandpolis_client::gui::ui::theme::{Role, Theme};
use sandpolis_client::gui::ui::widgets::{button, muted, row, text};
use sandpolis_instance::{InstanceId, InstanceType, LayerName};

/// Shell session information.
#[derive(Clone, Debug)]
pub struct ShellSession {
    pub session_id: String,
    pub shell_type: String,
}

/// Query shell sessions for an instance.
pub fn query_shell_sessions(_id: InstanceId) -> anyhow::Result<Vec<ShellSession>> {
    // TODO: Query from shell resident
    Ok(vec![])
}

/// Marks the scrollback container of a terminal controller.
#[derive(Component)]
pub struct TerminalScrollback;

/// Marks the prompt input and points at its scrollback container.
#[derive(Component)]
pub struct TerminalPrompt {
    pub instance: InstanceId,
    pub scrollback: Entity,
}

/// The shell layer's node controller (minimal terminal).
pub struct ShellController;

impl NodeController for ShellController {
    fn title(&self) -> &str {
        "Terminal"
    }

    fn build(&self, commands: &mut Commands, body: Entity, instance: InstanceId, theme: &Theme) {
        // Header + session status.
        let header = commands
            .spawn(row(theme.metrics.space_sm))
            .with_children(|h| {
                h.spawn(text(theme, "Shell session:", theme.metrics.font_md, Role::TextMuted));
                h.spawn((
                    text(theme, "", theme.metrics.font_md, Role::Text),
                    bind_text(move || match query_shell_sessions(instance) {
                        Ok(sessions) if sessions.is_empty() => "No active sessions".into(),
                        Ok(sessions) => format!("{} session(s) active", sessions.len()),
                        Err(_) => "Unknown".into(),
                    }),
                ));
                h.spawn(button(theme, "Create"))
                    .observe(move |_: On<Activate>| info!("Shell: create session on {}", instance));
            })
            .id();

        // Scrollback area.
        let scrollback = commands
            .spawn((
                TerminalScrollback,
                Node {
                    flex_direction: FlexDirection::Column,
                    row_gap: Val::Px(theme.metrics.space_xs),
                    flex_grow: 1.0,
                    padding: UiRect::all(Val::Px(theme.metrics.space_sm)),
                    overflow: Overflow::clip(),
                    ..default()
                },
                BackgroundColor(Color::srgb_u8(18, 18, 22)),
            ))
            .id();
        commands.entity(scrollback).with_children(|s| {
            s.spawn(muted(theme, "Connected to local echo shell.", theme.metrics.font_sm));
        });

        // Prompt input.
        let prompt = commands
            .spawn((
                TerminalPrompt {
                    instance,
                    scrollback,
                },
                text_input(theme, "type a command and press Enter", false),
            ))
            .id();

        commands.entity(body).add_children(&[header, scrollback, prompt]);
    }
}

/// Append the submitted command (and a placeholder response) to the scrollback.
fn on_terminal_submit(
    submit: On<TextSubmit>,
    theme: Res<Theme>,
    mut prompts: Query<(&TerminalPrompt, &mut TextInput)>,
    mut commands: Commands,
) {
    let Ok((prompt, mut input)) = prompts.get_mut(submit.entity) else {
        return;
    };
    let cmd = std::mem::take(&mut input.value);
    if cmd.trim().is_empty() {
        return;
    }
    info!("Shell command on {}: {}", prompt.instance, cmd);
    let scrollback = prompt.scrollback;
    let echo = format!("$ {}", cmd);
    commands.entity(scrollback).with_children(|s| {
        s.spawn(text(&theme, echo, theme.metrics.font_sm, Role::Text));
        s.spawn(muted(
            &theme,
            "Command sent to remote shell (implementation pending)",
            theme.metrics.font_sm,
        ));
    });
}

/// The shell layer's client plugin.
pub struct ShellClientPlugin;

impl Plugin for ShellClientPlugin {
    fn build(&self, app: &mut App) {
        app.add_observer(on_terminal_submit).register_layer_client(
            LayerClientInfo::new(
                LayerName::from("Shell"),
                "Remote shell access and command execution",
            )
            .with_controller(ShellController)
            .with_visible_instance_types(&[InstanceType::Server, InstanceType::Agent]),
        );
    }
}
