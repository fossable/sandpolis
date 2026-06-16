//! World-anchored node preview cards.
//!
//! Each node gets a small card anchored below it (via
//! [`WorldAnchored`](crate::gui::ui::anchored::WorldAnchored)) showing a layer
//! icon, the hostname, a layer-specific detail line, and an "Open" button that
//! opens the node controller. Toggled with `P`.

use crate::gui::controller::NodeControllerState;
use crate::gui::input::CurrentLayer;
use crate::gui::layer_ui::layer_icon_path;
use crate::gui::node::NodeEntity;
use crate::gui::queries;
use crate::gui::ui::Activate;
use crate::gui::ui::anchored::WorldAnchored;
use crate::gui::ui::gating::UiPointerState;
use crate::gui::ui::icon::IconCache;
use crate::gui::ui::theme::{Role, Theme, ThemedBg, ThemedBorder};
use crate::gui::ui::widgets::{button, text};
use crate::gui::ui::z;
use bevy::image::Image;
use bevy::prelude::*;
use sandpolis_instance::InstanceId;
use sandpolis_instance::LayerName;
use sandpolis_instance::network::NetworkLayer;

/// Get hostname for an instance.
pub fn get_hostname(instance_id: InstanceId) -> String {
    queries::query_instance_metadata(instance_id)
        .ok()
        .and_then(|m| m.hostname)
        .unwrap_or_else(|| format!("Node {}", instance_id))
}

/// Get layer-specific bottom line details for a node's preview card.
pub fn get_layer_details(
    layer: &LayerName,
    network_layer: &NetworkLayer,
    instance_id: InstanceId,
) -> String {
    match layer.name() {
        #[cfg(feature = "layer-filesystem")]
        "Filesystem" => {
            if let Ok(usage) = queries::query_filesystem_usage(instance_id) {
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

        "Network" => {
            if let Ok(stats) = queries::query_network_stats(network_layer, instance_id) {
                if let Some(latency) = stats.latency_ms {
                    format!("Latency: {} ms", latency)
                } else {
                    "No network connection".to_string()
                }
            } else {
                "No network connection".to_string()
            }
        }

        #[cfg(feature = "layer-inventory")]
        "Inventory" => {
            if let Ok(mem) = queries::query_memory_stats(instance_id) {
                let percent = if mem.total > 0 {
                    (mem.used as f64 / mem.total as f64) * 100.0
                } else {
                    0.0
                };
                format!("Memory: {:.0}% used", percent)
            } else {
                "No system data".to_string()
            }
        }

        #[cfg(feature = "layer-shell")]
        "Shell" => {
            if let Ok(sessions) = queries::query_shell_sessions(instance_id) {
                let active_count = sessions.iter().filter(|s| s.active).count();
                format!(
                    "{} session{}, {} active",
                    sessions.len(),
                    if sessions.len() == 1 { "" } else { "s" },
                    active_count
                )
            } else {
                "No shell sessions".to_string()
            }
        }

        #[cfg(feature = "layer-desktop")]
        "Desktop" => {
            if let Ok(metadata) = queries::query_instance_metadata(instance_id) {
                format!("OS: {:?}", metadata.os_type)
            } else {
                "No desktop data".to_string()
            }
        }

        _ => format!("Layer: {}", layer),
    }
}

/// Icon size for preview cards, in pixels.
const PREVIEW_ICON_PX: u32 = 20;

/// Whether node preview cards are shown (toggled with `P`).
#[derive(Resource)]
pub struct PreviewsVisible(pub bool);

impl Default for PreviewsVisible {
    fn default() -> Self {
        Self(true)
    }
}

/// Marker for a preview card root (one per node).
#[derive(Component)]
pub struct NodePreviewUi {
    pub node: Entity,
    pub instance_id: InstanceId,
}

/// Marker for a preview card's layer icon.
#[derive(Component)]
pub struct PreviewIcon;

/// Marker for a preview card's detail line.
#[derive(Component)]
pub struct PreviewDetail {
    pub instance_id: InstanceId,
}

/// Marker for a preview card's "open controller" button.
#[derive(Component)]
pub struct PreviewOpenButton {
    pub instance_id: InstanceId,
}

/// Toggle preview visibility with `P` (unless a text field is focused).
pub fn toggle_previews(
    ui_pointer: Res<UiPointerState>,
    keyboard: Res<ButtonInput<KeyCode>>,
    mut visible: ResMut<PreviewsVisible>,
) {
    if ui_pointer.wants_keyboard {
        return;
    }
    if keyboard.just_pressed(KeyCode::KeyP) {
        visible.0 = !visible.0;
    }
}

/// Spawn a preview card per node, despawn orphans, and clear all when hidden.
#[allow(clippy::too_many_arguments)]
pub fn sync_node_previews(
    mut commands: Commands,
    theme: Res<Theme>,
    visible: Res<PreviewsVisible>,
    current_layer: Res<CurrentLayer>,
    network_layer: Res<NetworkLayer>,
    mut images: ResMut<Assets<Image>>,
    mut icon_cache: ResMut<IconCache>,
    nodes: Query<(Entity, &NodeEntity)>,
    previews: Query<(Entity, &NodePreviewUi)>,
) {
    if !visible.0 {
        for (entity, _) in &previews {
            commands.entity(entity).despawn();
        }
        return;
    }

    // Spawn cards for new nodes.
    for (node_entity, node) in &nodes {
        if previews.iter().any(|(_, p)| p.node == node_entity) {
            continue;
        }
        let icon = icon_cache.get_or_rasterize(
            &mut images,
            layer_icon_path(&current_layer),
            PREVIEW_ICON_PX,
        );
        let hostname = get_hostname(node.instance_id);
        let detail = get_layer_details(&current_layer, &network_layer, node.instance_id);
        let instance_id = node.instance_id;

        commands
            .spawn((
                NodePreviewUi {
                    node: node_entity,
                    instance_id,
                },
                WorldAnchored {
                    target: node_entity,
                    offset: Vec2::new(0.0, 55.0),
                },
                GlobalZIndex(z::ANCHORED),
                Node {
                    position_type: PositionType::Absolute,
                    flex_direction: FlexDirection::Row,
                    align_items: AlignItems::Center,
                    column_gap: Val::Px(6.0),
                    padding: UiRect::all(Val::Px(6.0)),
                    border: UiRect::all(Val::Px(1.0)),
                    ..default()
                },
                BackgroundColor(theme.color(Role::Panel)),
                ThemedBg(Role::Panel),
                BorderColor::all(theme.color(Role::Border)),
                ThemedBorder(Role::Border),
            ))
            .with_children(|card| {
                card.spawn((
                    PreviewIcon,
                    ImageNode::new(icon),
                    Node {
                        width: Val::Px(PREVIEW_ICON_PX as f32),
                        height: Val::Px(PREVIEW_ICON_PX as f32),
                        ..default()
                    },
                ));
                card.spawn(Node {
                    flex_direction: FlexDirection::Column,
                    ..default()
                })
                .with_children(|col| {
                    col.spawn(text(&theme, hostname, theme.metrics.font_md, Role::Text));
                    col.spawn((
                        PreviewDetail { instance_id },
                        text(&theme, detail, theme.metrics.font_sm, Role::TextMuted),
                    ));
                });
                card.spawn((PreviewOpenButton { instance_id }, button(&theme, "Open")))
                    .observe(on_preview_open);
            });
    }

    // Despawn cards whose node is gone.
    for (preview_entity, preview) in &previews {
        if !nodes.iter().any(|(entity, _)| entity == preview.node) {
            commands.entity(preview_entity).despawn();
        }
    }
}

/// Refresh preview icon + detail when the active layer changes.
pub fn update_preview_content(
    current_layer: Res<CurrentLayer>,
    network_layer: Res<NetworkLayer>,
    mut images: ResMut<Assets<Image>>,
    mut icon_cache: ResMut<IconCache>,
    mut icons: Query<&mut ImageNode, With<PreviewIcon>>,
    mut details: Query<(&PreviewDetail, &mut Text)>,
) {
    if !current_layer.is_changed() {
        return;
    }
    let handle = icon_cache.get_or_rasterize(
        &mut images,
        layer_icon_path(&current_layer),
        PREVIEW_ICON_PX,
    );
    for mut icon in &mut icons {
        icon.image = handle.clone();
    }
    for (detail, mut label) in &mut details {
        label.0 = get_layer_details(&current_layer, &network_layer, detail.instance_id);
    }
}

/// Open the node controller for the clicked preview's node.
fn on_preview_open(
    activate: On<Activate>,
    buttons: Query<&PreviewOpenButton>,
    mut controller_state: ResMut<NodeControllerState>,
) {
    if let Ok(button) = buttons.get(activate.entity) {
        controller_state.open = Some(button.instance_id);
    }
}
