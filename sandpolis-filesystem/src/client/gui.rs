//! GUI components for the Filesystem layer.
//!
//! This module provides the file browser controller and layer-specific GUI elements.

use bevy::prelude::*;
use bevy_egui::egui;
use egui_file_dialog::FileDialog;
use sandpolis_instance::{InstanceId, LayerName};
use std::path::PathBuf;

use sandpolis_client::gui::layer_ext::{ActivityTypeInfo, LayerGuiExtension};

/// Per-instance file browser state stored in egui memory.
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

/// Filesystem usage statistics.
#[derive(Clone, Debug, Default)]
pub struct FilesystemUsage {
    pub total: u64,
    pub used: u64,
    pub free: u64,
}

/// File/directory entry.
#[derive(Clone, Debug)]
pub struct FileEntry {
    pub name: String,
    pub is_dir: bool,
    pub size: u64,
}

/// Query filesystem usage for an instance.
pub fn query_filesystem_usage(_id: InstanceId) -> anyhow::Result<FilesystemUsage> {
    // TODO: Query from filesystem resident
    Ok(FilesystemUsage::default())
}

/// Query directory contents.
pub fn query_directory_contents(
    _id: InstanceId,
    _path: &std::path::Path,
) -> anyhow::Result<Vec<FileEntry>> {
    // TODO: Query from filesystem resident
    Ok(vec![])
}

/// Format file size in human-readable form.
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

/// Render file browser controller using egui-file-dialog.
pub fn render(ui: &mut egui::Ui, instance_id: InstanceId) {
    let state_id = egui::Id::new(format!("file_browser_{}", instance_id));

    let mut browser_state = ui.data_mut(|d| {
        d.get_persisted::<FileBrowserState>(state_id)
            .unwrap_or_default()
    });

    // Initialize file dialog as a transient state (recreated each frame since FileDialog doesn't impl Clone)
    let mut file_dialog = FileDialog::new();

    // Path navigation bar
    ui.horizontal(|ui| {
        ui.label("Remote Path:");
        if ui.button("Home").on_hover_text("Home").clicked() {
            browser_state.current_path = "/".to_string();
        }
        if ui.button("Up").on_hover_text("Parent Directory").clicked() {
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
            match query_directory_contents(instance_id, &path) {
                Ok(entries) => {
                    if entries.is_empty() {
                        ui.label("(Empty directory)");
                    } else {
                        // Table header
                        egui::Grid::new(egui::Id::new("file_grid").with(instance_id))
                            .striped(true)
                            .spacing([10.0, 4.0])
                            .show(ui, |ui| {
                                ui.strong("Name");
                                ui.strong("Size");
                                ui.strong("Type");
                                ui.end_row();

                                for entry in entries {
                                    let icon = if entry.is_dir { "D" } else { "F" };
                                    let is_selected =
                                        browser_state.selected_files.contains(&entry.name);

                                    let label_text = format!("{} {}", icon, entry.name);
                                    let response = ui.selectable_label(is_selected, label_text);

                                    if response.clicked() {
                                        if entry.is_dir {
                                            // Navigate into directory
                                            let mut new_path = path.clone();
                                            new_path.push(&entry.name);
                                            browser_state.current_path =
                                                new_path.display().to_string();
                                            browser_state.selected_files.clear();
                                        } else {
                                            // Toggle file selection
                                            if is_selected {
                                                browser_state
                                                    .selected_files
                                                    .retain(|f| f != &entry.name);
                                            } else {
                                                browser_state
                                                    .selected_files
                                                    .push(entry.name.clone());
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

        if ui
            .add_enabled(has_selection, egui::Button::new("Download"))
            .clicked()
        {
            // Open file dialog for download destination
            file_dialog.pick_directory();
        }

        if ui.button("Upload").clicked() {
            // Open file dialog for upload source
            file_dialog.pick_file();
        }

        if ui
            .add_enabled(has_selection, egui::Button::new("Delete"))
            .on_hover_text("Delete selected files")
            .clicked()
        {
            // TODO: Implement delete functionality
            browser_state.selected_files.clear();
        }

        if ui.button("New Folder").clicked() {
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
    if let Ok(usage) = query_filesystem_usage(instance_id) {
        let used_gb = usage.used as f64 / 1_000_000_000.0;
        let total_gb = usage.total as f64 / 1_000_000_000.0;
        let percent = if usage.total > 0 {
            (usage.used as f64 / usage.total as f64) * 100.0
        } else {
            0.0
        };

        ui.label(format!(
            "Disk Usage: {:.1} GB / {:.1} GB ({:.1}%)",
            used_gb, total_gb, percent
        ));

        let progress_bar = egui::ProgressBar::new(percent as f32 / 100.0).show_percentage();
        ui.add(progress_bar);
    }

    // Persist state
    ui.data_mut(|d| d.insert_persisted(state_id, browser_state));
}

/// Filesystem layer GUI extension.
pub struct FilesystemGuiExtension;

impl LayerGuiExtension for FilesystemGuiExtension {
    fn layer(&self) -> &LayerName {
        static LAYER: std::sync::LazyLock<LayerName> =
            std::sync::LazyLock::new(|| LayerName::from("Filesystem"));
        &LAYER
    }

    fn description(&self) -> &'static str {
        "Browse and manage remote filesystems"
    }

    fn render_controller(&self, ui: &mut egui::Ui, instance_id: InstanceId) {
        render(ui, instance_id);
    }

    fn controller_name(&self) -> &'static str {
        "File Browser"
    }

    fn get_node_svg(&self, _instance_id: InstanceId) -> &'static str {
        // Show OS-specific icons for filesystem layer
        // TODO: Query instance metadata
        "os/Unknown.svg"
    }

    fn get_node_color(&self, instance_id: InstanceId) -> Color {
        // Color based on disk usage
        if let Ok(usage) = query_filesystem_usage(instance_id) {
            let percent = if usage.total > 0 {
                (usage.used as f64 / usage.total as f64) * 100.0
            } else {
                0.0
            };

            if percent < 70.0 {
                Color::srgb(0.7, 1.0, 0.7) // Green
            } else if percent < 90.0 {
                Color::srgb(1.0, 1.0, 0.7) // Yellow
            } else {
                Color::srgb(1.0, 0.7, 0.7) // Red
            }
        } else {
            Color::WHITE
        }
    }

    fn preview_icon(&self) -> &'static str {
        "Folder"
    }

    fn preview_details(&self, instance_id: InstanceId) -> String {
        if let Ok(usage) = query_filesystem_usage(instance_id) {
            let used_gb = usage.used as f64 / 1_000_000_000.0;
            let total_gb = usage.total as f64 / 1_000_000_000.0;
            if total_gb > 0.0 {
                let percent = (usage.used as f64 / usage.total as f64) * 100.0;
                format!("{:.1} GB / {:.1} GB ({:.0}%)", used_gb, total_gb, percent)
            } else {
                "No filesystem data".to_string()
            }
        } else {
            "No filesystem data".to_string()
        }
    }

    fn edge_color(&self) -> Color {
        Color::srgb(0.3, 1.0, 0.3) // Green
    }

    fn activity_types(&self) -> Vec<ActivityTypeInfo> {
        vec![ActivityTypeInfo {
            id: "file_transfer",
            name: "File Transfer",
            color: Color::srgb(0.3, 0.8, 0.3),
            size: 8.0,
        }]
    }

    fn visible_instance_types(&self) -> &'static [sandpolis_instance::InstanceType] {
        // Filesystem layer shows servers and agents (not clients)
        &[sandpolis_instance::InstanceType::Server, sandpolis_instance::InstanceType::Agent]
    }
}

/// Static instance of the filesystem GUI extension.
static FILESYSTEM_GUI_EXT: FilesystemGuiExtension = FilesystemGuiExtension;

// Register the extension with inventory
inventory::submit! {
    &FILESYSTEM_GUI_EXT as &dyn LayerGuiExtension
}
