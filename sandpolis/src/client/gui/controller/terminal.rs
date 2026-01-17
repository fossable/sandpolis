use bevy_egui::egui;
use egui_console::{ConsoleWindow, ConsoleBuilder, ConsoleEvent};
use sandpolis_core::InstanceId;
use crate::client::gui::queries;

/// Per-instance terminal state stored in egui memory
#[derive(serde::Serialize, serde::Deserialize, Default)]
pub struct TerminalState {
    pub session_id: Option<String>,
    #[serde(skip)]
    pub console: Option<ConsoleWindow>,
}

impl Clone for TerminalState {
    fn clone(&self) -> Self {
        Self {
            session_id: self.session_id.clone(),
            console: None, // ConsoleWindow doesn't implement Clone, so we skip it
        }
    }
}

/// Render terminal controller with egui_console
pub fn render(ui: &mut egui::Ui, instance_id: InstanceId) {
    let state_id = egui::Id::new(format!("terminal_{}", instance_id));

    let mut terminal_state = ui.data_mut(|d| {
        d.get_persisted::<TerminalState>(state_id)
            .unwrap_or_default()
    });

    // Session management header
    ui.horizontal(|ui| {
        ui.label("🖥️ Shell Session:");
        if let Ok(sessions) = queries::query_shell_sessions(instance_id) {
            if sessions.is_empty() {
                ui.label("No active sessions");
                if ui.button("➕ Create Session").clicked() {
                    // TODO: Create new shell session via WebSocket
                    tracing::info!("Creating new shell session for instance {}", instance_id);
                }
            } else {
                ui.label(format!("{} session(s) active", sessions.len()));
                if let Some(session_id) = &terminal_state.session_id {
                    ui.label(format!("Current: {}", session_id));
                }
            }
        }
    });

    ui.separator();

    // Initialize console if not present (can't be cloned/serialized)
    if terminal_state.console.is_none() {
        let console = ConsoleBuilder::new()
            .prompt("$ ")
            .history_size(100)
            .tab_quote_character('"')
            .build();

        terminal_state.console = Some(console);
    }

    // Render the console window
    if let Some(console) = &mut terminal_state.console {
        // Set up command completion (common shell commands)
        let commands = vec![
            "ls", "cd", "pwd", "cat", "echo", "mkdir", "rm", "cp", "mv",
            "grep", "find", "ps", "kill", "top", "df", "du", "wget", "curl",
            "tar", "zip", "unzip", "chmod", "chown", "nano", "vim", "clear",
            "exit", "help",
        ];

        let command_table = console.command_table_mut();
        command_table.clear();
        for cmd in commands {
            command_table.push(cmd.to_string());
        }

        // Draw console and handle events
        match console.draw(ui) {
            ConsoleEvent::Command(command) => {
                tracing::info!("Shell command entered: {}", command);
                // TODO: Send command to remote shell session via WebSocket
                // For now, write placeholder response
                console.write(&format!("> {}\n", command));
                console.write("Command sent to remote shell (implementation pending)\n");
            }
            ConsoleEvent::None => {
                // No command entered this frame
            }
        }
    }

    ui.separator();

    // Connection status and controls
    ui.horizontal(|ui| {
        ui.label("📡 Status:");
        if terminal_state.session_id.is_some() {
            ui.colored_label(egui::Color32::GREEN, "● Connected");

            if ui.button("🔌 Disconnect").clicked() {
                // TODO: Close WebSocket connection
                terminal_state.session_id = None;
                if let Some(console) = &mut terminal_state.console {
                    console.write("Disconnected from remote shell\n");
                }
            }
        } else {
            ui.colored_label(egui::Color32::GRAY, "● Disconnected");

            if ui.button("🔗 Connect").clicked() {
                // TODO: Establish WebSocket connection
                tracing::info!("Connecting to shell session for instance {}", instance_id);
                if let Some(console) = &mut terminal_state.console {
                    console.write("Connecting to remote shell...\n");
                }
            }
        }
    });

    // Persist state
    ui.data_mut(|d| d.insert_persisted(state_id, terminal_state));
}
