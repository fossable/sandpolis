//! Labelled line charts for node panels.
//!
//! A chart is a labelled plot of a time series over a fixed trailing window,
//! with a caption for the current value on the right: the shape
//! "CPU ... 42%" over a line. Layers use them for anything sampled over time —
//! CPU usage, memory — so history reads the same way whichever layer draws it.
//!
//! Like the rest of the retained UI, a chart is spawned once. [`bind_chart`]
//! attaches a projection that [`drive_bind_chart`] re-evaluates on a refresh
//! interval, in the same shape as [`super::table::bind_table`]. The projection
//! returns the whole series (typically replicated revision history queried from
//! the client database), so history survives the panel being closed and
//! reopened.
//!
//! The plot itself is rasterized into an [`Image`] shown by an [`ImageNode`]:
//! the texture holds only the line and its area fill on a transparent
//! background, so the frame recolors through the normal [`ThemedBg`] path and
//! only the line waits for the next redraw after a theme change.

use super::theme::{Role, Theme, ThemedBg, ThemedBorder};
use super::widgets::text;
use bevy::asset::RenderAssetUsages;
use bevy::prelude::*;
use bevy::render::render_resource::{Extent3d, TextureDimension, TextureFormat};
use std::sync::Arc;
use std::time::{Duration, Instant, SystemTime};

/// How often a chart's projection is re-evaluated by default.
const REFRESH: Duration = Duration::from_secs(1);

/// How much trailing history a chart shows by default.
const WINDOW: Duration = Duration::from_secs(15 * 60);

/// Adjacent samples further apart than this are not connected: the source
/// stopped reporting (agent offline), and drawing through the gap would invent
/// data. Sized to clear the slowest regular sampler (memory at 30s) with room.
const MAX_GAP: Duration = Duration::from_secs(90);

/// Height of a chart's plot, in logical pixels.
const PLOT_HEIGHT: f32 = 60.0;

/// Plot texture size. Rendered at 2x the logical size so the line stays crisp
/// when the node is scaled.
const TEX_WIDTH: usize = 480;
const TEX_HEIGHT: usize = 120;

/// Line thickness in texture pixels (2x scale).
const LINE_PX: usize = 3;

/// Alpha of the area fill under the line, out of 255.
const AREA_ALPHA: u8 = 38;

/// What a chart is showing: samples over time, plus the caption beside it.
#[derive(Clone, Default, PartialEq)]
pub struct ChartSeries {
    /// `(when, value)` samples, oldest first; values are clamped to
    /// `0.0..=1.0` when drawn.
    pub points: Vec<(SystemTime, f32)>,
    /// Text shown to the right of the label (e.g. `"42%"`).
    pub caption: String,
}

impl ChartSeries {
    pub fn new(points: Vec<(SystemTime, f32)>, caption: impl Into<String>) -> Self {
        Self {
            points,
            caption: caption.into(),
        }
    }

    /// An empty series with just a caption (e.g. `"No data"`).
    pub fn empty(caption: impl Into<String>) -> Self {
        Self::new(Vec::new(), caption)
    }
}

/// Marks a chart root.
#[derive(Component)]
pub struct Chart;

/// Marks the image node the plot is rasterized into.
#[derive(Component)]
pub struct ChartPlot;

/// Marks a chart's caption label.
#[derive(Component)]
pub struct ChartCaption;

/// A chart whose series is produced by a projection, refreshed on an interval.
#[derive(Component)]
pub struct BindChart {
    project: Arc<dyn Fn() -> ChartSeries + Send + Sync>,
    refresh: Duration,
    /// Trailing time window the plot covers, ending at "now".
    window: Duration,
    /// Role the line is painted in. Deliberately not escalated to Warn/Error
    /// with the value the way a gauge is: the adjacent gauge already conveys
    /// pressure, and a line changing color mid-history would misread.
    role: Role,
    /// Last projected series; the plot is redrawn only when this changes.
    last: Option<ChartSeries>,
    next_refresh: Option<Instant>,
}

/// Build a [`BindChart`] from a closure.
pub fn bind_chart(project: impl Fn() -> ChartSeries + Send + Sync + 'static) -> BindChart {
    BindChart {
        project: Arc::new(project),
        refresh: REFRESH,
        window: WINDOW,
        role: Role::Accent,
        last: None,
        // Unset so the first frame populates immediately.
        next_refresh: None,
    }
}

impl BindChart {
    pub fn with_refresh(mut self, refresh: Duration) -> Self {
        self.refresh = refresh;
        self
    }

    pub fn with_window(mut self, window: Duration) -> Self {
        self.window = window;
        self
    }

    pub fn with_role(mut self, role: Role) -> Self {
        self.role = role;
        self
    }
}

/// A labelled chart.
///
/// Pair it with [`bind_chart`] on the same entity to fill and update it:
/// `parent.spawn((chart(theme, "Usage"), bind_chart(..)))`.
pub fn chart(theme: &Theme, label: impl Into<String>) -> impl Bundle {
    (
        Chart,
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
                        ChartCaption,
                        text(theme, "", theme.metrics.font_sm, Role::TextMuted),
                    ),
                ],
            ),
            // Plot frame, with the rasterized plot as its only child. The
            // texture handle is allocated lazily by the driver, since spawn
            // sites don't have `Assets<Image>` access.
            (
                Node {
                    width: Val::Percent(100.0),
                    height: Val::Px(PLOT_HEIGHT),
                    border: UiRect::all(Val::Px(1.0)),
                    ..default()
                },
                BackgroundColor(theme.color(Role::Surface)),
                ThemedBg(Role::Surface),
                BorderColor::all(theme.color(Role::Border)),
                ThemedBorder(Role::Border),
                children![(
                    ChartPlot,
                    Node {
                        width: Val::Percent(100.0),
                        height: Val::Percent(100.0),
                        ..default()
                    },
                    ImageNode::default(),
                )],
            ),
        ],
    )
}

/// Re-evaluate due [`BindChart`] projections and redraw the plot of any chart
/// whose series changed (or whose line color did, via a theme change).
pub fn drive_bind_chart(
    theme: Res<Theme>,
    mut images: ResMut<Assets<Image>>,
    mut charts: Query<(&mut BindChart, &Children)>,
    children: Query<&Children>,
    mut plots: Query<&mut ImageNode, With<ChartPlot>>,
    mut captions: Query<&mut Text, With<ChartCaption>>,
) {
    let now = Instant::now();
    let retheme = theme.is_changed();
    for (mut bind, chart_children) in &mut charts {
        let due = bind.next_refresh.is_none_or(|at| now >= at);
        if !due && !retheme {
            continue;
        }

        if due {
            bind.next_refresh = Some(now + bind.refresh);
            let series = (bind.project)();
            if bind.last.as_ref() == Some(&series) && !retheme {
                continue;
            }
            bind.last = Some(series);
        }
        let Some(series) = bind.last.as_ref() else {
            continue;
        };

        let buffer = rasterize(series, bind.window, theme.color(bind.role));

        // A chart's parts are grandchildren (the header row, then the plot
        // frame), so walk one level down from each direct child rather than
        // assuming an order or a depth.
        for row in chart_children.iter() {
            let Ok(parts) = children.get(row) else {
                continue;
            };
            for part in parts.iter() {
                if let Ok(mut plot) = plots.get_mut(part) {
                    if plot.image == Handle::default() {
                        plot.image = images.add(plot_image(buffer.clone()));
                    } else if let Some(mut image) = images.get_mut(&plot.image) {
                        image.data = Some(buffer.clone());
                    }
                } else if let Ok(mut caption) = captions.get_mut(part)
                    && caption.0 != series.caption
                {
                    caption.0 = series.caption.clone();
                }
            }
        }
    }
}

/// Wrap a plot buffer as an [`Image`] for an [`ImageNode`].
fn plot_image(rgba: Vec<u8>) -> Image {
    Image::new(
        Extent3d {
            width: TEX_WIDTH as u32,
            height: TEX_HEIGHT as u32,
            depth_or_array_layers: 1,
        },
        TextureDimension::D2,
        rgba,
        TextureFormat::Rgba8UnormSrgb,
        RenderAssetUsages::RENDER_WORLD | RenderAssetUsages::MAIN_WORLD,
    )
}

/// Draw `series` over its trailing `window` as a straight-alpha RGBA buffer:
/// transparent background, a line in `color` with a faint area fill below it.
fn rasterize(series: &ChartSeries, window: Duration, color: Color) -> Vec<u8> {
    let mut buffer = vec![0u8; TEX_WIDTH * TEX_HEIGHT * 4];

    let line = color.to_srgba();
    let (r, g, b) = (
        (line.red * 255.0) as u8,
        (line.green * 255.0) as u8,
        (line.blue * 255.0) as u8,
    );

    let columns = sample_columns(&series.points, window);
    let y_of = |value: f32| (1.0 - value.clamp(0.0, 1.0)) * (TEX_HEIGHT - 1) as f32;
    let mut put = |x: usize, y: usize, alpha: u8| {
        let index = (y * TEX_WIDTH + x) * 4;
        buffer[index..index + 4].copy_from_slice(&[r, g, b, alpha]);
    };

    for x in 0..TEX_WIDTH {
        let Some(value) = columns[x] else {
            continue;
        };
        let y = y_of(value);

        // Area fill from the line to the baseline.
        for py in (y as usize)..TEX_HEIGHT {
            put(x, py, AREA_ALPHA);
        }

        // The line itself, stretched to meet the next column so steep slopes
        // stay connected.
        let y_next = match columns.get(x + 1).copied().flatten() {
            Some(next) => y_of(next),
            None => y,
        };
        let (top, bottom) = if y <= y_next { (y, y_next) } else { (y_next, y) };
        let bottom = (bottom as usize + LINE_PX - 1).min(TEX_HEIGHT - 1);
        for py in (top as usize)..=bottom {
            put(x, py, 255);
        }
    }

    buffer
}

/// The interpolated value under each texture column, or `None` where the
/// window isn't covered: before the first sample, after a stale last sample,
/// or across a reporting gap (see [`MAX_GAP`]). A fresh last sample is held
/// flat to the right edge so a live chart reaches "now".
fn sample_columns(points: &[(SystemTime, f32)], window: Duration) -> Vec<Option<f32>> {
    let mut columns = vec![None; TEX_WIDTH];
    if points.is_empty() {
        return columns;
    }
    let Some(start) = SystemTime::now().checked_sub(window) else {
        return columns;
    };

    let mut i = 0;
    for (x, column) in columns.iter_mut().enumerate() {
        let t = start + window.mul_f64(x as f64 / (TEX_WIDTH - 1) as f64);
        while i + 1 < points.len() && points[i + 1].0 <= t {
            i += 1;
        }

        let (t0, v0) = points[i];
        *column = if t0 > t {
            // Before the first sample in the window.
            None
        } else if let Some(&(t1, v1)) = points.get(i + 1)
            && t1.duration_since(t0).unwrap_or_default() <= MAX_GAP
        {
            let span = t1.duration_since(t0).unwrap_or_default();
            let f = t.duration_since(t0).unwrap_or_default().as_secs_f64()
                / span.as_secs_f64().max(f64::EPSILON);
            Some(v0 + (v1 - v0) * (f as f32))
        } else if t.duration_since(t0).unwrap_or_default() <= MAX_GAP {
            // Held flat past the sample — to "now" when it's the last one, or
            // into a reporting gap before breaking.
            Some(v0)
        } else {
            None
        };
    }
    columns
}

/// Installs the chart driver.
pub struct ChartPlugin;

impl Plugin for ChartPlugin {
    fn build(&self, app: &mut App) {
        app.add_systems(Update, drive_bind_chart);
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn ago(secs: u64) -> SystemTime {
        SystemTime::now() - Duration::from_secs(secs)
    }

    #[test]
    fn empty_series_is_all_gaps() {
        let columns = sample_columns(&[], WINDOW);
        assert!(columns.iter().all(|c| c.is_none()));
    }

    #[test]
    fn fresh_sample_holds_flat_to_the_right_edge() {
        let columns = sample_columns(&[(ago(10), 0.5)], WINDOW);
        assert_eq!(columns[TEX_WIDTH - 1], Some(0.5));
        // The window starts long before the sample, so the left is empty.
        assert_eq!(columns[0], None);
    }

    #[test]
    fn stale_sample_does_not_reach_the_right_edge() {
        let columns = sample_columns(&[(ago(WINDOW.as_secs() / 2), 0.5)], WINDOW);
        assert_eq!(columns[TEX_WIDTH - 1], None);
        // But it is held flat for MAX_GAP after its own time.
        let x = |secs_ago: u64| {
            let offset = WINDOW.as_secs() - secs_ago;
            (offset as f64 / WINDOW.as_secs() as f64 * (TEX_WIDTH - 1) as f64) as usize
        };
        assert_eq!(
            columns[x(WINDOW.as_secs() / 2 - MAX_GAP.as_secs() / 2)],
            Some(0.5)
        );
    }

    #[test]
    fn adjacent_samples_interpolate() {
        let columns = sample_columns(&[(ago(60), 0.0), (ago(10), 1.0)], WINDOW);
        let values: Vec<f32> = columns.iter().flatten().copied().collect();
        assert!(!values.is_empty());
        // Monotonic climb from 0 toward 1 across the covered columns.
        for pair in values.windows(2) {
            assert!(pair[1] >= pair[0] - f32::EPSILON);
        }
        assert!(values.last().copied().unwrap() > 0.9);
    }

    #[test]
    fn reporting_gaps_are_not_bridged() {
        let far = WINDOW.as_secs() - 60;
        let columns = sample_columns(&[(ago(far), 0.2), (ago(10), 0.8)], WINDOW);
        // Somewhere between the two distant samples the line must break.
        let middle = &columns[TEX_WIDTH / 4..TEX_WIDTH / 2];
        assert!(middle.iter().any(|c| c.is_none()));
        // While both samples themselves are still drawn.
        assert!(columns.iter().any(|c| *c == Some(0.2)));
        assert_eq!(columns[TEX_WIDTH - 1], Some(0.8));
    }
}
