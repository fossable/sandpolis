// TODO rename server_selection
use anyhow::Result;
use ratatui::{
    buffer::Buffer,
    crossterm::event::{Event, KeyCode, KeyEventKind},
    layout::{Constraint, Layout, Rect},
    style::{Style, Stylize},
    text::{Line, Text},
    widgets::{
        Block, Borders, List, ListItem, ListState, Paragraph, StatefulWidget, Widget, WidgetRef,
    },
};
use ratatui_image::{StatefulImage, protocol::StatefulProtocol};
use sandpolis_client::tui::{EventHandler, Panel, help::HelpWidget, loading::LoadingWidget};
use sandpolis_core::UserName;
use sandpolis_network::ServerUrl;
use sandpolis_server::{ServerLayer, client::LoadServerBanner};
use sandpolis_user::{ClientAuthToken, LoginPassword, messages::LoginRequest};
use std::{
    ops::Deref,
    sync::{Arc, RwLock},
    time::Duration,
};
use tokio::{
    sync::{broadcast, mpsc::Receiver},
    time::sleep,
};
use tracing::debug;
use tui_popup::{Popup, SizedWidgetRef};
use validator::Validate;

use super::GRAPHICS;

pub struct ServerListWidget {
    state: Arc<RwLock<ServerListWidgetState>>,
    focused: bool,
    server_layer: ServerLayer,
}

struct ServerListWidgetState {
    list_state: ListState,
    list_items: Vec<Arc<ServerListItem>>,
    default_banner_image: Option<StatefulProtocol>,
    mode: ServerListWidgetMode,
    add_server_widget: AddServerWidget,
}

enum ServerListWidgetMode {
    Normal,
    Adding,
    /// While we're trying to login
    AddingLogin,
    Selecting,
}

impl ServerListWidget {
    pub fn new(server_layer: ServerLayer) -> Self {
        let mut list_items = server_layer
            .servers
            .iter()
            .map(|server_data| {
                Arc::new({
                    let server_layer = server_layer.clone();
                    let address = server_data.read().address.clone();
                    ServerListItem {
                        banner: RwLock::new(LoadServerBanner::Loading),
                        fetch_banner: RwLock::new(server_layer.fetch_banner(&address)),
                        ping: RwLock::new(None),
                        address,
                    }
                })
            })
            .collect::<Vec<Arc<ServerListItem>>>();

        // Add local server if one exists
        #[cfg(feature = "server")]
        {
            list_items.push(Arc::new(ServerListItem {
                address: "127.0.0.1:8768".parse().unwrap(),
                banner: RwLock::new(LoadServerBanner::Loaded(server_layer.banner.read().clone())),
                // The banner is already loaded, so this channel will never be used
                fetch_banner: RwLock::new(tokio::sync::mpsc::channel(1).1),
                ping: RwLock::new(Some(0)),
            }));
        }

        let state = Arc::new(RwLock::new(ServerListWidgetState {
            list_items,
            list_state: ListState::default(),
            default_banner_image: GRAPHICS.as_ref().map(|picker| {
                picker.new_resize_protocol(
                    image::ImageReader::open("/home/cilki/Downloads/sandpolis-256.png")
                        .unwrap()
                        .decode()
                        .unwrap(),
                )
            }),
            mode: ServerListWidgetMode::Normal,
            add_server_widget: AddServerWidget::default(),
        }));

        Self {
            state,
            focused: true,
            server_layer,
        }
    }
}

impl WidgetRef for ServerListWidget {
    fn render_ref(&self, area: Rect, buf: &mut Buffer) {
        let mut state = self.state.write().unwrap();

        let help_widget = HelpWidget {
            keybindings: vec![
                (KeyCode::Char('a'), "Add a new server".to_string()),
                (KeyCode::Char('X'), "Remove server".to_string()),
                (KeyCode::Right, "Login".to_string()),
            ],
        };
        let block = Block::default()
            .borders(Borders::ALL)
            .title("Server Selection");

        let [banner_area, list_area, help_area] = Layout::vertical([
            Constraint::Percentage(30),
            Constraint::Percentage(70),
            Constraint::Length(help_widget.height()),
        ])
        .areas(block.inner(area));

        block.render(area, buf);

        // Render banner
        StatefulWidget::render(
            StatefulImage::default(),
            banner_area,
            buf,
            state.default_banner_image.as_mut().unwrap(),
        );

        // Render list
        StatefulWidget::render(
            state
                .list_items
                .iter()
                .map(|i| i.deref())
                .collect::<List>()
                .highlight_style(Style::new().white()),
            list_area,
            buf,
            &mut state.list_state,
        );

        // Render help
        if self.focused {
            help_widget.render(help_area, buf);
        }

        match state.mode {
            // Render dialog if we're in "add" mode
            ServerListWidgetMode::Adding => {
                let popup = Popup::new(state.add_server_widget.clone());
                popup.render(area, buf);
            }
            // Render loading dialog during login
            ServerListWidgetMode::AddingLogin => {
                let loading_widget = LoadingWidget::new("Connecting to server...");
                let popup = Popup::new(loading_widget);
                popup.render(area, buf);
            }
            _ => (),
        }
    }
}

struct ServerListItem {
    address: ServerUrl,
    banner: RwLock<LoadServerBanner>,
    fetch_banner: RwLock<Receiver<LoadServerBanner>>,
    ping: RwLock<Option<u32>>,
}

impl ServerListItem {
    async fn run(&self) {
        loop {
            match *self.banner.read().unwrap() {
                LoadServerBanner::Loading => {
                    if let Some(progress) = self.fetch_banner.write().unwrap().recv().await {
                        *self.banner.write().unwrap() = progress;
                    }
                }
                LoadServerBanner::Loaded(_) => {
                    sleep(Duration::from_secs(3)).await;
                    // TODO ping
                }
                LoadServerBanner::Inaccessible => {
                    sleep(Duration::from_secs(3)).await;
                    // TODO try again
                }
                _ => {}
            }
        }
    }
}

impl From<&ServerListItem> for ListItem<'_> {
    fn from(item: &ServerListItem) -> Self {
        let mut text = Text::default();

        // Add server name
        text.extend([format!("{}", item.address).blue()]);

        // Add online status
        match &*item.banner.read().unwrap() {
            LoadServerBanner::Loading => text.extend([format!("Loading").gray()]),
            LoadServerBanner::Loaded(server_banner_data) => {
                text.extend([format!("Online").green()])
            }
            LoadServerBanner::Inaccessible => text.extend([format!("Offline")]),
            LoadServerBanner::Failed(error) => text.extend([format!("Error")]),
        }
        ListItem::new(text)
    }
}

impl EventHandler for ServerListWidget {
    fn handle_event(&mut self, event: &Event) {
        if let Event::Key(key) = event {
            if key.kind == KeyEventKind::Press {
                let mut state = self.state.write().unwrap();
                match state.mode {
                    ServerListWidgetMode::Normal => match key.code {
                        KeyCode::Char('j') | KeyCode::Down => {
                            state.list_state.select_next();
                        }
                        KeyCode::Char('k') | KeyCode::Up => {
                            state.list_state.select_previous();
                        }
                        KeyCode::Char('a') => {
                            state.mode = ServerListWidgetMode::Adding;
                            debug!("Entering add server mode");
                        }
                        _ => {}
                    },
                    ServerListWidgetMode::AddingLogin => match key.code {
                        KeyCode::Esc => {
                            state.mode = ServerListWidgetMode::Normal;
                            state.add_server_widget = AddServerWidget::default(); // Reset form
                            debug!("Exiting add server mode");
                        }
                        _ => {}
                    },
                    ServerListWidgetMode::Adding => match key.code {
                        KeyCode::Esc => {
                            state.mode = ServerListWidgetMode::Normal;
                            state.add_server_widget = AddServerWidget::default(); // Reset form
                            debug!("Exiting add server mode");
                        }
                        KeyCode::Tab => {
                            state.add_server_widget.next_field();
                        }
                        KeyCode::BackTab => {
                            state.add_server_widget.prev_field();
                        }
                        KeyCode::Enter => {
                            if let Ok(form_data) = state.add_server_widget.get_form_data() {
                                state.mode = ServerListWidgetMode::AddingLogin;

                                let state = self.state.clone();
                                tokio::spawn(async move {
                                    state.write().unwrap().add_server_widget =
                                        AddServerWidget::default(); // Reset login form

                                    // TODO get cluster id and server id

                                    self.server_layer.login(
                                        todo!(),
                                        LoginRequest {
                                            username: form_data.username,
                                            password: LoginPassword::new(
                                                todo!(),
                                                &form_data.password,
                                            ),
                                            totp_token: form_data.totp,
                                            lifetime: Some(Duration::new(1, 0)),
                                        },
                                    );
                                });
                            }
                        }
                        KeyCode::Backspace => {
                            state.add_server_widget.handle_backspace();
                        }
                        KeyCode::Delete => {
                            state.add_server_widget.handle_delete();
                        }
                        KeyCode::Left => {
                            state.add_server_widget.move_cursor_left();
                        }
                        KeyCode::Right => {
                            state.add_server_widget.move_cursor_right();
                        }
                        KeyCode::Char(ch) => {
                            state.add_server_widget.handle_char_input(ch);
                        }
                        _ => {}
                    },
                    _ => todo!(),
                }
            }
        }
    }
}

impl Panel for ServerListWidget {
    fn set_focus(&mut self, focused: bool) {
        self.focused = focused;
    }
}

#[derive(Debug, Clone, Default)]
struct AddServerWidget {
    server_url: String,
    username: String,
    password: String,
    totp: String,
    focused_field: FormField,
    cursor_position: usize,
}

#[derive(Debug, Clone, Copy, PartialEq, Default)]
enum FormField {
    #[default]
    ServerUrl,
    Username,
    Password,
    Totp,
}

impl AddServerWidget {
    fn next_field(&mut self) {
        self.focused_field = match self.focused_field {
            FormField::ServerUrl => FormField::Username,
            FormField::Username => FormField::Password,
            FormField::Password => FormField::Totp,
            FormField::Totp => FormField::ServerUrl,
        };
        self.update_cursor_position();
    }

    fn prev_field(&mut self) {
        self.focused_field = match self.focused_field {
            FormField::ServerUrl => FormField::Totp,
            FormField::Username => FormField::ServerUrl,
            FormField::Password => FormField::Username,
            FormField::Totp => FormField::Password,
        };
        self.update_cursor_position();
    }

    fn update_cursor_position(&mut self) {
        self.cursor_position = match self.focused_field {
            FormField::ServerUrl => self.server_url.len(),
            FormField::Username => self.username.len(),
            FormField::Password => self.password.len(),
            FormField::Totp => self.totp.len(),
        };
    }

    fn current_field_value(&self) -> &str {
        match self.focused_field {
            FormField::ServerUrl => &self.server_url,
            FormField::Username => &self.username,
            FormField::Password => &self.password,
            FormField::Totp => &self.totp,
        }
    }

    fn current_field_value_mut(&mut self) -> &mut String {
        match self.focused_field {
            FormField::ServerUrl => &mut self.server_url,
            FormField::Username => &mut self.username,
            FormField::Password => &mut self.password,
            FormField::Totp => &mut self.totp,
        }
    }

    fn handle_char_input(&mut self, ch: char) {
        // Apply input filtering for TOTP field
        if self.focused_field == FormField::Totp {
            // Only allow numeric characters
            if !ch.is_ascii_digit() {
                return;
            }
            // Limit to 6 characters
            if self.totp.len() >= 6 {
                return;
            }
        }

        let cursor_pos = self.cursor_position;
        let field = self.current_field_value_mut();
        field.insert(cursor_pos, ch);
        self.cursor_position += 1;
    }

    fn handle_backspace(&mut self) {
        if self.cursor_position > 0 {
            let cursor_pos = self.cursor_position;
            let field = self.current_field_value_mut();
            field.remove(cursor_pos - 1);
            self.cursor_position -= 1;
        }
    }

    fn handle_delete(&mut self) {
        let cursor_pos = self.cursor_position;
        let field = self.current_field_value_mut();
        if cursor_pos < field.len() {
            field.remove(cursor_pos);
        }
    }

    fn move_cursor_left(&mut self) {
        if self.cursor_position > 0 {
            self.cursor_position -= 1;
        }
    }

    fn move_cursor_right(&mut self) {
        let field_len = self.current_field_value().len();
        if self.cursor_position < field_len {
            self.cursor_position += 1;
        }
    }

    fn is_valid(&self) -> bool {
        !self.server_url.trim().is_empty()
            && !self.username.trim().is_empty()
            && !self.password.trim().is_empty()
            && self.validate_server_url().is_ok()
            && self.validate_totp().is_ok()
    }

    fn validate_server_url(&self) -> Result<ServerUrl, String> {
        let url_str = self.server_url.trim();
        if url_str.is_empty() {
            return Err("Server URL cannot be empty".to_string());
        }

        // Try to parse as ServerUrl
        match url_str.parse::<ServerUrl>() {
            Ok(url) => Ok(url),
            Err(_) => {
                // If parsing fails, try to add default scheme and port
                let url_with_defaults = if !url_str.contains("://") {
                    format!("https://{}", url_str)
                } else {
                    url_str.to_string()
                };

                match url_with_defaults.parse::<ServerUrl>() {
                    Ok(url) => Ok(url),
                    Err(_) => Err("Invalid server URL format".to_string()),
                }
            }
        }
    }

    fn validate_totp(&self) -> Result<(), String> {
        let totp_str = self.totp.trim();
        if totp_str.is_empty() {
            return Ok(()); // TOTP is optional
        }

        if totp_str.len() != 6 {
            return Err("TOTP must be exactly 6 digits".to_string());
        }

        if !totp_str.chars().all(|c| c.is_ascii_digit()) {
            return Err("TOTP must contain only numeric characters".to_string());
        }

        Ok(())
    }

    fn get_validation_errors(&self) -> Vec<String> {
        let mut errors = Vec::new();

        if self.server_url.trim().is_empty() {
            errors.push("Server URL is required".to_string());
        } else if let Err(err) = self.validate_server_url() {
            errors.push(err);
        }

        if self.username.trim().is_empty() {
            errors.push("Username is required".to_string());
        }

        if self.password.trim().is_empty() {
            errors.push("Password is required".to_string());
        }

        if let Err(err) = self.validate_totp() {
            errors.push(err);
        }

        errors
    }

    fn get_form_data(&self) -> Result<ServerFormData> {
        let data = ServerFormData {
            server_url: self.server_url.parse()?,
            username: self.username.parse()?,
            password: self.password.clone(),
            totp: if self.totp.is_empty() {
                None
            } else {
                Some(self.totp.to_string())
            },
        };
        data.validate()?;

        Ok(data)
    }
}

#[derive(Debug, Clone, Validate)]
struct ServerFormData {
    password: String,
    server_url: ServerUrl,
    totp: Option<String>,
    username: UserName,
}

impl SizedWidgetRef for AddServerWidget {
    fn width(&self) -> usize {
        60
    }

    fn height(&self) -> usize {
        12
    }
}

impl WidgetRef for AddServerWidget {
    fn render_ref(&self, area: Rect, buf: &mut Buffer) {
        let block = Block::default()
            .title("Add Server")
            .borders(Borders::ALL)
            .style(Style::default().white());

        let inner = block.inner(area);
        block.render(area, buf);

        // Layout: title + 4 form fields + submit/cancel instructions
        let chunks = Layout::vertical([
            Constraint::Length(2), // Server URL
            Constraint::Length(2), // Username
            Constraint::Length(2), // Password
            Constraint::Length(2), // TOTP (optional)
            Constraint::Length(1), // Spacing
            Constraint::Length(1), // Instructions
        ])
        .split(inner);

        // Render Server URL field
        self.render_field(
            "Server URL",
            &self.server_url,
            FormField::ServerUrl,
            chunks[0],
            buf,
        );

        // Render Username field
        self.render_field(
            "Username",
            &self.username,
            FormField::Username,
            chunks[1],
            buf,
        );

        // Render Password field
        self.render_password_field(chunks[2], buf);

        // Render TOTP field
        self.render_field(
            "TOTP (6 digits)",
            &self.totp,
            FormField::Totp,
            chunks[3],
            buf,
        );

        // Render instructions or validation errors
        let errors = self.get_validation_errors();
        let text = if self.is_valid() {
            Line::from("Enter: Connect  •  Esc: Cancel  •  Tab: Next Field").gray()
        } else if !errors.is_empty() {
            Line::from(format!("Errors: {}", errors.join(", "))).red()
        } else {
            Line::from("Tab: Next Field  •  Esc: Cancel  •  (Enter server URL, username, and password to connect)").gray()
        };

        Paragraph::new(text).render(chunks[5], buf);
    }
}

impl AddServerWidget {
    fn render_field(
        &self,
        label: &str,
        value: &str,
        field_type: FormField,
        area: Rect,
        buf: &mut Buffer,
    ) {
        let is_focused = self.focused_field == field_type;
        let style = if is_focused {
            Style::default().white().on_blue()
        } else {
            Style::default().gray()
        };

        // Create display value with cursor if focused
        let display_value = if is_focused {
            let mut chars: Vec<char> = value.chars().collect();
            if self.cursor_position <= chars.len() {
                chars.insert(self.cursor_position, '│');
            }
            chars.into_iter().collect()
        } else {
            value.to_string()
        };

        let chunks = Layout::horizontal([
            Constraint::Length(label.len() as u16 + 2),
            Constraint::Min(1),
        ])
        .split(area);

        // Render label
        Paragraph::new(format!("{}: ", label))
            .style(Style::default().white())
            .render(chunks[0], buf);

        // Render input field
        Paragraph::new(display_value)
            .style(style)
            .render(chunks[1], buf);
    }

    fn render_password_field(&self, area: Rect, buf: &mut Buffer) {
        let is_focused = self.focused_field == FormField::Password;
        let style = if is_focused {
            Style::default().white().on_blue()
        } else {
            Style::default().gray()
        };

        // Mask password with asterisks
        let masked_password = if is_focused {
            let mut masked: Vec<char> = self.password.chars().map(|_| '*').collect();
            if self.cursor_position <= masked.len() {
                masked.insert(self.cursor_position, '│');
            }
            masked.into_iter().collect()
        } else {
            "*".repeat(self.password.len())
        };

        let chunks = Layout::horizontal([
            Constraint::Length(11), // "Password: ".len()
            Constraint::Min(1),
        ])
        .split(area);

        // Render label
        Paragraph::new("Password: ")
            .style(Style::default().white())
            .render(chunks[0], buf);

        // Render masked input field
        Paragraph::new(masked_password)
            .style(style)
            .render(chunks[1], buf);
    }
}
