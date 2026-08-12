//! GUI components for the Shell layer.
//!
//! Renders remote shell sessions through [`alacritty_terminal`], a headless VT
//! emulator. "Create" opens a [`ShellSessionStreamRequester`] to the agent via
//! [`InstanceConnection::open_stream_to`](sandpolis_instance); the agent runs
//! the shell on a real PTY. Output bytes are fed to an
//! [`alacritty_terminal::Term`] grid and rendered as a fixed grid of styled
//! text rows; keystrokes are translated to terminal input and sent back as
//! `Stdin`.

mod keys;
mod render;

use alacritty_terminal::Term;
use alacritty_terminal::event::{Event, EventListener};
use alacritty_terminal::grid::Dimensions;
use alacritty_terminal::vte::ansi::Processor;
use bevy::input_focus::tab_navigation::TabIndex;
use bevy::input_focus::{FocusCause, InputFocus};
use bevy::prelude::*;
use sandpolis_client::gui::ui::Activate;
use sandpolis_client::gui::controller::ControllerTarget;
use sandpolis_client::gui::ui::controller::{LayerClientInfo, NodeController, RegisterLayerClient};
use sandpolis_client::gui::ui::gating::WantsKeyboard;
use sandpolis_client::gui::ui::theme::{Role, Theme};
use sandpolis_client::gui::ui::widgets::{button, row, text};
use sandpolis_instance::network::stream::StreamMessage;
use sandpolis_instance::{InstanceId, InstanceType, LayerName};
use std::collections::HashMap;
use std::path::PathBuf;
use tokio::sync::mpsc::{Receiver, Sender, UnboundedReceiver, channel};

use crate::session::{ShellOutput, ShellSessionStreamRequest, ShellSessionStreamRequester};

/// Terminal font size, in logical pixels.
const FONT_SIZE: f32 = 13.0;
/// Advance width of one cell. JetBrains Mono advances 0.6em per glyph.
const CELL_W: f32 = FONT_SIZE * 0.6;
/// Height of one cell (line height).
const CELL_H: f32 = FONT_SIZE * 1.2;
/// Default terminal background.
const TERM_BG: Color = Color::srgb(0.08, 0.08, 0.10);

/// Handle to the embedded monospace font the terminal renders with.
#[derive(Resource)]
struct TerminalFont(Handle<Font>);

/// Active client-side shell sessions, keyed by the node they belong to.
///
/// The key is a [`ControllerTarget`] rather than a bare `InstanceId` because a
/// probe node borrows its gateway server's id; only the sub-node device id tells
/// an SSH probe apart from the server it orbits.
#[derive(Resource, Default)]
struct ShellStreams {
    sessions: HashMap<ControllerTarget, ShellStreamSession>,
}

/// A live shell session the GUI is rendering.
struct ShellStreamSession {
    /// Output chunks pushed by the requester (registered on the connection).
    output: UnboundedReceiver<ShellOutput>,
    /// Outbound requests (stdin, resize). A background task forwards these over
    /// the stream, translating to the SSH wire type for probe sessions.
    outbound: Sender<ShellSessionStreamRequest>,
    /// Headless terminal emulator fed by `output`.
    term: Term<EventProxy>,
    /// ANSI byte parser driving `term`.
    processor: Processor,
    /// The grid container entity this session renders into, if a controller
    /// panel is currently open. `None` while the panel is closed.
    grid: Option<Entity>,
    rows: u16,
    cols: u16,
    /// The grid changed and needs to be redrawn.
    dirty: bool,
    /// The stream ended (child exited or connection dropped).
    ended: bool,
}

/// Forwards terminal replies (cursor/device/color queries) back to the PTY.
#[derive(Clone)]
struct EventProxy(Sender<ShellSessionStreamRequest>);

impl EventListener for EventProxy {
    fn send_event(&self, event: Event) {
        if let Event::PtyWrite(text) = event {
            let _ = self
                .0
                .try_send(ShellSessionStreamRequest::Stdin {
                    data: text.into_bytes(),
                });
        }
    }
}

/// Grid dimensions passed to [`Term::new`] / [`Term::resize`].
#[derive(Clone, Copy)]
struct TermDimensions {
    columns: usize,
    screen_lines: usize,
}

impl Dimensions for TermDimensions {
    fn total_lines(&self) -> usize {
        self.screen_lines
    }
    fn screen_lines(&self) -> usize {
        self.screen_lines
    }
    fn columns(&self) -> usize {
        self.columns
    }
}

/// Marks the grid container of a terminal controller.
#[derive(Component)]
struct TerminalGrid {
    target: ControllerTarget,
}

/// Marks a grid that should open a session once its size is known.
#[derive(Component)]
struct TerminalPendingStart;

/// The shell layer's node controller (VT terminal).
pub struct ShellController;

impl NodeController for ShellController {
    fn title(&self) -> &str {
        "Terminal"
    }

    fn title_for(&self, target: ControllerTarget) -> String {
        #[cfg(feature = "probe")]
        if let Some(device) = ssh_probe_device(target) {
            return device.display_name();
        }
        #[cfg(not(feature = "probe"))]
        let _ = target;
        self.title().to_string()
    }

    fn build(
        &self,
        commands: &mut Commands,
        body: Entity,
        target: ControllerTarget,
        theme: &Theme,
    ) {
        // Grid container: captures keyboard focus and renders the terminal.
        let grid = commands
            .spawn((
                TerminalGrid { target },
                WantsKeyboard,
                TabIndex(0),
                Interaction::default(),
                Node {
                    flex_direction: FlexDirection::Column,
                    flex_grow: 1.0,
                    padding: UiRect::all(Val::Px(theme.metrics.space_xs)),
                    overflow: Overflow::clip(),
                    ..default()
                },
                BackgroundColor(TERM_BG),
            ))
            .id();

        // Clicking the grid focuses it so keystrokes route to the terminal.
        commands.entity(grid).observe(
            move |_: On<Pointer<Click>>, mut focus: ResMut<InputFocus>| {
                focus.set(grid, FocusCause::Navigated);
            },
        );

        // Header with a Create button.
        let header = commands
            .spawn(row(theme.metrics.space_sm))
            .with_children(|h| {
                h.spawn(text(theme, "Shell:", theme.metrics.font_md, Role::TextMuted));
                h.spawn(button(theme, "Create")).observe(
                    move |_: On<Activate>, mut commands: Commands, streams: Res<ShellStreams>| {
                        // Reattachment of an existing session is handled by
                        // `attach_terminal_grids`; only start a fresh one.
                        if streams.sessions.contains_key(&target) {
                            return;
                        }
                        commands.entity(grid).insert(TerminalPendingStart);
                    },
                );
            })
            .id();

        commands.entity(body).add_children(&[header, grid]);
    }
}

/// Load the embedded monospace font.
fn load_terminal_font(mut commands: Commands, asset_server: Res<AssetServer>) {
    commands.insert_resource(TerminalFont(
        asset_server.load("fonts/JetBrainsMono-Regular.ttf"),
    ));
}

/// Reattach a live session to a freshly (re)built grid entity, e.g. when the
/// controller panel is reopened.
fn attach_terminal_grids(
    grids: Query<(Entity, &TerminalGrid), Added<TerminalGrid>>,
    mut streams: ResMut<ShellStreams>,
) {
    for (entity, grid) in grids.iter() {
        if let Some(session) = streams.sessions.get_mut(&grid.target) {
            session.grid = Some(entity);
            session.dirty = true;
        }
    }
}

/// Open sessions for grids marked pending, once their laid-out size is known.
fn start_pending_sessions(
    mut commands: Commands,
    pending: Query<(Entity, &TerminalGrid, &ComputedNode), With<TerminalPendingStart>>,
    mut streams: ResMut<ShellStreams>,
) {
    for (entity, grid, node) in pending.iter() {
        let logical = node.size() * node.inverse_scale_factor();
        if logical.x < CELL_W || logical.y < CELL_H {
            continue; // not laid out yet
        }
        let cols = ((logical.x / CELL_W).floor() as u16).max(2);
        let rows = ((logical.y / CELL_H).floor() as u16).max(2);

        let (outbound, outbound_rx) = channel(64);

        let dims = TermDimensions {
            columns: cols as usize,
            screen_lines: rows as usize,
        };
        let term = Term::new(
            alacritty_terminal::term::Config::default(),
            &dims,
            EventProxy(outbound.clone()),
        );

        let output = open_session_stream(grid.target, rows, cols, outbound_rx);

        streams.sessions.insert(
            grid.target,
            ShellStreamSession {
                output,
                outbound,
                term,
                processor: Processor::new(),
                grid: Some(entity),
                rows,
                cols,
                dirty: true,
                ended: false,
            },
        );
        commands.entity(entity).remove::<TerminalPendingStart>();
        info!(
            "Shell session started for {} ({}x{})",
            grid.target.instance, cols, rows
        );
    }
}

/// The SSH probe device behind `target`, if there is one.
///
/// Reads the probe layer's device registry directly rather than going through
/// `sandpolis_probe::client::gui`, whose helpers are behind that crate's `client`
/// feature — depending on them would drag its whole GUI stack in here.
#[cfg(feature = "probe")]
fn ssh_probe_device(target: ControllerTarget) -> Option<sandpolis_probe::RegisteredDevice> {
    let device_id = target.sub?;
    let device = sandpolis_probe::REGISTERED_DEVICES
        .read()
        .ok()?
        .iter()
        .find(|device| device.id == device_id)?
        .clone();
    device.device.ssh.as_ref()?;
    Some(device)
}

/// Open the stream backing a new session and hand back the channel its output
/// arrives on.
///
/// A probe target gets an SSH session run by the device's owning server; anything
/// else gets a PTY on the agent itself. Both produce [`ShellOutput`], so the
/// terminal above this doesn't care which it got.
fn open_session_stream(
    target: ControllerTarget,
    rows: u16,
    cols: u16,
    outbound_rx: Receiver<ShellSessionStreamRequest>,
) -> UnboundedReceiver<ShellOutput> {
    #[cfg(feature = "probe")]
    if let Some(device) = ssh_probe_device(target) {
        let (requester, output) = crate::ssh::SshSessionStreamRequester::channel();
        let initial = crate::ssh::SshSessionStreamRequest::Start {
            device_id: device.id,
            rows: rows as u32,
            cols: cols as u32,
        };
        spawn_ssh_stream(&device, requester, initial, outbound_rx);
        return output;
    }

    let (requester, output) = ShellSessionStreamRequester::channel();
    let initial = ShellSessionStreamRequest::Start {
        path: PathBuf::from("/bin/sh"),
        environment: HashMap::new(),
        rows: rows as u32,
        cols: cols as u32,
    };
    spawn_shell_stream(target.instance, requester, initial, outbound_rx);
    output
}

/// Open an SSH session to a probe device and forward outbound requests over it
/// until the channel closes.
///
/// Probes are reachable only from servers, so this opens a stream *to the owning
/// server* (not a relayed one to an agent) and translates the terminal's
/// agent-shaped requests into the SSH wire type on the way out.
#[cfg(feature = "probe")]
fn spawn_ssh_stream(
    device: &sandpolis_probe::RegisteredDevice,
    requester: crate::ssh::SshSessionStreamRequester,
    initial: crate::ssh::SshSessionStreamRequest,
    mut outbound_rx: Receiver<ShellSessionStreamRequest>,
) {
    let conn = device
        .device
        .server
        .as_ref()
        .and_then(sandpolis_client::sync::connection_for)
        .or_else(sandpolis_client::sync::connection);
    let Some(conn) = conn else {
        warn!("No server connection; cannot start SSH session");
        return;
    };
    sandpolis_client::sync::spawn(async move {
        let (id, msg_tx) = match conn.open_stream(requester, initial).await {
            Ok(v) => v,
            Err(e) => {
                warn!(error = %e, "Failed to open SSH session");
                return;
            }
        };
        while let Some(req) = outbound_rx.recv().await {
            let req = match req {
                ShellSessionStreamRequest::Stdin { data } => {
                    crate::ssh::SshSessionStreamRequest::Stdin { data }
                }
                ShellSessionStreamRequest::Resize { rows, cols } => {
                    crate::ssh::SshSessionStreamRequest::Resize { rows, cols }
                }
                // Sent as the stream's initial request, never through here.
                ShellSessionStreamRequest::Start { .. } => continue,
            };
            let payload = match serde_cbor::to_vec(&req) {
                Ok(p) => p,
                Err(_) => continue,
            };
            if msg_tx.send(StreamMessage::local(id, payload)).await.is_err() {
                break;
            }
        }
        conn.close_stream(id);
    });
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
    sandpolis_client::sync::spawn(async move {
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
                .send(StreamMessage::to(id, payload, instance))
                .await
                .is_err()
            {
                break;
            }
        }
        conn.close_stream(id);
    });
}

/// Drain shell output into each session's terminal emulator.
fn drive_shell_streams(mut streams: ResMut<ShellStreams>) {
    use tokio::sync::mpsc::error::TryRecvError;

    for session in streams.sessions.values_mut() {
        let mut chunk = Vec::new();
        loop {
            match session.output.try_recv() {
                Ok(output) => {
                    chunk.extend_from_slice(&output.stdout);
                    chunk.extend_from_slice(&output.stderr);
                }
                Err(TryRecvError::Empty) => break,
                Err(TryRecvError::Disconnected) => {
                    session.ended = true;
                    break;
                }
            }
        }
        if !chunk.is_empty() {
            session.processor.advance(&mut session.term, &chunk);
            session.dirty = true;
        }
    }
}

/// Recompute grid size from the laid-out container and resize the terminal.
fn handle_terminal_resize(
    grids: Query<(&TerminalGrid, &ComputedNode), Changed<ComputedNode>>,
    mut streams: ResMut<ShellStreams>,
) {
    for (grid, node) in grids.iter() {
        let Some(session) = streams.sessions.get_mut(&grid.target) else {
            continue;
        };
        let logical = node.size() * node.inverse_scale_factor();
        if logical.x < CELL_W || logical.y < CELL_H {
            continue;
        }
        let cols = ((logical.x / CELL_W).floor() as u16).max(2);
        let rows = ((logical.y / CELL_H).floor() as u16).max(2);
        if cols == session.cols && rows == session.rows {
            continue;
        }
        session.cols = cols;
        session.rows = rows;
        session.term.resize(TermDimensions {
            columns: cols as usize,
            screen_lines: rows as usize,
        });
        let _ = session.outbound.try_send(ShellSessionStreamRequest::Resize {
            rows: rows as u32,
            cols: cols as u32,
        });
        session.dirty = true;
    }
}

/// The shell layer's client plugin.
pub struct ShellClientPlugin;

impl Plugin for ShellClientPlugin {
    fn build(&self, app: &mut App) {
        app.init_resource::<ShellStreams>()
            .add_systems(Startup, load_terminal_font)
            .add_systems(
                Update,
                (
                    attach_terminal_grids,
                    start_pending_sessions,
                    keys::terminal_keyboard_input,
                    drive_shell_streams,
                    handle_terminal_resize,
                    render::render_terminals,
                )
                    .chain(),
            )
            .register_layer_client(
                LayerClientInfo::new(
                    LayerName::from("Shell"),
                    "Remote shell access and command execution",
                )
                .with_controller(ShellController)
                .with_visible_instance_types(&[InstanceType::Server, InstanceType::Agent])
                // SSH probes get a terminal just like agents do. Devices that
                // expose nothing this layer can drive stay hidden.
                .showing_probe_nodes_for(&["SSH"]),
            );
    }
}
