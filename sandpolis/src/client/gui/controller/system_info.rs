use bevy_egui::egui;
use sandpolis_core::InstanceId;
use crate::client::gui::queries;

/// Render system information controller
pub fn render(ui: &mut egui::Ui, network_layer: &sandpolis_network::NetworkLayer, instance_id: InstanceId) {
    // Push unique ID scope to prevent collisions with collapsing headers
    ui.push_id(instance_id.to_string(), |ui| {
        egui::ScrollArea::vertical()
            .max_height(350.0)
            .show(ui, |ui| {
                // Hardware Information Section
                ui.collapsing("Hardware", |ui| {
                if let Ok(info) = queries::query_hardware_info(instance_id) {
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
                if let Ok(metadata) = queries::query_instance_metadata(instance_id) {
                    ui.label(format!("OS Type: {:?}", metadata.os_type));
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
                if let Ok(mem) = queries::query_memory_stats(instance_id) {
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

                    ui.add(egui::ProgressBar::new(percent as f32 / 100.0)
                        .text(format!("{:.1}%", percent)));
                } else {
                    ui.label("Memory usage not available");
                }
            });

            ui.separator();

            // Storage Usage Section
            ui.collapsing("Storage Usage", |ui| {
                if let Ok(usage) = queries::query_filesystem_usage(instance_id) {
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

                    ui.add(egui::ProgressBar::new(percent as f32 / 100.0)
                        .text(format!("{:.1}%", percent)));
                } else {
                    ui.label("Storage usage not available");
                }
            });

            ui.separator();

            // Network Information Section
            ui.collapsing("Network", |ui| {
                if let Ok(stats) = queries::query_network_stats(network_layer, instance_id) {
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
