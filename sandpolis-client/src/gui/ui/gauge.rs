//! Labelled progress bars for node panels.
//!
//! A gauge is a labelled track with a proportional fill and a caption on the
//! right: the shape "Memory ... 6.9 GB / 16.0 GB" over a bar. Layers use them for
//! anything bounded — memory, disk, CPU — so the same kind of quantity reads the
//! same way whichever layer draws it.
//!
//! Like the rest of the retained UI, a gauge is spawned once. [`bind_gauge`]
//! attaches a projection that [`drive_bind_gauge`] re-evaluates each frame, in
//! the same shape as [`super::bind::bind_text`].

use super::theme::{Role, Theme, ThemedBg, ThemedBorder};
use super::widgets::text;
use bevy::prelude::*;
use std::sync::Arc;

/// Height of a gauge's track, in logical pixels.
const TRACK_HEIGHT: f32 = 6.0;

/// Fill fraction past which the gauge reads as a warning.
const WARN_AT: f32 = 0.8;

/// Fill fraction past which the gauge reads as an error.
const ERROR_AT: f32 = 0.95;

/// What a gauge is showing: a fraction of its track, plus the caption beside it.
#[derive(Clone, Default, PartialEq)]
pub struct GaugeValue {
    /// Fill fraction, clamped to `0.0..=1.0` when drawn.
    pub fraction: f32,
    /// Text shown to the right of the label (e.g. `"6.9 GB / 16.0 GB"`).
    pub caption: String,
}

impl GaugeValue {
    pub fn new(fraction: f32, caption: impl Into<String>) -> Self {
        Self {
            fraction,
            caption: caption.into(),
        }
    }

    /// A gauge for `used` out of `total`, reading empty when `total` is zero.
    pub fn ratio(used: u64, total: u64, caption: impl Into<String>) -> Self {
        let fraction = if total == 0 {
            0.0
        } else {
            used as f32 / total as f32
        };
        Self::new(fraction, caption)
    }

    /// The role the fill is painted in, which is how a gauge conveys "this is
    /// getting full" without the reader parsing the caption.
    fn role(&self) -> Role {
        if self.fraction >= ERROR_AT {
            Role::Error
        } else if self.fraction >= WARN_AT {
            Role::Warn
        } else {
            Role::Accent
        }
    }
}

/// Marks a gauge root.
#[derive(Component)]
pub struct Gauge;

/// Marks the filled portion of a gauge's track.
#[derive(Component)]
pub struct GaugeFill;

/// Marks a gauge's caption label.
#[derive(Component)]
pub struct GaugeCaption;

/// A gauge whose value is produced by a projection, refreshed each frame.
#[derive(Component, Clone)]
pub struct BindGauge(pub Arc<dyn Fn() -> GaugeValue + Send + Sync>);

/// Build a [`BindGauge`] from a closure.
pub fn bind_gauge(project: impl Fn() -> GaugeValue + Send + Sync + 'static) -> BindGauge {
    BindGauge(Arc::new(project))
}

/// A labelled gauge showing `value`.
///
/// Pair it with [`bind_gauge`] on the same entity to keep it live:
/// `parent.spawn((gauge(theme, "Memory", GaugeValue::default()), bind_gauge(..)))`.
pub fn gauge(theme: &Theme, label: impl Into<String>, value: GaugeValue) -> impl Bundle {
    let role = value.role();
    let fraction = value.fraction.clamp(0.0, 1.0);
    (
        Gauge,
        Node {
            flex_direction: FlexDirection::Column,
            width: Val::Percent(100.0),
            row_gap: Val::Px(theme.metrics.space_xs),
            ..default()
        },
        children![
            // Label on the left, caption on the right.
            (
                Node {
                    flex_direction: FlexDirection::Row,
                    justify_content: JustifyContent::SpaceBetween,
                    align_items: AlignItems::Center,
                    column_gap: Val::Px(theme.metrics.space_sm),
                    width: Val::Percent(100.0),
                    ..default()
                },
                children![
                    text(theme, label, theme.metrics.font_sm, Role::Text),
                    (
                        GaugeCaption,
                        text(theme, value.caption, theme.metrics.font_sm, Role::TextMuted),
                    ),
                ],
            ),
            // Track, with the fill as its only child.
            (
                Node {
                    width: Val::Percent(100.0),
                    height: Val::Px(TRACK_HEIGHT),
                    border: UiRect::all(Val::Px(1.0)),
                    ..default()
                },
                BackgroundColor(theme.color(Role::Surface)),
                ThemedBg(Role::Surface),
                BorderColor::all(theme.color(Role::Border)),
                ThemedBorder(Role::Border),
                children![(
                    GaugeFill,
                    Node {
                        width: Val::Percent(fraction * 100.0),
                        height: Val::Percent(100.0),
                        ..default()
                    },
                    BackgroundColor(theme.color(role)),
                    // The fill's role tracks the value, so the driver below moves
                    // it between Accent/Warn/Error as the gauge fills.
                    ThemedBg(role),
                )],
            ),
        ],
    )
}

/// Re-evaluate every [`BindGauge`] and update its fill width, fill color, and
/// caption when the value changes.
pub fn drive_bind_gauge(
    theme: Res<Theme>,
    binds: Query<(&BindGauge, &Children)>,
    children: Query<&Children>,
    mut fills: Query<(&mut Node, &mut ThemedBg, &mut BackgroundColor), With<GaugeFill>>,
    mut captions: Query<&mut Text, With<GaugeCaption>>,
) {
    for (bind, gauge_children) in &binds {
        let value = (bind.0)();
        let width = Val::Percent(value.fraction.clamp(0.0, 1.0) * 100.0);
        let role = value.role();

        // A gauge's parts are grandchildren (the header row, then the track), so
        // walk one level down from each direct child rather than assuming an
        // order or a depth.
        for row in gauge_children.iter() {
            let Ok(parts) = children.get(row) else {
                continue;
            };
            for part in parts.iter() {
                if let Ok((mut node, mut themed, mut background)) = fills.get_mut(part) {
                    if node.width != width {
                        node.width = width;
                    }
                    // Painted here as well as recorded, because the recolor
                    // systems only run on a theme change and this is a value
                    // change.
                    if themed.0 != role {
                        themed.0 = role;
                        background.0 = theme.color(role);
                    }
                } else if let Ok(mut caption) = captions.get_mut(part)
                    && caption.0 != value.caption
                {
                    caption.0 = value.caption.clone();
                }
            }
        }
    }
}

/// Installs the gauge driver.
pub struct GaugePlugin;

impl Plugin for GaugePlugin {
    fn build(&self, app: &mut App) {
        app.add_systems(Update, drive_bind_gauge);
    }
}
