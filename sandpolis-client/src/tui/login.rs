//! Interactive login prompt shown before a CLI subcommand runs against a
//! server whose realm has user accounts. Also walks the first-login flow:
//! setting an initial password and enrolling TOTP when the realm requires it.

use crate::tui::EventHandler;
use crossterm::event::{Event, KeyCode, KeyEventKind, KeyModifiers};
use ratatui::buffer::Buffer;
use ratatui::layout::{Alignment, Constraint, Flex, Layout, Rect};
use ratatui::style::{Modifier, Style};
use ratatui::text::Line;
use ratatui::widgets::{Block, Borders, Clear, Paragraph, Widget, WidgetRef};
use sandpolis_server::ServerConnection;
use sandpolis_server::login::{LoginPassword, LoginRequest, LoginResponse};
use sandpolis_server::user::UserName;
use std::sync::{Arc, Mutex};

/// What the prompt was doing when it ended.
#[derive(Debug, Clone)]
pub enum LoginOutcome {
    /// The token is stored on the [`ServerConnection`]; this carries the user
    /// it belongs to so the caller can persist both.
    Success { username: UserName },
    Cancelled,
}

enum PromptMode {
    Credentials,
    PasswordSetup { totp_required: bool },
    TotpEnroll { otpauth_url: String },
}

struct PromptState {
    mode: PromptMode,
    username: String,
    password: String,
    confirm: String,
    totp: String,
    focused: usize,
    error: Option<String>,
    busy: bool,
    pending: Option<Result<LoginResponse, String>>,
    outcome: Option<LoginOutcome>,
}

impl PromptState {
    /// Editable fields in the current mode, as (label, masked).
    fn fields(&self) -> Vec<(&'static str, bool)> {
        match &self.mode {
            PromptMode::Credentials => {
                let mut fields = vec![("Username", false), ("Password", true)];
                fields.push(("One-time code (if enrolled)", false));
                fields
            }
            PromptMode::PasswordSetup { .. } => {
                vec![("New password", true), ("Confirm password", true)]
            }
            PromptMode::TotpEnroll { .. } => vec![("One-time code", false)],
        }
    }

    fn field_value_mut(&mut self, index: usize) -> &mut String {
        match &self.mode {
            PromptMode::Credentials => match index {
                0 => &mut self.username,
                1 => &mut self.password,
                _ => &mut self.totp,
            },
            PromptMode::PasswordSetup { .. } => match index {
                0 => &mut self.password,
                _ => &mut self.confirm,
            },
            PromptMode::TotpEnroll { .. } => &mut self.totp,
        }
    }

    fn field_value(&self, index: usize) -> &str {
        match &self.mode {
            PromptMode::Credentials => match index {
                0 => &self.username,
                1 => &self.password,
                _ => &self.totp,
            },
            PromptMode::PasswordSetup { .. } => match index {
                0 => &self.password,
                _ => &self.confirm,
            },
            PromptMode::TotpEnroll { .. } => &self.totp,
        }
    }
}

pub struct LoginPromptWidget {
    connection: ServerConnection,
    state: Arc<Mutex<PromptState>>,
}

impl LoginPromptWidget {
    /// `username` pre-fills the form, typically from the saved server entry.
    pub fn new(connection: ServerConnection, username: Option<UserName>) -> Self {
        Self {
            connection,
            state: Arc::new(Mutex::new(PromptState {
                mode: PromptMode::Credentials,
                username: username.map(|u| u.to_string()).unwrap_or_default(),
                password: String::new(),
                confirm: String::new(),
                totp: String::new(),
                focused: 0,
                error: None,
                busy: false,
                pending: None,
                outcome: None,
            })),
        }
    }

    /// Whether the prompt has finished (either way); passed to `run_tui_until`.
    pub fn finished(&self) -> bool {
        self.state.lock().unwrap().outcome.is_some()
    }

    /// How the prompt ended, once [`finished`](Self::finished).
    pub fn outcome(&self) -> Option<LoginOutcome> {
        self.state.lock().unwrap().outcome.clone()
    }

    fn submit(&self, state: &mut PromptState) {
        let username = match state.username.parse::<UserName>() {
            Ok(username) => username,
            Err(_) => {
                state.error = Some("Invalid username".to_string());
                return;
            }
        };

        let setup = match &state.mode {
            PromptMode::PasswordSetup { .. } => {
                if state.password.is_empty() {
                    state.error = Some("Password cannot be empty".to_string());
                    return;
                }
                if state.password != state.confirm {
                    state.error = Some("Passwords do not match".to_string());
                    return;
                }
                true
            }
            _ => false,
        };

        let request = LoginRequest {
            username,
            password: LoginPassword::new(self.connection.cluster_id, &state.password),
            setup,
            totp_token: (!state.totp.is_empty()).then(|| state.totp.clone()),
            lifetime: None,
        };

        state.busy = true;
        state.error = None;

        let connection = self.connection.clone();
        let shared = self.state.clone();
        tokio::spawn(async move {
            let result = connection.login(request).await.map_err(|e| e.to_string());
            shared.lock().unwrap().pending = Some(result);
        });
    }

    /// Apply a completed login attempt to the prompt.
    fn process_pending(&self, state: &mut PromptState) {
        let Some(result) = state.pending.take() else {
            return;
        };
        state.busy = false;

        match result {
            Ok(LoginResponse::Ok(_)) => {
                // The token is already stored on the connection by `login`.
                if let Ok(username) = state.username.parse() {
                    state.outcome = Some(LoginOutcome::Success { username });
                }
            }
            Ok(LoginResponse::PasswordSetupRequired { totp_required }) => {
                state.mode = PromptMode::PasswordSetup { totp_required };
                state.password.clear();
                state.confirm.clear();
                state.focused = 0;
            }
            Ok(LoginResponse::TotpSetupRequired { otpauth_url }) => {
                state.mode = PromptMode::TotpEnroll { otpauth_url };
                state.totp.clear();
                state.focused = 0;
            }
            Ok(LoginResponse::Denied) => {
                state.error = Some("Invalid username, password, or code".to_string());
            }
            Ok(LoginResponse::Expired) => {
                state.error = Some("Account has expired".to_string());
            }
            Ok(LoginResponse::Invalid) => {
                state.error = Some("Invalid login request".to_string());
            }
            Err(e) => {
                state.error = Some(e);
            }
        }
    }
}

impl WidgetRef for LoginPromptWidget {
    fn render_ref(&self, area: Rect, buf: &mut Buffer) {
        let mut state = self.state.lock().unwrap();
        self.process_pending(&mut state);

        let fields = state.fields();
        let extra = match &state.mode {
            PromptMode::TotpEnroll { .. } => 4,
            _ => 0,
        };
        let height = (fields.len() as u16) * 2 + 7 + extra;
        let [popup] = Layout::vertical([Constraint::Length(height)])
            .flex(Flex::Center)
            .areas(area);
        let [popup] = Layout::horizontal([Constraint::Length(64)])
            .flex(Flex::Center)
            .areas(popup);

        Clear.render(popup, buf);
        let block = Block::default()
            .title(format!(" Login — {} ", self.connection.url))
            .borders(Borders::ALL);
        let inner = block.inner(popup);
        block.render(popup, buf);

        let mut lines: Vec<Line> = Vec::new();

        match &state.mode {
            PromptMode::Credentials => {
                if let Some(message) = &self.connection.banner.message {
                    lines.push(Line::from(message.clone()));
                }
            }
            PromptMode::PasswordSetup { totp_required } => {
                lines.push(Line::from("First login: choose a password"));
                if *totp_required {
                    lines.push(Line::from("Two-factor enrollment follows"));
                }
            }
            PromptMode::TotpEnroll { otpauth_url } => {
                lines.push(Line::from("Add this secret to your authenticator app:"));
                lines.push(Line::from(otpauth_url.clone()));
                lines.push(Line::from(""));
            }
        }

        for (index, (label, masked)) in fields.iter().enumerate() {
            let value = state.field_value(index);
            let shown = if *masked {
                "*".repeat(value.len())
            } else {
                value.to_string()
            };
            let style = if index == state.focused {
                Style::default().add_modifier(Modifier::BOLD | Modifier::UNDERLINED)
            } else {
                Style::default()
            };
            lines.push(Line::styled(format!("{label}: {shown}"), style));
            lines.push(Line::from(""));
        }

        if state.busy {
            lines.push(Line::from("Logging in..."));
        } else if let Some(error) = &state.error {
            lines.push(Line::styled(
                error.clone(),
                Style::default().add_modifier(Modifier::REVERSED),
            ));
        } else {
            lines.push(Line::from(""));
        }
        lines.push(Line::from(
            "Tab: next field  Enter: submit  Esc: cancel",
        ));

        Paragraph::new(lines)
            .alignment(Alignment::Left)
            .render(inner, buf);
    }
}

impl EventHandler for LoginPromptWidget {
    fn handle_event(&mut self, event: Event) -> Option<Event> {
        let mut state = self.state.lock().unwrap();
        self.process_pending(&mut state);

        if let Event::Key(key) = &event
            && key.kind == KeyEventKind::Press
        {
            // Aborting must stay possible even while a login attempt is running.
            if key.code == KeyCode::Esc
                || (key.code == KeyCode::Char('c') && key.modifiers.contains(KeyModifiers::CONTROL))
            {
                state.outcome = Some(LoginOutcome::Cancelled);
                return None;
            }

            if state.busy {
                return None;
            }

            let field_count = state.fields().len();
            match key.code {
                KeyCode::Tab | KeyCode::Down => {
                    state.focused = (state.focused + 1) % field_count;
                }
                KeyCode::BackTab | KeyCode::Up => {
                    state.focused = (state.focused + field_count - 1) % field_count;
                }
                KeyCode::Enter => {
                    self.submit(&mut state);
                }
                KeyCode::Backspace => {
                    let focused = state.focused;
                    state.field_value_mut(focused).pop();
                }
                KeyCode::Char(ch) => {
                    let focused = state.focused;
                    state.field_value_mut(focused).push(ch);
                }
                _ => {}
            }
        }

        // Everything is consumed: the prompt owns the terminal until it ends,
        // and `q` has to be typeable in every field.
        None
    }
}
