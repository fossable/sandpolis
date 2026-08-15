//! GUI components for the Filesystem layer.
//!
//! Provides the file-browser node panel and the layer's client plugin.
//!
//! Note: interactive remote navigation and local file picking (via `rfd`) are
//! deferred until the filesystem residents/queries return live data; today the
//! directory query is a stub, so the panel surfaces disk usage plus the transfer
//! actions.

use bevy::prelude::*;
use sandpolis_client::gui::layer_visuals::utilization_tint;
use sandpolis_client::gui::ui::Activate;
use sandpolis_client::gui::ui::gauge::{GaugeValue, bind_gauge, gauge};
use sandpolis_client::gui::ui::layer::{LayerClientInfo, RegisterLayerClient};
use sandpolis_client::gui::ui::node_panel::{NodePanel, PanelCtx};
use sandpolis_client::gui::ui::theme::Role;
use sandpolis_client::gui::ui::widgets::{button, heading, muted, row, text};
use sandpolis_instance::{InstanceId, InstanceType, LayerName};

/// Width a gauge is given inside a collapsed panel, whose own width is only
/// whatever its content asks for — a percentage-width track would collapse to
/// nothing there.
const SUMMARY_GAUGE_WIDTH: f32 = 160.0;

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

/// A gauge value describing an instance's disk usage.
fn disk_usage(instance: InstanceId) -> GaugeValue {
    let usage = query_filesystem_usage(instance).unwrap_or_default();
    if usage.total == 0 {
        return GaugeValue::new(0.0, "No filesystem data");
    }
    GaugeValue::ratio(
        usage.used,
        usage.total,
        format!(
            "{:.1} GB / {:.1} GB",
            usage.used as f64 / 1e9,
            usage.total as f64 / 1e9
        ),
    )
}

/// The filesystem layer's node panel (file browser).
pub struct FilesystemPanel;

impl NodePanel for FilesystemPanel {
    fn build_summary(&self, ctx: &mut PanelCtx) {
        let Some(instance) = ctx.target.instance else {
            return;
        };
        let theme = ctx.theme;
        ctx.children(|p| {
            p.spawn(Node {
                width: Val::Px(SUMMARY_GAUGE_WIDTH),
                flex_direction: FlexDirection::Column,
                ..default()
            })
            .with_children(|slot| {
                slot.spawn((
                    gauge(theme, "Disk", disk_usage(instance)),
                    bind_gauge(move || disk_usage(instance)),
                ));
            });
        });
    }

    fn build_detail(&self, ctx: &mut PanelCtx) {
        let Some(instance) = ctx.target.instance else {
            return;
        };
        let theme = ctx.theme;
        ctx.children(|p| {
            // Path bar.
            p.spawn(row(theme.metrics.space_sm)).with_children(|bar| {
                bar.spawn(text(
                    theme,
                    "Remote path:",
                    theme.metrics.font_md,
                    Role::TextMuted,
                ));
                bar.spawn(button(theme, "Home"))
                    .observe(move |_: On<Activate>| info!("Filesystem: go home on {}", instance));
                bar.spawn(button(theme, "Up"))
                    .observe(move |_: On<Activate>| info!("Filesystem: go up on {}", instance));
                bar.spawn(text(theme, "/", theme.metrics.font_md, Role::Text));
            });

            // File list (stub query → empty).
            p.spawn(heading(theme, "Remote Files"));
            p.spawn(muted(theme, "(Empty directory)", theme.metrics.font_md));

            // Actions.
            p.spawn(row(theme.metrics.space_sm))
                .with_children(|actions| {
                    actions.spawn(button(theme, "Download")).observe(
                        move |_: On<Activate>| info!("Filesystem: download from {}", instance),
                    );
                    actions
                        .spawn(button(theme, "Upload"))
                        .observe(move |_: On<Activate>| info!("Filesystem: upload to {}", instance));
                    actions
                        .spawn(button(theme, "Delete"))
                        .observe(move |_: On<Activate>| info!("Filesystem: delete on {}", instance));
                    actions.spawn(button(theme, "New Folder")).observe(
                        move |_: On<Activate>| info!("Filesystem: new folder on {}", instance),
                    );
                });

            // Disk usage.
            p.spawn(heading(theme, "Disk Usage"));
            p.spawn((
                gauge(theme, "Disk", disk_usage(instance)),
                bind_gauge(move || disk_usage(instance)),
            ));
        });
    }
}

/// Tint a node by its disk usage while the Filesystem layer is active.
///
/// The layer keeps its OS icon: what distinguishes a node here is how full it
/// is, not what it runs.
fn node_tint(id: InstanceId) -> Color {
    match query_filesystem_usage(id) {
        Ok(usage) => utilization_tint(usage.used, usage.total),
        Err(_) => Color::WHITE,
    }
}

/// The filesystem layer's client plugin.
pub struct FilesystemClientPlugin;

impl Plugin for FilesystemClientPlugin {
    fn build(&self, app: &mut App) {
        app.register_layer_client(
            LayerClientInfo::new(
                LayerName::from("Filesystem"),
                "Browse and manage remote filesystems",
            )
            .with_panel(FilesystemPanel)
            .with_visible_instance_types(&[InstanceType::Server, InstanceType::Agent])
            .with_node_tint(node_tint),
        );
    }
}
