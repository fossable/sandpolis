use super::queries;
use super::{CurrentLayer, components::NodeEntity};
use crate::{InstanceState, Layer};
use bevy::prelude::*;
use bevy_egui::{EguiContexts, egui};

/// Component tracking NodePreview state for each node
#[derive(Component)]
pub struct NodePreview {
    pub show: bool,
    pub width: f32,
    pub height: f32,
}

impl NodePreview {
    /// Create responsive preview based on window size
    pub fn from_window_size(window_width: f32, window_height: f32) -> Self {
        // For mobile screens (< 800px width), use smaller preview
        let is_mobile = window_width < 800.0;

        if is_mobile {
            Self {
                show: true,
                width: (window_width * 0.35).clamp(140.0, 180.0),
                height: (window_height * 0.12).clamp(70.0, 90.0),
            }
        } else {
            Self::default()
        }
    }
}

impl Default for NodePreview {
    fn default() -> Self {
        Self {
            show: true,
            width: 200.0,
            height: 100.0,
        }
    }
}

/// Render NodePreview windows below each node with layer-specific content
pub fn render_node_previews(
    mut contexts: EguiContexts,
    current_layer: Res<CurrentLayer>,
    state: Res<InstanceState>,
    camera_query: Query<(&Camera, &GlobalTransform), With<super::components::WorldView>>,
    node_query: Query<(&Transform, &NodeEntity, Option<&NodePreview>)>,
    windows: Query<&Window>,
) {
    let Ok(window) = windows.single() else {
        return;
    };

    let Ok((camera, camera_transform)) = camera_query.single() else {
        return;
    };

    let Ok(ctx) = contexts.ctx_mut() else {
        return;
    };

    // Render preview for each node
    for (transform, node_entity, preview_opt) in node_query.iter() {
        // Skip if preview is disabled
        let default_preview = NodePreview::default();
        let preview = preview_opt.unwrap_or(&default_preview);
        if !preview.show {
            continue;
        }

        // Convert world position to screen position
        let Ok(viewport_pos) = camera.world_to_viewport(camera_transform, transform.translation)
        else {
            continue;
        };

        // Position preview below the node
        let preview_pos = egui::Pos2::new(
            viewport_pos.x - preview.width / 2.0,
            viewport_pos.y + 60.0, // Offset below node
        );

        // Render layer-specific content
        egui::Window::new(format!("Preview_{}", node_entity.instance_id))
            .title_bar(false)
            .resizable(false)
            .movable(false)
            .fixed_pos(preview_pos)
            .fixed_size([preview.width, preview.height])
            .show(ctx, |ui| {
                render_preview_content(ui, &current_layer, &state, node_entity.instance_id);
            });
    }
}

/// Render layer-specific content inside the NodePreview
fn render_preview_content(
    ui: &mut egui::Ui,
    current_layer: &Layer,
    state: &InstanceState,
    instance_id: sandpolis_core::InstanceId,
) {
    match *current_layer {
        #[cfg(feature = "layer-filesystem")]
        Layer::Filesystem => {
            ui.label(egui::RichText::new("Filesystem").strong());
            ui.separator();

            // Query filesystem usage from database
            if let Ok(usage) = queries::query_filesystem_usage(state, instance_id) {
                let used_gb = usage.used as f64 / 1_000_000_000.0;
                let total_gb = usage.total as f64 / 1_000_000_000.0;
                let percent = if usage.total > 0 {
                    (usage.used as f64 / usage.total as f64) * 100.0
                } else {
                    0.0
                };

                ui.label(format!("Used: {:.1} GB / {:.1} GB", used_gb, total_gb));
                ui.add(
                    egui::ProgressBar::new(percent as f32 / 100.0).text(format!("{:.0}%", percent)),
                );
            } else {
                ui.label("No data");
            }
        }

        Layer::Network => {
            ui.label(egui::RichText::new("Network").strong());
            ui.separator();

            // Query network stats from database
            if let Ok(stats) = queries::query_network_stats(state, instance_id) {
                if let Some(latency) = stats.latency_ms {
                    ui.label(format!("Latency: {} ms", latency));
                }
                if let Some(throughput) = stats.throughput_bps {
                    let mbps = throughput as f64 / 1_000_000.0;
                    ui.label(format!("Throughput: {:.2} Mbps", mbps));
                }
            } else {
                ui.label("No connection");
            }
        }

        #[cfg(feature = "layer-inventory")]
        Layer::Inventory => {
            ui.label(egui::RichText::new("System Info").strong());
            ui.separator();

            // Query hardware info from database
            if let Ok(info) = queries::query_hardware_info(state, instance_id) {
                if let Some(cpu) = info.cpu_model {
                    ui.label(format!("CPU: {}", cpu));
                }
                if let Some(cores) = info.cpu_cores {
                    ui.label(format!("Cores: {}", cores));
                }
                if let Some(mem) = info.memory_total {
                    let gb = mem as f64 / 1_000_000_000.0;
                    ui.label(format!("RAM: {:.1} GB", gb));
                }
            } else {
                ui.label("No data");
            }

            // Query memory stats
            if let Ok(mem) = queries::query_memory_stats(state, instance_id) {
                let percent = if mem.total > 0 {
                    (mem.used as f64 / mem.total as f64) * 100.0
                } else {
                    0.0
                };
                ui.add(
                    egui::ProgressBar::new(percent as f32 / 100.0)
                        .text(format!("Memory: {:.0}%", percent)),
                );
            }
        }

        #[cfg(feature = "layer-shell")]
        Layer::Shell => {
            ui.label(egui::RichText::new("Shell").strong());
            ui.separator();

            // Query shell sessions from database
            if let Ok(sessions) = queries::query_shell_sessions(state, instance_id) {
                let active_count = sessions.iter().filter(|s| s.active).count();
                ui.label(format!("Sessions: {}", sessions.len()));
                ui.label(format!("Active: {}", active_count));
            } else {
                ui.label("No sessions");
            }
        }

        #[cfg(feature = "layer-desktop")]
        Layer::Desktop => {
            ui.label(egui::RichText::new("Desktop").strong());
            ui.separator();

            // Query instance metadata for OS info
            if let Ok(metadata) = queries::query_instance_metadata(state, instance_id) {
                ui.label(format!("OS: {:?}", metadata.os_type));
                if let Some(hostname) = metadata.hostname {
                    ui.label(format!("Host: {}", hostname));
                }
            } else {
                ui.label("No data");
            }
        }

        _ => {
            // Default preview for other layers
            ui.label(egui::RichText::new(format!("{:?}", current_layer)).strong());
            ui.separator();
            ui.label(format!("Instance: {}", instance_id));
        }
    }
}

/// Toggle NodePreview visibility based on user input
pub fn toggle_node_preview_visibility(
    mut contexts: bevy_egui::EguiContexts,
    keyboard: Res<ButtonInput<KeyCode>>,
    mut node_query: Query<&mut NodePreview>,
) {
    // Don't handle hotkey if egui wants keyboard input
    let Ok(ctx) = contexts.ctx_mut() else {
        return;
    };
    if ctx.wants_keyboard_input() {
        return;
    }

    // Press 'P' to toggle preview visibility
    if keyboard.just_pressed(KeyCode::KeyP) {
        for mut preview in node_query.iter_mut() {
            preview.show = !preview.show;
        }
    }
}
