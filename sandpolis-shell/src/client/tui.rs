use crate::ShellLayer;
use crate::session::{ShellOutput, ShellSessionStreamRequest, ShellSessionStreamRequester};
use ratatui::buffer::Buffer;
use ratatui::crossterm::event::{Event, KeyCode, KeyEventKind, KeyModifiers};
use ratatui::layout::{Constraint, Direction, Layout, Rect};
use ratatui::style::{Color, Style};
use ratatui::text::{Line, Span, Text};
use ratatui::widgets::{Block, Borders, Clear, Paragraph, Widget, WidgetRef};
use sandpolis_client::tui::EventHandler;
use sandpolis_instance::InstanceId;
use sandpolis_instance::network::stream::StreamMessage;
use std::collections::HashMap;
use std::path::PathBuf;
use std::sync::Mutex;
use tokio::sync::mpsc::{Receiver, Sender, UnboundedReceiver, channel};
use tracing::warn;
use tui_term::widget::PseudoTerminal;

pub struct ShellTerminalWidget {
    pub shell: ShellLayer,
    /// Target agent this terminal connects to.
    instance: InstanceId,
    /// VT100 screen state. Behind a `Mutex` so async output can be drained
    /// during the `&self` render pass.
    parser: Mutex<tui_term::vt100::Parser>,
    pub shell_path: Option<PathBuf>,
    pub connected: bool,
    pub input_buffer: String,
    pub status_message: String,
    /// stdout/stderr produced by the relayed session, drained each render.
    output: Mutex<Option<UnboundedReceiver<ShellOutput>>>,
    /// Outbound stdin/resize to the agent; dropping it closes the stream.
    outbound: Option<Sender<ShellSessionStreamRequest>>,
}

impl ShellTerminalWidget {
    pub fn new(instance: InstanceId, shell: ShellLayer) -> Self {
        let mut parser = tui_term::vt100::Parser::new(24, 80, 0);
        // Initialize with a welcome message
        parser.process(b"Shell Terminal - Press Enter to connect\r\n");
        parser.process(b"Use Ctrl+C to disconnect\r\n");

        Self {
            shell,
            instance,
            parser: Mutex::new(parser),
            shell_path: None,
            connected: false,
            input_buffer: String::new(),
            status_message: "Ready to connect".to_string(),
            output: Mutex::new(None),
            outbound: None,
        }
    }

    pub fn set_shell_path(&mut self, path: PathBuf) {
        self.status_message = format!("Shell: {}", path.display());
        self.shell_path = Some(path);
    }

    pub fn connect(&mut self) {
        if self.connected {
            return;
        }

        let path = self
            .shell_path
            .clone()
            .unwrap_or_else(|| PathBuf::from("/bin/sh"));

        let (rows, cols) = {
            let parser = self.parser.lock().unwrap();
            parser.screen().size()
        };

        let (requester, output) = ShellSessionStreamRequester::channel();
        let (outbound, outbound_rx) = channel(64);

        let initial = ShellSessionStreamRequest::Start {
            path: path.clone(),
            environment: HashMap::new(),
            rows: rows as u32,
            cols: cols as u32,
        };
        spawn_shell_stream(self.instance, requester, initial, outbound_rx);

        *self.output.lock().unwrap() = Some(output);
        self.outbound = Some(outbound);
        self.connected = true;
        self.status_message = format!("Connected to {}", path.display());

        let welcome = format!("Connected to shell: {}\r\n", path.display());
        self.parser.lock().unwrap().process(welcome.as_bytes());
    }

    pub fn disconnect(&mut self) {
        self.connected = false;
        // Dropping the sender closes the relayed stream's outbound channel.
        self.outbound = None;
        *self.output.lock().unwrap() = None;
        self.status_message = "Disconnected".to_string();
        self.parser
            .lock()
            .unwrap()
            .process(b"\r\nDisconnected from shell\r\n");
    }

    /// Forward raw bytes to the agent's shell as stdin.
    fn send_stdin(&mut self, data: Vec<u8>) {
        if let Some(tx) = &self.outbound {
            let _ = tx.try_send(ShellSessionStreamRequest::Stdin { data });
        }
    }

    pub fn resize(&mut self, width: u16, height: u16) {
        *self.parser.lock().unwrap() = tui_term::vt100::Parser::new(height, width, 0);
        if let Some(tx) = &self.outbound {
            let _ = tx.try_send(ShellSessionStreamRequest::Resize {
                rows: height as u32,
                cols: width as u32,
            });
        }
    }
}

impl WidgetRef for ShellTerminalWidget {
    fn render_ref(&self, area: Rect, buf: &mut Buffer) {
        // Drain any output produced by the relayed session into the parser.
        if let Ok(mut output) = self.output.lock() {
            if let Some(rx) = output.as_mut() {
                while let Ok(chunk) = rx.try_recv() {
                    let mut parser = self.parser.lock().unwrap();
                    parser.process(&chunk.stdout);
                    parser.process(&chunk.stderr);
                }
            }
        }

        // Split the area into terminal and status bar
        let chunks = Layout::default()
            .direction(Direction::Vertical)
            .constraints([
                Constraint::Min(3),    // Terminal area
                Constraint::Length(3), // Status bar
            ])
            .split(area);

        // Render the terminal block
        let terminal_block = Block::default()
            .title("Shell Terminal")
            .borders(Borders::ALL)
            .border_style(if self.connected {
                Style::default().fg(Color::Green)
            } else {
                Style::default().fg(Color::Red)
            });

        // Calculate inner area for terminal content
        let terminal_inner = terminal_block.inner(chunks[0]);
        terminal_block.render(chunks[0], buf);

        // Render the terminal content
        {
            let parser = self.parser.lock().unwrap();
            let terminal_widget = PseudoTerminal::new(parser.screen());
            Widget::render(terminal_widget, terminal_inner, buf);
        }

        // Render status bar
        let status_text = vec![
            Line::from(vec![
                Span::styled("Status: ", Style::default().fg(Color::Yellow)),
                Span::raw(&self.status_message),
            ]),
            Line::from(vec![
                Span::styled("Controls: ", Style::default().fg(Color::Cyan)),
                Span::raw("Enter=Connect, Ctrl+C=Disconnect, Ctrl+Q=Quit"),
            ]),
        ];

        let status_paragraph = Paragraph::new(Text::from(status_text)).block(
            Block::default()
                .title("Status")
                .borders(Borders::ALL)
                .border_style(Style::default().fg(Color::Blue)),
        );

        status_paragraph.render(chunks[1], buf);
    }
}

impl EventHandler for ShellTerminalWidget {
    fn handle_event(&mut self, event: Event) -> Option<Event> {
        if let Event::Key(key) = event {
            if key.kind == KeyEventKind::Press {
                match key.code {
                    KeyCode::Enter => {
                        if !self.connected {
                            self.connect();
                        } else {
                            // Forward the buffered line as stdin (pipes don't
                            // echo, so echo locally first), then clear it.
                            let mut data = std::mem::take(&mut self.input_buffer).into_bytes();
                            data.push(b'\n');
                            self.parser.lock().unwrap().process(b"\r\n");
                            self.send_stdin(data);
                        }
                        return None;
                    }
                    KeyCode::Char('c') if key.modifiers.contains(KeyModifiers::CONTROL) => {
                        if self.connected {
                            self.disconnect();
                        }
                        return None;
                    }
                    KeyCode::Char('q') if key.modifiers.contains(KeyModifiers::CONTROL) => {
                        // This should be handled by the parent application
                        return None;
                    }
                    KeyCode::Char(c) => {
                        if self.connected {
                            self.input_buffer.push(c);
                            // Local echo for real-time feedback.
                            self.parser.lock().unwrap().process(c.to_string().as_bytes());
                        }
                        return None;
                    }
                    KeyCode::Backspace => {
                        if self.connected && !self.input_buffer.is_empty() {
                            self.input_buffer.pop();
                            // Erase one character on screen.
                            self.parser.lock().unwrap().process(b"\x08 \x08");
                        }
                        return None;
                    }
                    KeyCode::Tab => {
                        if self.connected {
                            self.input_buffer.push('\t');
                            self.parser.lock().unwrap().process(b"\t");
                        }
                        return None;
                    }
                    _ => {}
                }
            }
        } else if let Event::Resize(width, height) = event {
            // Handle terminal resize
            self.resize(width, height);
        }
        Some(event)
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

/// Shell selector widget for choosing which shell to use
pub struct ShellSelectorWidget {
    pub shells: Vec<crate::DiscoveredShell>,
    pub selected: usize,
    pub show_selector: bool,
}

impl ShellSelectorWidget {
    pub fn new() -> Self {
        Self {
            shells: Vec::new(),
            selected: 0,
            show_selector: false,
        }
    }

    pub fn set_shells(&mut self, shells: Vec<crate::DiscoveredShell>) {
        self.shells = shells;
        self.selected = 0;
    }

    pub fn show(&mut self) {
        self.show_selector = true;
    }

    pub fn hide(&mut self) {
        self.show_selector = false;
    }

    pub fn selected_shell(&self) -> Option<&crate::DiscoveredShell> {
        self.shells.get(self.selected)
    }

    pub fn select_next(&mut self) {
        if !self.shells.is_empty() {
            self.selected = (self.selected + 1) % self.shells.len();
        }
    }

    pub fn select_previous(&mut self) {
        if !self.shells.is_empty() {
            self.selected = if self.selected == 0 {
                self.shells.len() - 1
            } else {
                self.selected - 1
            };
        }
    }
}

impl WidgetRef for ShellSelectorWidget {
    fn render_ref(&self, area: Rect, buf: &mut Buffer) {
        if !self.show_selector {
            return;
        }

        // Create a centered popup
        let popup_area = centered_rect(60, 50, area);

        // Clear the area
        Clear.render(popup_area, buf);

        // Create shell list
        let mut lines = vec![
            Line::from(vec![Span::styled(
                "Select Shell:",
                Style::default().fg(Color::Yellow),
            )]),
            Line::from(""),
        ];

        for (i, shell) in self.shells.iter().enumerate() {
            let style = if i == self.selected {
                Style::default().bg(Color::Blue).fg(Color::White)
            } else {
                Style::default()
            };

            let version_info = shell.version.as_deref().unwrap_or("Unknown version");
            lines.push(Line::from(vec![Span::styled(
                format!(
                    "  {:?} - {} ({})",
                    shell.shell_type,
                    shell.location.display(),
                    version_info
                ),
                style,
            )]));
        }

        lines.push(Line::from(""));
        lines.push(Line::from(vec![
            Span::styled("Controls: ", Style::default().fg(Color::Cyan)),
            Span::raw("Up/Down Navigate, Enter Select, Esc Cancel"),
        ]));

        let paragraph = Paragraph::new(Text::from(lines)).block(
            Block::default()
                .title("Shell Selection")
                .borders(Borders::ALL)
                .border_style(Style::default().fg(Color::Green)),
        );

        paragraph.render(popup_area, buf);
    }
}

impl EventHandler for ShellSelectorWidget {
    fn handle_event(&mut self, event: Event) -> Option<Event> {
        if !self.show_selector {
            return Some(event);
        }

        if let Event::Key(key) = event {
            if key.kind == KeyEventKind::Press {
                match key.code {
                    KeyCode::Up | KeyCode::Char('k') => {
                        self.select_previous();
                        return None;
                    }
                    KeyCode::Down | KeyCode::Char('j') => {
                        self.select_next();
                        return None;
                    }
                    KeyCode::Enter => {
                        self.hide();
                        return None;
                    }
                    KeyCode::Esc => {
                        self.hide();
                        return None;
                    }
                    _ => {}
                }
            }
        }
        Some(event)
    }
}

// Helper function to create a centered rectangle
fn centered_rect(percent_x: u16, percent_y: u16, r: Rect) -> Rect {
    let popup_layout = Layout::default()
        .direction(Direction::Vertical)
        .constraints([
            Constraint::Percentage((100 - percent_y) / 2),
            Constraint::Percentage(percent_y),
            Constraint::Percentage((100 - percent_y) / 2),
        ])
        .split(r);

    Layout::default()
        .direction(Direction::Horizontal)
        .constraints([
            Constraint::Percentage((100 - percent_x) / 2),
            Constraint::Percentage(percent_x),
            Constraint::Percentage((100 - percent_x) / 2),
        ])
        .split(popup_layout[1])[1]
}
