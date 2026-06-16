//! Layer toolbar: a row of icon buttons shown just above the layer indicator. The
//! buttons depend on the active layer and come from each layer's registered
//! [`ToolbarAction`](crate::gui::ui::controller::ToolbarAction)s.
//!
//! The toolbar root is spawned (empty) as a child of the layer-chrome column in
//! [`crate::gui::layer_ui`]; [`rebuild_layer_toolbar`] fills it with buttons
//! reactively whenever the active layer (or the registry) changes.

use crate::gui::input::CurrentLayer;
use crate::gui::ui::Activate;
use crate::gui::ui::controller::LayerRegistry;
use crate::gui::ui::icon::IconCache;
use crate::gui::ui::theme::Theme;
use crate::gui::ui::tooltip::Tooltip;
use crate::gui::ui::widgets::icon_button;
use bevy::image::Image;
use bevy::prelude::*;

/// Icon rasterization size (px) for toolbar buttons.
const ICON_PX: u32 = 40;

/// Marker for the toolbar row container (filled reactively).
#[derive(Component)]
pub struct LayerToolbarRoot;

/// Marker for a single toolbar button.
#[derive(Component)]
pub struct LayerToolbarButton;

/// Rebuild the toolbar buttons when the active layer or registry changes.
pub fn rebuild_layer_toolbar(
    mut commands: Commands,
    theme: Res<Theme>,
    registry: Res<LayerRegistry>,
    current: Res<CurrentLayer>,
    mut images: ResMut<Assets<Image>>,
    mut icon_cache: ResMut<IconCache>,
    root: Query<Entity, With<LayerToolbarRoot>>,
    buttons: Query<Entity, With<LayerToolbarButton>>,
) {
    if !(current.is_changed() || registry.is_changed()) {
        return;
    }
    let Ok(root) = root.single() else {
        return;
    };

    for entity in &buttons {
        commands.entity(entity).despawn();
    }

    let actions = registry.toolbar_actions(&current);
    if actions.is_empty() {
        return;
    }

    commands.entity(root).with_children(|parent| {
        for action in actions {
            let handle = icon_cache.get_or_rasterize(&mut images, action.icon, ICON_PX);
            let on_click = action.on_click.clone();
            parent
                .spawn((
                    LayerToolbarButton,
                    Tooltip::new(action.label),
                    icon_button(&theme, handle),
                ))
                .observe(move |_: On<Activate>, mut commands: Commands| {
                    (on_click)(&mut commands);
                });
        }
    });
}
