//! GUI components for the Filesystem layer.
//!
//! Provides the file-browser node controller and the layer's client plugin.
//!
//! Note: interactive remote navigation and local file picking (via `rfd`) are
//! deferred until the filesystem residents/queries return live data; today the
//! directory query is a stub, so the controller surfaces disk usage plus the
//! transfer actions.

use bevy::prelude::*;
use sandpolis_client::gui::ui::Activate;
use sandpolis_client::gui::ui::bind::bind_text;
use sandpolis_client::gui::ui::controller::{
    LayerClientInfo, NodeController, RegisterLayerClient,
};
use sandpolis_client::gui::ui::theme::{Role, Theme};
use sandpolis_client::gui::ui::widgets::{button, heading, muted, row, text};
use sandpolis_instance::{InstanceId, InstanceType, LayerName};

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

/// The filesystem layer's node controller (file browser).
pub struct FilesystemController;

impl NodeController for FilesystemController {
    fn title(&self) -> &str {
        "File Browser"
    }

    fn build(&self, commands: &mut Commands, body: Entity, instance: InstanceId, theme: &Theme) {
        commands.entity(body).with_children(|p| {
            // Path bar.
            p.spawn(row(theme.metrics.space_sm)).with_children(|bar| {
                bar.spawn(text(theme, "Remote path:", theme.metrics.font_md, Role::TextMuted));
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
            p.spawn(row(theme.metrics.space_sm)).with_children(|actions| {
                actions
                    .spawn(button(theme, "Download"))
                    .observe(move |_: On<Activate>| info!("Filesystem: download from {}", instance));
                actions
                    .spawn(button(theme, "Upload"))
                    .observe(move |_: On<Activate>| info!("Filesystem: upload to {}", instance));
                actions
                    .spawn(button(theme, "Delete"))
                    .observe(move |_: On<Activate>| info!("Filesystem: delete on {}", instance));
                actions
                    .spawn(button(theme, "New Folder"))
                    .observe(move |_: On<Activate>| info!("Filesystem: new folder on {}", instance));
            });

            // Disk usage.
            p.spawn((
                text(theme, "", theme.metrics.font_md, Role::Text),
                bind_text(move || {
                    let usage = query_filesystem_usage(instance).unwrap_or_default();
                    if usage.total > 0 {
                        let percent = (usage.used as f64 / usage.total as f64) * 100.0;
                        format!(
                            "Disk: {:.1} GB / {:.1} GB ({:.1}%)",
                            usage.used as f64 / 1e9,
                            usage.total as f64 / 1e9,
                            percent
                        )
                    } else {
                        "No filesystem data".into()
                    }
                }),
            ));
        });
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
            .with_controller(FilesystemController)
            .with_visible_instance_types(&[InstanceType::Server, InstanceType::Agent]),
        );
    }
}
