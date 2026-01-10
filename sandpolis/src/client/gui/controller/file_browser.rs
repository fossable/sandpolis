use bevy_egui::egui;
use sandpolis_core::InstanceId;
use crate::InstanceState;
use crate::client::gui::queries;
use std::path::PathBuf;

/// Per-instance file browser state stored in egui memory
#[derive(Default, Clone, serde::Serialize, serde::Deserialize)]
pub struct FileBrowserState {
    pub current_path: String,
    pub selected_file: Option<String>,
}

/// Render file browser controller
pub fn render(ui: &mut egui::Ui, state: &InstanceState, instance_id: InstanceId) {
    let state_id = egui::Id::new(format!("file_browser_{}", instance_id));

    let mut browser_state = ui.data_mut(|d| {
        d.get_persisted::<FileBrowserState>(state_id)
            .unwrap_or_else(|| FileBrowserState {
                current_path: "/".to_string(),
                selected_file: None,
            })
    });

    // Path navigation bar
    ui.horizontal(|ui| {
        ui.label("Path:");
        if ui.button("🏠").on_hover_text("Home").clicked() {
            browser_state.current_path = "/".to_string();
        }
        if ui.button("⬆").on_hover_text("Parent Directory").clicked() {
            let path = PathBuf::from(&browser_state.current_path);
            if let Some(parent) = path.parent() {
                browser_state.current_path = parent.display().to_string();
            }
        }
        ui.label(&browser_state.current_path);
    });

    ui.separator();

    // File list
    ui.label("Files and Directories:");

    egui::ScrollArea::vertical()
        .max_height(250.0)
        .show(ui, |ui| {
            let path = PathBuf::from(&browser_state.current_path);
            match queries::query_directory_contents(state, instance_id, &path) {
                Ok(entries) => {
                    if entries.is_empty() {
                        ui.label("(Empty directory)");
                    } else {
                        for entry in entries {
                            let icon = if entry.is_dir { "📁" } else { "📄" };
                            let label_text = format!("{} {}", icon, entry.name);

                            let is_selected = browser_state.selected_file.as_ref() == Some(&entry.name);
                            let response = ui.selectable_label(is_selected, label_text);

                            if response.clicked() {
                                if entry.is_dir {
                                    // Navigate into directory
                                    let mut new_path = path.clone();
                                    new_path.push(&entry.name);
                                    browser_state.current_path = new_path.display().to_string();
                                    browser_state.selected_file = None;
                                } else {
                                    // Select file
                                    browser_state.selected_file = Some(entry.name.clone());
                                }
                            }

                            // Show file size
                            if !entry.is_dir {
                                ui.label(format!("  Size: {} bytes", entry.size));
                            }
                        }
                    }
                }
                Err(_) => {
                    ui.label("Error loading directory contents");
                }
            }
        });

    ui.separator();

    // Action buttons
    ui.horizontal(|ui| {
        let has_selection = browser_state.selected_file.is_some();

        if ui.add_enabled(has_selection, egui::Button::new("Download")).clicked() {
            // TODO: Trigger download
        }

        if ui.button("Upload").clicked() {
            // TODO: Trigger upload
        }

        if ui.add_enabled(has_selection, egui::Button::new("Delete")).clicked() {
            browser_state.selected_file = None;
        }
    });

    // Filesystem usage stats
    ui.separator();
    if let Ok(usage) = queries::query_filesystem_usage(state, instance_id) {
        let used_gb = usage.used as f64 / 1_000_000_000.0;
        let total_gb = usage.total as f64 / 1_000_000_000.0;
        let percent = if usage.total > 0 {
            (usage.used as f64 / usage.total as f64) * 100.0
        } else {
            0.0
        };

        ui.label(format!("Disk Usage: {:.1} GB / {:.1} GB ({:.1}%)", used_gb, total_gb, percent));
        ui.add(egui::ProgressBar::new(percent as f32 / 100.0));
    }

    // Persist state
    ui.data_mut(|d| d.insert_persisted(state_id, browser_state));
}
