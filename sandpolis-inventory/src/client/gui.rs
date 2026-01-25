//! GUI components for the Inventory layer.
//!
//! This module provides the system information controller and layer-specific GUI elements.

use bevy::prelude::*;
use bevy_egui::egui;
use sandpolis_core::{InstanceId, LayerName};

use sandpolis_client::gui::layer_ext::{ActivityTypeInfo, LayerGuiExtension};

/// Hardware information.
#[derive(Clone, Debug, Default)]
pub struct HardwareInfo {
    pub cpu_model: Option<String>,
    pub cpu_cores: Option<u32>,
    pub memory_total: Option<u64>,
}

/// Memory statistics.
#[derive(Clone, Debug, Default)]
pub struct MemoryStats {
    pub total: u64,
    pub used: u64,
    pub free: u64,
}

/// Filesystem usage statistics.
#[derive(Clone, Debug, Default)]
pub struct FilesystemUsage {
    pub total: u64,
    pub used: u64,
    pub free: u64,
}

/// Network statistics.
#[derive(Clone, Debug, Default)]
pub struct NetworkStats {
    pub latency_ms: Option<u32>,
    pub throughput_bps: Option<u64>,
}

/// Instance metadata.
#[derive(Clone, Debug, Default)]
pub struct InstanceMetadata {
    pub os_type: String,
    pub hostname: Option<String>,
}

/// Package information.
#[derive(Clone, Debug)]
pub struct PackageInfo {
    pub name: String,
    pub version: String,
}

/// Query hardware information for an instance.
pub fn query_hardware_info(_id: InstanceId) -> anyhow::Result<HardwareInfo> {
    // TODO: Query from inventory resident
    Ok(HardwareInfo::default())
}

/// Query instance metadata.
pub fn query_instance_metadata(_id: InstanceId) -> anyhow::Result<InstanceMetadata> {
    // TODO: Query from database
    Ok(InstanceMetadata::default())
}

/// Query memory statistics.
pub fn query_memory_stats(_id: InstanceId) -> anyhow::Result<MemoryStats> {
    // TODO: Query from inventory resident
    Ok(MemoryStats::default())
}

/// Query filesystem usage.
pub fn query_filesystem_usage(_id: InstanceId) -> anyhow::Result<FilesystemUsage> {
    // TODO: Query from inventory resident
    Ok(FilesystemUsage::default())
}

/// Query network statistics.
pub fn query_network_stats(_id: InstanceId) -> anyhow::Result<NetworkStats> {
    // TODO: Query from network layer
    Ok(NetworkStats::default())
}

/// Query installed packages.
pub fn query_packages(_id: InstanceId) -> anyhow::Result<Vec<PackageInfo>> {
    // TODO: Query from inventory resident
    Ok(vec![])
}

/// Render system information controller.
pub fn render_system_info(ui: &mut egui::Ui, instance_id: InstanceId) {
    // Push unique ID scope to prevent collisions with collapsing headers
    ui.push_id(instance_id, |ui| {
        egui::ScrollArea::vertical()
            .max_height(350.0)
            .show(ui, |ui| {
                // Hardware Information Section
                ui.collapsing("Hardware", |ui| {
                    if let Ok(info) = query_hardware_info(instance_id) {
                        ui.label(egui::RichText::new("CPU").strong());
                        if let Some(cpu_model) = info.cpu_model {
                            ui.label(format!("Model: {}", cpu_model));
                        } else {
                            ui.label("Model: Unknown");
                        }
                        if let Some(cores) = info.cpu_cores {
                            ui.label(format!("Cores: {}", cores));
                        }

                        ui.add_space(10.0);

                        ui.label(egui::RichText::new("Memory").strong());
                        if let Some(memory) = info.memory_total {
                            let gb = memory as f64 / 1_000_000_000.0;
                            ui.label(format!("Total RAM: {:.2} GB", gb));
                        } else {
                            ui.label("Total RAM: Unknown");
                        }
                    } else {
                        ui.label("Hardware information not available");
                    }
                });

                ui.separator();

                // OS Information Section
                ui.collapsing("Operating System", |ui| {
                    if let Ok(metadata) = query_instance_metadata(instance_id) {
                        ui.label(format!("OS Type: {}", metadata.os_type));
                        if let Some(hostname) = metadata.hostname {
                            ui.label(format!("Hostname: {}", hostname));
                        }
                    } else {
                        ui.label("OS information not available");
                    }
                });

                ui.separator();

                // Memory Usage Section
                ui.collapsing("Memory Usage", |ui| {
                    if let Ok(mem) = query_memory_stats(instance_id) {
                        let total_gb = mem.total as f64 / 1_000_000_000.0;
                        let used_gb = mem.used as f64 / 1_000_000_000.0;
                        let free_gb = mem.free as f64 / 1_000_000_000.0;
                        let percent = if mem.total > 0 {
                            (mem.used as f64 / mem.total as f64) * 100.0
                        } else {
                            0.0
                        };

                        ui.label(format!("Total: {:.2} GB", total_gb));
                        ui.label(format!("Used: {:.2} GB", used_gb));
                        ui.label(format!("Free: {:.2} GB", free_gb));

                        ui.add(
                            egui::ProgressBar::new(percent as f32 / 100.0)
                                .text(format!("{:.1}%", percent)),
                        );
                    } else {
                        ui.label("Memory usage not available");
                    }
                });

                ui.separator();

                // Storage Usage Section
                ui.collapsing("Storage Usage", |ui| {
                    if let Ok(usage) = query_filesystem_usage(instance_id) {
                        let total_gb = usage.total as f64 / 1_000_000_000.0;
                        let used_gb = usage.used as f64 / 1_000_000_000.0;
                        let free_gb = usage.free as f64 / 1_000_000_000.0;
                        let percent = if usage.total > 0 {
                            (usage.used as f64 / usage.total as f64) * 100.0
                        } else {
                            0.0
                        };

                        ui.label(format!("Total: {:.2} GB", total_gb));
                        ui.label(format!("Used: {:.2} GB", used_gb));
                        ui.label(format!("Free: {:.2} GB", free_gb));

                        ui.add(
                            egui::ProgressBar::new(percent as f32 / 100.0)
                                .text(format!("{:.1}%", percent)),
                        );
                    } else {
                        ui.label("Storage usage not available");
                    }
                });

                ui.separator();

                // Network Information Section
                ui.collapsing("Network", |ui| {
                    if let Ok(stats) = query_network_stats(instance_id) {
                        if let Some(latency) = stats.latency_ms {
                            ui.label(format!("Latency: {} ms", latency));
                        }
                        if let Some(throughput) = stats.throughput_bps {
                            let mbps = throughput as f64 / 1_000_000.0;
                            ui.label(format!("Throughput: {:.2} Mbps", mbps));
                        }
                        if stats.latency_ms.is_none() && stats.throughput_bps.is_none() {
                            ui.label("No network statistics available");
                        }
                    } else {
                        ui.label("Network information not available");
                    }
                });
            });
    });
}

/// Package manager state for egui memory.
#[derive(Default, Clone, serde::Serialize, serde::Deserialize)]
pub struct PackageManagerState {
    pub search_term: String,
    pub selected_package: Option<String>,
}

/// Render package manager controller.
pub fn render_package_manager(ui: &mut egui::Ui, instance_id: InstanceId) {
    let state_id = egui::Id::new(format!("pm_{}", instance_id));
    let mut pm_state = ui.data_mut(|d| {
        d.get_persisted::<PackageManagerState>(state_id)
            .unwrap_or_default()
    });

    ui.horizontal(|ui| {
        ui.label("Search:");
        ui.text_edit_singleline(&mut pm_state.search_term);
        if ui.button("Find").clicked() {}
    });

    ui.separator();
    ui.label("Installed Packages:");
    egui::ScrollArea::vertical()
        .max_height(250.0)
        .show(ui, |ui| match query_packages(instance_id) {
            Ok(packages) => {
                if packages.is_empty() {
                    ui.label("No packages found");
                } else {
                    for package in packages {
                        let is_selected = pm_state.selected_package.as_ref() == Some(&package.name);
                        if ui
                            .selectable_label(
                                is_selected,
                                format!("{} v{}", package.name, package.version),
                            )
                            .clicked()
                        {
                            pm_state.selected_package = Some(package.name.clone());
                        }
                    }
                }
            }
            Err(_) => {
                ui.label("Error loading packages");
            }
        });

    ui.separator();
    ui.horizontal(|ui| {
        let _ = ui.button("Install");
        let has_selection = pm_state.selected_package.is_some();
        let _ = ui.add_enabled(has_selection, egui::Button::new("Update"));
        let _ = ui.add_enabled(has_selection, egui::Button::new("Remove"));
    });

    ui.data_mut(|d| d.insert_persisted(state_id, pm_state));
}

/// Inventory layer GUI extension.
pub struct InventoryGuiExtension;

impl LayerGuiExtension for InventoryGuiExtension {
    fn layer(&self) -> &LayerName {
        static LAYER: std::sync::LazyLock<LayerName> =
            std::sync::LazyLock::new(|| LayerName::from("Inventory"));
        &LAYER
    }

    fn description(&self) -> &'static str {
        "Hardware and software inventory"
    }

    fn render_controller(&self, ui: &mut egui::Ui, instance_id: InstanceId) {
        // Render system info by default for inventory layer
        render_system_info(ui, instance_id);
    }

    fn controller_name(&self) -> &'static str {
        "System Information"
    }

    fn get_node_svg(&self, _instance_id: InstanceId) -> &'static str {
        "inventory/Computer.svg"
    }

    fn get_node_color(&self, instance_id: InstanceId) -> Color {
        // Color based on memory usage
        if let Ok(mem) = query_memory_stats(instance_id) {
            let percent = if mem.total > 0 {
                (mem.used as f64 / mem.total as f64) * 100.0
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
        "Computer"
    }

    fn preview_details(&self, instance_id: InstanceId) -> String {
        if let Ok(mem) = query_memory_stats(instance_id) {
            if mem.total > 0 {
                let used_gb = mem.used as f64 / 1_000_000_000.0;
                let total_gb = mem.total as f64 / 1_000_000_000.0;
                let percent = (mem.used as f64 / mem.total as f64) * 100.0;
                format!(
                    "RAM: {:.1} GB / {:.1} GB ({:.0}%)",
                    used_gb, total_gb, percent
                )
            } else {
                "No inventory data".to_string()
            }
        } else {
            "No inventory data".to_string()
        }
    }

    fn edge_color(&self) -> Color {
        Color::srgb(0.3, 0.5, 1.0) // Blue
    }

    fn activity_types(&self) -> Vec<ActivityTypeInfo> {
        vec![ActivityTypeInfo {
            id: "system_update",
            name: "System Update",
            color: Color::srgb(0.5, 0.5, 1.0),
            size: 7.0,
        }]
    }

    fn visible_instance_types(&self) -> &'static [sandpolis_core::InstanceType] {
        // Inventory layer shows servers and agents (not clients)
        &[sandpolis_core::InstanceType::Server, sandpolis_core::InstanceType::Agent]
    }
}

/// Static instance of the inventory GUI extension.
static INVENTORY_GUI_EXT: InventoryGuiExtension = InventoryGuiExtension;

// Register the extension with inventory
inventory::submit! {
    &INVENTORY_GUI_EXT as &dyn LayerGuiExtension
}
