//! GUI components for the Shell layer.
//!
//! Provides a minimal native terminal node controller (scrollback + single-line
//! prompt) wired to a real relayed shell session: "Create" opens a
//! [`ShellSessionStreamRequester`] to the agent via
//! [`InstanceConnection::open_stream_to`], stdout/stderr are drained into the
//! scrollback, and submitted lines are forwarded as `Stdin`. A full VT100/PTY
//! terminal is still deferred — output is appended as plain text.

use bevy::prelude::*;
use sandpolis_client::gui::ui::Activate;
use sandpolis_client::gui::ui::bind::bind_text;
use sandpolis_client::gui::ui::controller::{LayerClientInfo, NodeController, RegisterLayerClient};
use sandpolis_client::gui::ui::text_input::{TextInput, TextSubmit, text_input};
use sandpolis_client::gui::ui::theme::{Role, Theme};
use sandpolis_client::gui::ui::widgets::{button, muted, row, text};
use sandpolis_instance::network::stream::StreamMessage;
use sandpolis_instance::{InstanceId, InstanceType, LayerName};
use std::collections::HashMap;
use std::path::PathBuf;
use tokio::sync::mpsc::{Receiver, Sender, UnboundedReceiver, channel};

use crate::session::{ShellOutput, ShellSessionStreamRequest, ShellSessionStreamRequester};

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

/// Active client-side shell sessions, keyed by instance.
#[derive(Resource, Default)]
struct ShellStreams {
    sessions: HashMap<InstanceId, ShellStreamSession>,
}

/// A live shell session the GUI is rendering.
struct ShellStreamSession {
    /// Output chunks pushed by the requester (registered on the connection).
    output: UnboundedReceiver<ShellOutput>,
    /// Outbound requests (stdin, resize). A background task forwards these over
    /// the relayed stream to the agent.
    outbound: Sender<ShellSessionStreamRequest>,
    /// The scrollback container to append output into.
    scrollback: Entity,
}

/// Marks the scrollback container of a terminal controller.
#[derive(Component)]
pub struct TerminalScrollback {
    pub instance: InstanceId,
}

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
        // Scrollback area (built first so the controls can reference it).
        let scrollback = commands
            .spawn((
                TerminalScrollback { instance },
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
            s.spawn(muted(theme, "No active session.", theme.metrics.font_sm));
        });

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
                h.spawn(button(theme, "Create")).observe(
                    move |_: On<Activate>, mut streams: ResMut<ShellStreams>| {
                        if streams.sessions.contains_key(&instance) {
                            return;
                        }

                        let (requester, output) = ShellSessionStreamRequester::channel();
                        let (outbound, outbound_rx) = channel(64);

                        let initial = ShellSessionStreamRequest::Start {
                            path: PathBuf::from("/bin/sh"),
                            environment: HashMap::new(),
                            rows: 24,
                            cols: 80,
                        };
                        spawn_shell_stream(instance, requester, initial, outbound_rx);

                        streams.sessions.insert(
                            instance,
                            ShellStreamSession {
                                output,
                                outbound,
                                scrollback,
                            },
                        );
                        info!("Shell session requested for {}", instance);
                    },
                );
            })
            .id();

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

/// Open a relayed shell session to `instance` and forward outbound requests
/// (stdin, resize) over it until the channel closes.
fn spawn_shell_stream(
    instance: InstanceId,
    requester: ShellSessionStreamRequester,
    initial: ShellSessionStreamRequest,
    mut outbound_rx: Receiver<ShellSessionStreamRequest>,
) {
    let Some(conn) = sandpolis_client::sync::connection() else {
        warn!("No server connection; cannot start shell session");
        return;
    };
    tokio::spawn(async move {
        let (id, msg_tx) = match conn.open_stream_to(instance, requester, initial).await {
            Ok(v) => v,
            Err(e) => {
                warn!(error = %e, "Failed to open shell session");
                return;
            }
        };
        while let Some(req) = outbound_rx.recv().await {
            let payload = match serde_cbor::to_vec(&req) {
                Ok(p) => p,
                Err(_) => continue,
            };
            if msg_tx
                .send(StreamMessage {
                    stream_id: id,
                    payload,
                    dst: Some(instance),
                })
                .await
                .is_err()
            {
                break;
            }
        }
        conn.close_stream(id);
    });
}

/// Drain shell output and append it to each session's scrollback.
fn drive_shell_streams(
    mut streams: ResMut<ShellStreams>,
    theme: Res<Theme>,
    mut commands: Commands,
) {
    for session in streams.sessions.values_mut() {
        let mut chunk = Vec::new();
        while let Ok(output) = session.output.try_recv() {
            chunk.extend_from_slice(&output.stdout);
            chunk.extend_from_slice(&output.stderr);
        }
        if chunk.is_empty() {
            continue;
        }
        let rendered = String::from_utf8_lossy(&chunk).replace('\r', "");
        for line in rendered.split_inclusive('\n') {
            let line = line.trim_end_matches('\n').to_string();
            let scrollback = session.scrollback;
            let theme = &theme;
            commands.entity(scrollback).with_children(|s| {
                s.spawn(text(theme, line, theme.metrics.font_sm, Role::Text));
            });
        }
    }
}

/// Forward the submitted command to the active session as stdin.
fn on_terminal_submit(
    submit: On<TextSubmit>,
    theme: Res<Theme>,
    mut prompts: Query<(&TerminalPrompt, &mut TextInput)>,
    streams: Res<ShellStreams>,
    mut commands: Commands,
) {
    let Ok((prompt, mut input)) = prompts.get_mut(submit.entity) else {
        return;
    };
    let cmd = std::mem::take(&mut input.value);
    if cmd.trim().is_empty() {
        return;
    }

    let scrollback = prompt.scrollback;
    let echo = format!("$ {}", cmd);
    commands.entity(scrollback).with_children(|s| {
        s.spawn(text(&theme, echo, theme.metrics.font_sm, Role::Text));
    });

    match streams.sessions.get(&prompt.instance) {
        Some(session) => {
            let mut data = cmd.into_bytes();
            data.push(b'\n');
            let _ = session
                .outbound
                .try_send(ShellSessionStreamRequest::Stdin { data });
        }
        None => {
            commands.entity(scrollback).with_children(|s| {
                s.spawn(muted(
                    &theme,
                    "No active session — press Create first.",
                    theme.metrics.font_sm,
                ));
            });
        }
    }
}

/// The shell layer's client plugin.
pub struct ShellClientPlugin;

impl Plugin for ShellClientPlugin {
    fn build(&self, app: &mut App) {
        app.init_resource::<ShellStreams>()
            .add_systems(Update, drive_shell_streams)
            .add_observer(on_terminal_submit)
            .register_layer_client(
                LayerClientInfo::new(
                    LayerName::from("Shell"),
                    "Remote shell access and command execution",
                )
                .with_controller(ShellController)
                .with_visible_instance_types(&[InstanceType::Server, InstanceType::Agent]),
            );
    }
}
