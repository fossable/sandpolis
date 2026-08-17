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
        // A sub-node here is a probe device, whose filesystem the probe
        // subsystem reaches for us.
        #[cfg(feature = "probe")]
        if let Some(device_id) = ctx.target.sub {
            probe::build_summary(ctx, device_id);
            return;
        }

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
        #[cfg(feature = "probe")]
        if let Some(device_id) = ctx.target.sub {
            probe::build_detail(ctx, device_id);
            return;
        }

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

/// Browsing probe devices (NFS, SMB).
///
/// Everything protocol-specific lives behind [`sandpolis_probe::filesystem`]:
/// this module asks for a directory listing and space totals for a device id and
/// renders whatever comes back, without knowing which protocol answered.
#[cfg(feature = "probe")]
mod probe {
    use super::*;
    use sandpolis_client::gui::ui::bind::bind_text;
    use sandpolis_probe::ProbeType;
    use sandpolis_probe::filesystem::{FileKind, client as probe_fs};
    use std::path::PathBuf;

    /// The protocol used to reach a device's filesystem: its first filesystem
    /// protocol, since a device rarely exports the same tree over two.
    pub(super) fn protocol(device_id: u64) -> Option<ProbeType> {
        sandpolis_probe::REGISTERED_DEVICES
            .read()
            .ok()?
            .iter()
            .find(|d| d.id == device_id)
            .and_then(|d| d.device.filesystem_protocols().first().copied())
    }

    /// Space totals reported by the device, for the shared disk gauge.
    fn usage(device_id: u64) -> GaugeValue {
        let Some(usage) = probe_fs::view(device_id).and_then(|view| view.usage) else {
            return GaugeValue::new(0.0, "No filesystem data");
        };
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

    /// Render the current directory as one label. A reusable scrolling table is
    /// still on the client subsystem's list; until it lands, one bound label is
    /// what keeps this honest about live data rather than faking a widget.
    fn listing(device_id: u64) -> String {
        let Some(view) = probe_fs::view(device_id) else {
            return "Loading…".to_string();
        };
        if let Some(error) = view.error {
            return error;
        }
        let Some(entries) = view.entries else {
            return if view.busy {
                "Loading…".to_string()
            } else {
                "Not listed yet".to_string()
            };
        };
        if entries.is_empty() {
            return "(Empty directory)".to_string();
        }
        entries
            .iter()
            .map(|entry| match entry.kind {
                FileKind::Dir => format!("{}/", entry.name),
                _ => format!("{}  ({})", entry.name, human_size(entry.size)),
            })
            .collect::<Vec<_>>()
            .join("\n")
    }

    fn human_size(bytes: u64) -> String {
        const UNITS: [&str; 5] = ["B", "KB", "MB", "GB", "TB"];
        let mut size = bytes as f64;
        let mut unit = 0;
        while size >= 1024.0 && unit < UNITS.len() - 1 {
            size /= 1024.0;
            unit += 1;
        }
        if unit == 0 {
            format!("{bytes} {}", UNITS[0])
        } else {
            format!("{size:.1} {}", UNITS[unit])
        }
    }

    /// The directory currently shown for a device. A view exists before any
    /// listing has landed, so an empty path falls back to the root too.
    fn cwd(device_id: u64) -> PathBuf {
        probe_fs::view(device_id)
            .map(|view| view.cwd)
            .filter(|cwd| cwd.components().next().is_some())
            .unwrap_or_else(|| PathBuf::from("/"))
    }

    pub(super) fn build_summary(ctx: &mut PanelCtx, device_id: u64) {
        let theme = ctx.theme;
        ctx.children(|p| {
            p.spawn(Node {
                width: Val::Px(SUMMARY_GAUGE_WIDTH),
                flex_direction: FlexDirection::Column,
                ..default()
            })
            .with_children(|slot| {
                slot.spawn((
                    gauge(theme, "Disk", usage(device_id)),
                    bind_gauge(move || usage(device_id)),
                ));
            });
        });
    }

    pub(super) fn build_detail(ctx: &mut PanelCtx, device_id: u64) {
        let theme = ctx.theme;
        let Some(protocol) = protocol(device_id) else {
            ctx.children(|p| {
                p.spawn(muted(
                    theme,
                    "This device exposes no filesystem protocol.",
                    theme.metrics.font_md,
                ));
            });
            return;
        };

        ctx.children(|p| {
            // Path bar.
            p.spawn(row(theme.metrics.space_sm)).with_children(|bar| {
                bar.spawn(button(theme, "Home"))
                    .observe(move |_: On<Activate>| {
                        probe_fs::browse(device_id, protocol, PathBuf::from("/"));
                    });
                bar.spawn(button(theme, "Up"))
                    .observe(move |_: On<Activate>| {
                        let current = cwd(device_id);
                        let parent = current.parent().map(PathBuf::from).unwrap_or_else(|| PathBuf::from("/"));
                        probe_fs::browse(device_id, protocol, parent);
                    });
                bar.spawn(button(theme, "Refresh"))
                    .observe(move |_: On<Activate>| {
                        probe_fs::browse(device_id, protocol, cwd(device_id));
                    });
                bar.spawn((
                    text(theme, "/", theme.metrics.font_md, Role::Text),
                    bind_text(move || cwd(device_id).display().to_string()),
                ));
            });

            p.spawn(heading(theme, "Remote Files"));
            p.spawn((
                // Opening the panel is the request to see the directory; the
                // listing loads itself (see `start_pending_browses`).
                ProbeBrowser {
                    device_id,
                    protocol,
                },
                text(theme, "", theme.metrics.font_md, Role::Text),
                bind_text(move || listing(device_id)),
            ));

            p.spawn(heading(theme, "Disk Usage"));
            p.spawn((
                gauge(theme, "Disk", usage(device_id)),
                bind_gauge(move || usage(device_id)),
            ));
        });
    }

    /// An open file listing for a probe device.
    #[derive(Component)]
    pub(super) struct ProbeBrowser {
        device_id: u64,
        protocol: ProbeType,
    }

    /// Devices already asked to list, so the auto-browse below doesn't refire
    /// every frame.
    #[derive(Resource, Default)]
    pub(super) struct BrowsedDevices(std::collections::HashSet<u64>);

    /// List the root directory of any browser that hasn't loaded yet.
    ///
    /// Checked every frame rather than on `Added` so a listing that couldn't
    /// start (no connection) is retried until it can.
    pub(super) fn start_pending_browses(
        mut browsed: ResMut<BrowsedDevices>,
        browsers: Query<&ProbeBrowser>,
        mut open: Local<std::collections::HashSet<u64>>,
    ) {
        open.clear();
        for browser in &browsers {
            open.insert(browser.device_id);
            if browsed.0.contains(&browser.device_id) {
                continue;
            }
            if probe_fs::connection_for(browser.device_id).is_none() {
                continue;
            }
            probe_fs::browse(browser.device_id, browser.protocol, PathBuf::from("/"));
            browsed.0.insert(browser.device_id);
        }
        // Forget closed panels, so reopening one re-lists — which is how a file
        // server that was unreachable gets retried.
        browsed.0.retain(|device_id| open.contains(device_id));
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
        #[cfg(feature = "probe")]
        {
            app.init_resource::<probe::BrowsedDevices>();
            app.add_systems(Update, probe::start_pending_browses);
        }

        let info = LayerClientInfo::new(
            LayerName::from("Filesystem"),
            "Browse and manage remote filesystems",
        )
        .with_panel(FilesystemPanel)
        .with_visible_instance_types(&[InstanceType::Agent])
        .with_node_tint(node_tint);

        // NFS and SMB probes are browsable here just like agents. Devices that
        // expose nothing this layer can drive stay hidden.
        #[cfg(feature = "probe")]
        let info = info.showing_probe_nodes_for(&["NFS", "SMB"]);

        app.register_layer_client(info);
    }
}
