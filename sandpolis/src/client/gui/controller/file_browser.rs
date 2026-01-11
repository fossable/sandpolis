use bevy_egui::egui;
use egui_file_dialog::{FileDialog, DialogState};
use sandpolis_core::InstanceId;
use crate::InstanceState;
use crate::client::gui::queries;
use std::path::PathBuf;

/// Per-instance file browser state stored in egui memory
#[derive(Clone, serde::Serialize, serde::Deserialize)]
pub struct FileBrowserState {
    pub current_path: String,
    pub selected_files: Vec<String>,
}

impl Default for FileBrowserState {
    fn default() -> Self {
        Self {
            current_path: "/".to_string(),
            selected_files: Vec::new(),
        }
    }
}

/// Render file browser controller using egui-file-dialog
pub fn render(ui: &mut egui::Ui, state: &InstanceState, instance_id: InstanceId) {
    let state_id = egui::Id::new(format!("file_browser_{}", instance_id));
    let dialog_id = egui::Id::new(format!("file_dialog_{}", instance_id));

    let mut browser_state = ui.data_mut(|d| {
        d.get_persisted::<FileBrowserState>(state_id)
            .unwrap_or_default()
    });

    // Initialize file dialog as a transient state (recreated each frame since FileDialog doesn't impl Clone)
    let mut file_dialog = FileDialog::new();

    // Path navigation bar
    ui.horizontal(|ui| {
        ui.label("Remote Path:");
        if ui.button("🏠").on_hover_text("Home").clicked() {
            browser_state.current_path = "/".to_string();
        }
        if ui.button("⬆").on_hover_text("Parent Directory").clicked() {
            let path = PathBuf::from(&browser_state.current_path);
            if let Some(parent) = path.parent() {
                browser_state.current_path = parent.display().to_string();
            }
        }
        ui.monospace(&browser_state.current_path);
    });

    ui.separator();

    // File list using native table
    ui.heading("Remote Files:");

    egui::ScrollArea::vertical()
        .max_height(200.0)
        .show(ui, |ui| {
            let path = PathBuf::from(&browser_state.current_path);
            match queries::query_directory_contents(state, instance_id, &path) {
                Ok(entries) => {
                    if entries.is_empty() {
                        ui.label("(Empty directory)");
                    } else {
                        // Table header
                        egui::Grid::new("file_grid")
                            .striped(true)
                            .spacing([10.0, 4.0])
                            .show(ui, |ui| {
                                ui.strong("Name");
                                ui.strong("Size");
                                ui.strong("Type");
                                ui.end_row();

                                for entry in entries {
                                    let icon = if entry.is_dir { "📁" } else { "📄" };
                                    let is_selected = browser_state.selected_files.contains(&entry.name);

                                    let label_text = format!("{} {}", icon, entry.name);
                                    let response = ui.selectable_label(is_selected, label_text);

                                    if response.clicked() {
                                        if entry.is_dir {
                                            // Navigate into directory
                                            let mut new_path = path.clone();
                                            new_path.push(&entry.name);
                                            browser_state.current_path = new_path.display().to_string();
                                            browser_state.selected_files.clear();
                                        } else {
                                            // Toggle file selection
                                            if is_selected {
                                                browser_state.selected_files.retain(|f| f != &entry.name);
                                            } else {
                                                browser_state.selected_files.push(entry.name.clone());
                                            }
                                        }
                                    }

                                    // File size
                                    if entry.is_dir {
                                        ui.label("-");
                                    } else {
                                        ui.label(format_size(entry.size));
                                    }

                                    // File type
                                    ui.label(if entry.is_dir { "Directory" } else { "File" });
                                    ui.end_row();
                                }
                            });
                    }
                }
                Err(_) => {
                    ui.colored_label(egui::Color32::RED, "Error loading directory contents");
                }
            }
        });

    ui.separator();

    // Action buttons
    ui.horizontal(|ui| {
        let has_selection = !browser_state.selected_files.is_empty();

        if ui.add_enabled(has_selection, egui::Button::new("📥 Download")).clicked() {
            // Open file dialog for download destination
            file_dialog.pick_directory();
        }

        if ui.button("📤 Upload").clicked() {
            // Open file dialog for upload source
            file_dialog.pick_file();
        }

        if ui.add_enabled(has_selection, egui::Button::new("🗑 Delete"))
            .on_hover_text("Delete selected files")
            .clicked()
        {
            // TODO: Implement delete functionality
            browser_state.selected_files.clear();
        }

        if ui.button("➕ New Folder").clicked() {
            // TODO: Implement create folder functionality
        }
    });

    // Update and handle file dialog
    file_dialog.update(ui.ctx());

    // Check if user selected a path
    if let Some(picked_path) = file_dialog.take_picked() {
        // Handle selected file/directory
        tracing::info!("Selected path for upload/download: {:?}", picked_path);
        // TODO: Implement actual upload/download logic based on context
    }

    ui.separator();

    // Filesystem usage stats
    if let Ok(usage) = queries::query_filesystem_usage(state, instance_id) {
        let used_gb = usage.used as f64 / 1_000_000_000.0;
        let total_gb = usage.total as f64 / 1_000_000_000.0;
        let percent = if usage.total > 0 {
            (usage.used as f64 / usage.total as f64) * 100.0
        } else {
            0.0
        };

        ui.label(format!("💾 Disk Usage: {:.1} GB / {:.1} GB ({:.1}%)", used_gb, total_gb, percent));

        let progress_bar = egui::ProgressBar::new(percent as f32 / 100.0)
            .show_percentage();
        ui.add(progress_bar);
    }

    // Persist state
    ui.data_mut(|d| d.insert_persisted(state_id, browser_state));
}

/// Format file size in human-readable form
fn format_size(bytes: u64) -> String {
    const KB: u64 = 1024;
    const MB: u64 = KB * 1024;
    const GB: u64 = MB * 1024;

    if bytes >= GB {
        format!("{:.2} GB", bytes as f64 / GB as f64)
    } else if bytes >= MB {
        format!("{:.2} MB", bytes as f64 / MB as f64)
    } else if bytes >= KB {
        format!("{:.2} KB", bytes as f64 / KB as f64)
    } else {
        format!("{} B", bytes)
    }
}
