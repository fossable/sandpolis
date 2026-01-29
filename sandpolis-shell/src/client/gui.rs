//! GUI components for the Shell layer.
//!
//! This module provides the terminal controller and layer-specific GUI elements.

use bevy::prelude::*;
use bevy_egui::egui;
use egui_console::{ConsoleBuilder, ConsoleEvent, ConsoleWindow};
use sandpolis_instance::{InstanceId, LayerName};

use sandpolis_client::gui::layer_ext::{ActivityTypeInfo, LayerGuiExtension};

/// Per-instance terminal state stored in egui memory.
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

/// Render terminal controller with egui_console.
pub fn render(ui: &mut egui::Ui, instance_id: InstanceId) {
    let state_id = egui::Id::new(format!("terminal_{}", instance_id));

    let mut terminal_state = ui.data_mut(|d| {
        d.get_persisted::<TerminalState>(state_id)
            .unwrap_or_default()
    });

    // Session management header
    ui.horizontal(|ui| {
        ui.label("Shell Session:");
        if let Ok(sessions) = query_shell_sessions(instance_id) {
            if sessions.is_empty() {
                ui.label("No active sessions");
                if ui.button("Create Session").clicked() {
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
            "ls", "cd", "pwd", "cat", "echo", "mkdir", "rm", "cp", "mv", "grep", "find", "ps",
            "kill", "top", "df", "du", "wget", "curl", "tar", "zip", "unzip", "chmod", "chown",
            "nano", "vim", "clear", "exit", "help",
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
        ui.label("Status:");
        if terminal_state.session_id.is_some() {
            ui.colored_label(egui::Color32::GREEN, "Connected");

            if ui.button("Disconnect").clicked() {
                // TODO: Close WebSocket connection
                terminal_state.session_id = None;
                if let Some(console) = &mut terminal_state.console {
                    console.write("Disconnected from remote shell\n");
                }
            }
        } else {
            ui.colored_label(egui::Color32::GRAY, "Disconnected");

            if ui.button("Connect").clicked() {
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

/// Shell layer GUI extension.
pub struct ShellGuiExtension;

impl LayerGuiExtension for ShellGuiExtension {
    fn layer(&self) -> &LayerName {
        static LAYER: std::sync::LazyLock<LayerName> =
            std::sync::LazyLock::new(|| LayerName::from("Shell"));
        &LAYER
    }

    fn description(&self) -> &'static str {
        "Remote shell access and command execution"
    }

    fn render_controller(&self, ui: &mut egui::Ui, instance_id: InstanceId) {
        render(ui, instance_id);
    }

    fn controller_name(&self) -> &'static str {
        "Terminal"
    }

    fn get_node_svg(&self, _instance_id: InstanceId) -> &'static str {
        "shell/Terminal.svg"
    }

    fn get_node_color(&self, instance_id: InstanceId) -> Color {
        // Color based on session status
        if let Ok(sessions) = query_shell_sessions(instance_id) {
            if sessions.is_empty() {
                Color::WHITE
            } else {
                Color::srgb(0.7, 1.0, 0.7) // Green for active session
            }
        } else {
            Color::WHITE
        }
    }

    fn preview_icon(&self) -> &'static str {
        "Terminal"
    }

    fn preview_details(&self, instance_id: InstanceId) -> String {
        if let Ok(sessions) = query_shell_sessions(instance_id) {
            if sessions.is_empty() {
                "No active sessions".to_string()
            } else {
                format!("{} active session(s)", sessions.len())
            }
        } else {
            "Shell status unknown".to_string()
        }
    }

    fn edge_color(&self) -> Color {
        Color::srgb(0.8, 0.8, 0.3) // Yellow
    }

    fn activity_types(&self) -> Vec<ActivityTypeInfo> {
        vec![ActivityTypeInfo {
            id: "shell_command",
            name: "Shell Command",
            color: Color::srgb(0.8, 0.8, 0.3),
            size: 6.0,
        }]
    }

    fn visible_instance_types(&self) -> &'static [sandpolis_instance::InstanceType] {
        // Shell layer shows servers and agents (not clients)
        &[
            sandpolis_instance::InstanceType::Server,
            sandpolis_instance::InstanceType::Agent,
        ]
    }
}

/// Static instance of the shell GUI extension.
static SHELL_GUI_EXT: ShellGuiExtension = ShellGuiExtension;

// Register the extension with inventory
inventory::submit! {
    &SHELL_GUI_EXT as &dyn LayerGuiExtension
}
