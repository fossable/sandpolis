use bevy_egui::egui;
use sandpolis_core::InstanceId;
use crate::InstanceState;
use crate::client::gui::queries;

/// Per-instance terminal state stored in egui memory
#[derive(Default, Clone, serde::Serialize, serde::Deserialize)]
pub struct TerminalState {
    pub session_id: Option<String>,
    pub output_buffer: String,
    pub input_line: String,
}

/// Render terminal controller
pub fn render(ui: &mut egui::Ui, state: &InstanceState, instance_id: InstanceId) {
    let state_id = egui::Id::new(format!("terminal_{}", instance_id));

    let mut terminal_state = ui.data_mut(|d| {
        d.get_persisted::<TerminalState>(state_id)
            .unwrap_or_default()
    });

    ui.horizontal(|ui| {
        ui.label("Shell Session:");
        if let Ok(sessions) = queries::query_shell_sessions(state, instance_id) {
            if sessions.is_empty() {
                ui.label("No sessions");
                if ui.button("Create Session").clicked() {}
            }
        }
    });

    ui.separator();
    ui.label("Output:");
    egui::ScrollArea::vertical().max_height(250.0).show(ui, |ui| {
        ui.add(egui::TextEdit::multiline(&mut terminal_state.output_buffer.as_str())
            .desired_width(f32::INFINITY).font(egui::TextStyle::Monospace).interactive(false));
    });

    ui.separator();
    ui.horizontal(|ui| {
        ui.label("$");
        ui.add(egui::TextEdit::singleline(&mut terminal_state.input_line)
            .desired_width(f32::INFINITY).font(egui::TextStyle::Monospace));
        if ui.button("Send").clicked() {
            terminal_state.input_line.clear();
        }
    });

    ui.data_mut(|d| d.insert_persisted(state_id, terminal_state));
}
