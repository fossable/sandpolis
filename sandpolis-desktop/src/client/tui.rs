//! Terminal viewer for the Desktop layer.
//!
//! Mirrors the shell layer's `ShellTerminalWidget`: on connect it opens a relayed
//! stream to the agent, decodes incoming frames (via the shared
//! [`DesktopStreamRequester`]) and renders the latest one with `ratatui_image`
//! halfblocks. Typed characters are forwarded to the remote as input events.
//!
//! `run_tui` does not enable mouse capture, so pointer input is not forwarded
//! here (the GUI viewer covers full pointer control).

use image::Rgba;
use ratatui::buffer::Buffer;
use ratatui::crossterm::event::{Event, KeyCode, KeyEventKind, KeyModifiers};
use ratatui::layout::{Constraint, Direction, Layout, Rect};
use ratatui::style::{Color, Style};
use ratatui::text::{Line, Span, Text};
use ratatui::widgets::{Block, Borders, Paragraph, StatefulWidget, Widget, WidgetRef};
use ratatui_image::{
    StatefulImage,
    protocol::{StatefulProtocol, StatefulProtocolType, halfblocks::Halfblocks},
};
use sandpolis_client::tui::EventHandler;
use sandpolis_instance::InstanceId;
use sandpolis_instance::network::stream::StreamMessage;
use std::sync::Mutex;
use std::time::Instant;
use tokio::sync::mpsc::{Receiver, Sender, UnboundedReceiver, channel};
use tracing::warn;

use crate::session::{
    DesktopFrame, DesktopStreamColorMode, DesktopStreamCompressionMode, DesktopStreamEvent,
    DesktopStreamInputEvent, DesktopStreamRequest, DesktopStreamRequester,
};

/// Render-time state mutated during the `&self` render pass.
struct RenderState {
    /// Terminal image protocol state for the most recent frame.
    image_state: Option<StatefulProtocol>,
    fps: u32,
    frame_count: u32,
    last_fps_update: Instant,
}

/// A terminal desktop viewer for a single agent.
pub struct DesktopViewerWidget {
    instance: InstanceId,
    connected: bool,
    status_message: String,
    /// Terminal cell size used to build the image protocol.
    font_size: ratatui_image::FontSize,
    /// Decoded frames/state from the relayed stream, drained each render.
    events: Mutex<Option<UnboundedReceiver<DesktopStreamEvent>>>,
    /// Remote display dimensions, once known.
    size: Mutex<Option<(u32, u32)>>,
    render: Mutex<RenderState>,
    /// Outbound input/Stop to the agent; dropping it closes the stream.
    outbound: Option<Sender<DesktopStreamRequest>>,
}

impl DesktopViewerWidget {
    pub fn new(instance: InstanceId) -> Self {
        // Query the terminal's cell size for halfblock rendering; fall back to a
        // common 8x16 if the query fails.
        let font_size = ratatui_image::picker::Picker::from_query_stdio()
            .map(|picker| picker.font_size())
            .unwrap_or_else(|_| (8u16, 16u16).into());

        Self {
            instance,
            connected: false,
            status_message: "Press Space to connect".to_string(),
            font_size,
            events: Mutex::new(None),
            size: Mutex::new(None),
            render: Mutex::new(RenderState {
                image_state: None,
                fps: 0,
                frame_count: 0,
                last_fps_update: Instant::now(),
            }),
            outbound: None,
        }
    }

    fn connect(&mut self) {
        if self.connected {
            return;
        }

        let (requester, events) = DesktopStreamRequester::channel();
        let (outbound, outbound_rx) = channel(64);
        let initial = DesktopStreamRequest::Start {
            desktop_uuid: String::new(),
            color_mode: DesktopStreamColorMode::Rgb888,
            compression_mode: DesktopStreamCompressionMode::Zstd,
        };
        spawn_desktop_stream(self.instance, requester, initial, outbound_rx);

        *self.events.lock().unwrap() = Some(events);
        self.outbound = Some(outbound);
        self.connected = true;
        self.status_message = "Connecting…".to_string();
    }

    fn disconnect(&mut self) {
        if let Some(tx) = &self.outbound {
            let _ = tx.try_send(DesktopStreamRequest::Stop);
        }
        self.outbound = None;
        *self.events.lock().unwrap() = None;
        *self.size.lock().unwrap() = None;
        self.render.lock().unwrap().image_state = None;
        self.connected = false;
        self.status_message = "Disconnected".to_string();
    }

    /// Forward an input event to the remote desktop.
    fn send_input(&self, event: DesktopStreamInputEvent) {
        if let Some(tx) = &self.outbound {
            let _ = tx.try_send(DesktopStreamRequest::Input(event));
        }
    }
}

/// A typed-character input event with no other fields set.
fn input_key_typed(c: char) -> DesktopStreamInputEvent {
    DesktopStreamInputEvent {
        key_pressed: None,
        key_released: None,
        key_typed: Some(c),
        pointer_pressed: None,
        pointer_released: None,
        pointer_x: None,
        pointer_y: None,
        clipboard: None,
    }
}

/// Map a key code to the character it types, if any.
fn key_char(code: KeyCode) -> Option<char> {
    match code {
        KeyCode::Char(c) => Some(c),
        KeyCode::Enter => Some('\n'),
        KeyCode::Tab => Some('\t'),
        _ => None,
    }
}

impl WidgetRef for DesktopViewerWidget {
    fn render_ref(&self, area: Rect, buf: &mut Buffer) {
        // Drain decoded events, coalescing to the most recent frame.
        if let Ok(mut guard) = self.events.lock() {
            if let Some(rx) = guard.as_mut() {
                let mut latest: Option<DesktopFrame> = None;
                while let Ok(event) = rx.try_recv() {
                    match event {
                        DesktopStreamEvent::Started { width, height } => {
                            *self.size.lock().unwrap() = Some((width, height));
                        }
                        DesktopStreamEvent::Frame(frame) => latest = Some(frame),
                        DesktopStreamEvent::Stopped => {}
                    }
                }

                if let Some(frame) = latest {
                    if frame.width > 0 && frame.height > 0 {
                        *self.size.lock().unwrap() = Some((frame.width, frame.height));
                        if let Some(img) =
                            image::RgbaImage::from_raw(frame.width, frame.height, frame.rgba)
                        {
                            let mut r = self.render.lock().unwrap();
                            r.frame_count += 1;
                            let now = Instant::now();
                            if now.duration_since(r.last_fps_update).as_secs() >= 1 {
                                r.fps = r.frame_count;
                                r.frame_count = 0;
                                r.last_fps_update = now;
                            }
                            r.image_state = Some(StatefulProtocol::new(
                                image::DynamicImage::ImageRgba8(img),
                                self.font_size,
                                Some(Rgba([0, 0, 0, 255])),
                                StatefulProtocolType::Halfblocks(Halfblocks::default()),
                            ));
                        }
                    }
                }
            }
        }

        let chunks = Layout::default()
            .direction(Direction::Vertical)
            .constraints([Constraint::Min(3), Constraint::Length(3)])
            .split(area);

        let viewer_block = Block::default()
            .title("Desktop Viewer")
            .borders(Borders::ALL)
            .border_style(if self.connected {
                Style::default().fg(Color::Green)
            } else {
                Style::default().fg(Color::Red)
            });
        let viewer_inner = viewer_block.inner(chunks[0]);
        viewer_block.render(chunks[0], buf);

        {
            let mut r = self.render.lock().unwrap();
            if let Some(state) = r.image_state.as_mut() {
                StatefulWidget::render(StatefulImage::default(), viewer_inner, buf, state);
            } else {
                let hint = if self.connected {
                    "Waiting for frames…"
                } else {
                    "Press Space to connect"
                };
                Paragraph::new(hint).render(viewer_inner, buf);
            }
        }

        // Status bar: stream state plus controls.
        let state_line = match (self.connected, *self.size.lock().unwrap()) {
            (true, Some((w, h))) => {
                format!("Streaming {}×{} @ {} fps", w, h, self.render.lock().unwrap().fps)
            }
            (true, None) => "Connecting…".to_string(),
            (false, _) => self.status_message.clone(),
        };
        let controls = if self.connected {
            "Ctrl+C=Disconnect, keys forwarded to remote"
        } else {
            "Space=Connect, q=Quit"
        };
        let status = Paragraph::new(Text::from(vec![
            Line::from(vec![
                Span::styled("Status: ", Style::default().fg(Color::Yellow)),
                Span::raw(state_line),
            ]),
            Line::from(vec![
                Span::styled("Controls: ", Style::default().fg(Color::Cyan)),
                Span::raw(controls),
            ]),
        ]))
        .block(
            Block::default()
                .title("Status")
                .borders(Borders::ALL)
                .border_style(Style::default().fg(Color::Blue)),
        );
        status.render(chunks[1], buf);
    }
}

impl EventHandler for DesktopViewerWidget {
    fn handle_event(&mut self, event: Event) -> Option<Event> {
        if let Event::Key(key) = event {
            if key.kind == KeyEventKind::Press {
                // Ctrl+C disconnects; if already disconnected, let it bubble to quit.
                if key.code == KeyCode::Char('c') && key.modifiers.contains(KeyModifiers::CONTROL) {
                    if self.connected {
                        self.disconnect();
                        return None;
                    }
                    return Some(event);
                }

                if !self.connected {
                    if matches!(key.code, KeyCode::Char(' ') | KeyCode::Enter) {
                        self.connect();
                        return None;
                    }
                } else if let Some(c) = key_char(key.code) {
                    // Connected: forward typed characters to the remote desktop.
                    self.send_input(input_key_typed(c));
                    return None;
                }
            }
        }
        // Unconsumed: let `run_tui` handle quit keys (q / Ctrl+C when idle).
        Some(event)
    }
}

/// Open a relayed desktop stream to `instance` and forward outbound requests
/// (input, Stop) over it until the channel closes.
fn spawn_desktop_stream(
    instance: InstanceId,
    requester: DesktopStreamRequester,
    initial: DesktopStreamRequest,
    mut outbound_rx: Receiver<DesktopStreamRequest>,
) {
    let Some(conn) = sandpolis_client::sync::connection() else {
        warn!("No server connection; cannot start desktop stream");
        return;
    };
    sandpolis_client::sync::spawn(async move {
        let (id, msg_tx) = match conn.open_stream_to(instance, requester, initial).await {
            Ok(v) => v,
            Err(e) => {
                warn!(error = %e, "Failed to open desktop stream");
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
