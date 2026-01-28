use crate::ShellLayer;
use ratatui::buffer::Buffer;
use ratatui::crossterm::event::{Event, KeyCode, KeyEventKind, KeyModifiers};
use ratatui::layout::{Constraint, Direction, Layout, Rect};
use ratatui::style::{Color, Style};
use ratatui::text::{Line, Span, Text};
use ratatui::widgets::{Block, Borders, Clear, Paragraph, Widget, WidgetRef};
use sandpolis_client::tui::EventHandler;
use sandpolis_instance::InstanceId;
use std::path::PathBuf;
use tui_term::widget::PseudoTerminal;

pub struct ShellTerminalWidget {
    pub shell: ShellLayer,
    pub parser: tui_term::vt100::Parser,
    pub shell_path: Option<PathBuf>,
    pub connected: bool,
    pub input_buffer: String,
    pub status_message: String,
}

impl ShellTerminalWidget {
    pub fn new(_instance: InstanceId, shell: ShellLayer) -> Self {
        let mut parser = tui_term::vt100::Parser::new(24, 80, 0);
        // Initialize with a welcome message
        parser.process(b"Shell Terminal - Press Enter to connect\r\n");
        parser.process(b"Use Ctrl+C to disconnect\r\n");

        Self {
            shell,
            parser,
            shell_path: None,
            connected: false,
            input_buffer: String::new(),
            status_message: "Ready to connect".to_string(),
        }
    }

    pub fn set_shell_path(&mut self, path: PathBuf) {
        self.status_message = format!("Shell: {}", path.display());
        self.shell_path = Some(path);
    }

    pub fn connect(&mut self) {
        if let Some(shell_path) = &self.shell_path {
            self.connected = true;
            self.status_message = format!("Connected to {}", shell_path.display());

            // Write welcome message to parser
            let welcome = format!("Connected to shell: {}\r\n", shell_path.display());
            self.parser.process(welcome.as_bytes());
            self.parser.process(b"$ "); // Simple prompt
        } else {
            self.status_message = "No shell path set".to_string();
        }
    }

    pub fn disconnect(&mut self) {
        self.connected = false;
        self.status_message = "Disconnected".to_string();
        self.parser.process(b"\r\nDisconnected from shell\r\n");
    }

    pub fn send_input(&mut self, input: &str) {
        if self.connected {
            // Echo the input to the parser
            self.parser.process(input.as_bytes());

            if input.ends_with('\r') || input.ends_with('\n') {
                // Process the command here - in a real implementation,
                // this would send the command to the actual shell process
                self.parser.process(b"\r\n");

                // Simple command simulation
                let command = input.trim();
                match command {
                    "exit" | "quit" => {
                        self.disconnect();
                        return;
                    }
                    "clear" => {
                        // Clear the screen by creating new parser
                        self.parser = tui_term::vt100::Parser::new(24, 80, 0);
                        self.parser.process(b"$ ");
                        return;
                    }
                    _ => {
                        // Simulate command output
                        let output = format!("Command executed: {}\r\n", command);
                        self.parser.process(output.as_bytes());
                    }
                }

                // Show prompt again
                self.parser.process(b"$ ");
            }
        }
    }

    pub fn resize(&mut self, width: u16, height: u16) {
        // Create new parser with new dimensions
        let mut new_parser = tui_term::vt100::Parser::new(height, width, 0);
        // Copy current screen content if needed
        if self.connected {
            new_parser.process(b"$ ");
        } else {
            new_parser.process(b"Shell Terminal - Press Enter to connect\r\n");
            new_parser.process(b"Use Ctrl+C to disconnect\r\n");
        }
        self.parser = new_parser;
    }
}

impl WidgetRef for ShellTerminalWidget {
    fn render_ref(&self, area: Rect, buf: &mut Buffer) {
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
        let terminal_widget = PseudoTerminal::new(self.parser.screen());
        Widget::render(terminal_widget, terminal_inner, buf);

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
                            // Send the current input buffer and clear it
                            let input = format!("{}\r", self.input_buffer);
                            self.send_input(&input);
                            self.input_buffer.clear();
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
                            // Send character immediately for real-time feedback
                            self.send_input(&c.to_string());
                        }
                        return None;
                    }
                    KeyCode::Backspace => {
                        if self.connected && !self.input_buffer.is_empty() {
                            self.input_buffer.pop();
                            // Send backspace sequence
                            self.parser.process(b"\x08 \x08");
                        }
                        return None;
                    }
                    KeyCode::Tab => {
                        if self.connected {
                            // Tab completion placeholder
                            self.input_buffer.push('\t');
                            self.send_input("\t");
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
