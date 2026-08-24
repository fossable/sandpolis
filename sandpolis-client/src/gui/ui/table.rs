//! A data-bound table for lists of rows.
//!
//! A table is a CSS-grid of text cells under an optional header row, sized so
//! columns align across rows without hardcoded widths. Layers use it for
//! anything list-shaped — database rows, users, systemd units — so tabular data
//! reads the same way whichever layer draws it.
//!
//! Like the rest of the retained UI, a table is spawned once. [`bind_table`]
//! attaches a projection in the same shape as [`super::bind::bind_text`], but
//! since projecting a table means querying a whole row set, the projection is
//! re-evaluated on a refresh interval (default 1s) rather than every frame, and
//! the cells are respawned only when the projected data actually changes.
//!
//! Rows are respawned wholesale on change rather than diffed per row; with the
//! refresh throttle and list sizes in the tens-to-hundreds this is fine.
//! Virtualized rows for large tables (e.g. installed packages) are future work.

use super::theme::{Role, Theme, ThemedBorder};
use super::widgets::text;
use bevy::prelude::*;
use std::sync::Arc;
use std::time::{Duration, Instant};

/// How often a table's projection is re-evaluated by default.
const REFRESH: Duration = Duration::from_secs(1);

/// One cell: text plus an optional role tint (`None` reads as [`Role::Text`]).
#[derive(Clone, PartialEq, Default)]
pub struct TableCell {
    pub text: String,
    pub role: Option<Role>,
}

impl TableCell {
    pub fn new(text: impl Into<String>) -> Self {
        Self {
            text: text.into(),
            role: None,
        }
    }

    pub fn with_role(mut self, role: Role) -> Self {
        self.role = Some(role);
        self
    }
}

impl From<String> for TableCell {
    fn from(text: String) -> Self {
        Self::new(text)
    }
}

impl From<&str> for TableCell {
    fn from(text: &str) -> Self {
        Self::new(text)
    }
}

/// One row of cells. Rows shorter than the column count are padded with empty
/// cells when drawn.
#[derive(Clone, PartialEq, Default)]
pub struct TableRow {
    pub cells: Vec<TableCell>,
}

impl TableRow {
    pub fn new(cells: impl IntoIterator<Item = impl Into<TableCell>>) -> Self {
        Self {
            cells: cells.into_iter().map(Into::into).collect(),
        }
    }

    /// Tint every untinted cell in the row, which is how a whole row conveys
    /// "this one needs attention" (e.g. a failed unit in [`Role::Error`]).
    pub fn with_role(mut self, role: Role) -> Self {
        for cell in &mut self.cells {
            cell.role.get_or_insert(role);
        }
        self
    }
}

/// What a table is showing: column headers plus rows.
#[derive(Clone, PartialEq, Default)]
pub struct TableData {
    /// Header labels; empty means a headerless table.
    pub columns: Vec<String>,
    pub rows: Vec<TableRow>,
    /// Muted line shown instead of the grid while `rows` is empty.
    pub placeholder: String,
}

impl TableData {
    pub fn new(columns: impl IntoIterator<Item = impl Into<String>>) -> Self {
        Self {
            columns: columns.into_iter().map(Into::into).collect(),
            ..default()
        }
    }

    /// A headerless two-column table of key/value pairs.
    pub fn key_value(pairs: impl IntoIterator<Item = (String, String)>) -> Self {
        Self {
            rows: pairs
                .into_iter()
                .map(|(k, v)| TableRow::new([k, v]))
                .collect(),
            ..default()
        }
    }

    pub fn push_row(&mut self, row: TableRow) {
        self.rows.push(row);
    }

    pub fn with_placeholder(mut self, placeholder: impl Into<String>) -> Self {
        self.placeholder = placeholder.into();
        self
    }

    /// How many columns the grid needs: the wider of the header and the widest
    /// row, never zero.
    fn column_count(&self) -> usize {
        self.columns
            .len()
            .max(
                self.rows
                    .iter()
                    .map(|row| row.cells.len())
                    .max()
                    .unwrap_or(0),
            )
            .max(1)
    }
}

/// Marks the grid node whose children are the cells.
#[derive(Component)]
pub struct TableGrid;

/// A table whose data is produced by a projection, refreshed on an interval.
#[derive(Component)]
pub struct BindTable {
    project: Arc<dyn Fn() -> TableData + Send + Sync>,
    refresh: Duration,
    /// Last projected data; cells are respawned only when this changes.
    last: Option<TableData>,
    next_refresh: Option<Instant>,
}

/// Build a [`BindTable`] from a closure.
pub fn bind_table(project: impl Fn() -> TableData + Send + Sync + 'static) -> BindTable {
    BindTable {
        project: Arc::new(project),
        refresh: REFRESH,
        last: None,
        // Unset so the first frame populates immediately.
        next_refresh: None,
    }
}

impl BindTable {
    pub fn with_refresh(mut self, refresh: Duration) -> Self {
        self.refresh = refresh;
        self
    }
}

/// A table root.
///
/// Pair it with [`bind_table`] on the same entity to fill and update it:
/// `parent.spawn((table(theme, None), bind_table(..)))`.
///
/// With `max_height` the table owns vertical scrolling (for hosts that don't
/// scroll themselves); with `None` it grows to its content and the nearest
/// scrolling ancestor (e.g. a node panel body) handles overflow.
pub fn table(theme: &Theme, max_height: Option<f32>) -> impl Bundle {
    let mut root = Node {
        flex_direction: FlexDirection::Column,
        width: Val::Percent(100.0),
        ..default()
    };
    if let Some(height) = max_height {
        root.max_height = Val::Px(height);
        root.min_height = Val::Px(0.0);
        root.overflow = Overflow::scroll_y();
    }
    (
        root,
        children![(
            TableGrid,
            Node {
                display: Display::Grid,
                width: Val::Percent(100.0),
                column_gap: Val::Px(theme.metrics.space_md),
                row_gap: Val::Px(theme.metrics.space_xs),
                ..default()
            },
        )],
    )
}

/// Re-evaluate due [`BindTable`] projections and respawn the cells of any table
/// whose data changed.
pub fn drive_bind_table(
    mut commands: Commands,
    theme: Res<Theme>,
    mut tables: Query<(&mut BindTable, &Children)>,
    mut grids: Query<&mut Node, With<TableGrid>>,
) {
    let now = Instant::now();
    for (mut bind, children) in &mut tables {
        if bind.next_refresh.is_some_and(|at| now < at) {
            continue;
        }
        bind.next_refresh = Some(now + bind.refresh);

        let data = (bind.project)();
        if bind.last.as_ref() == Some(&data) {
            continue;
        }

        let Some(grid) = children.iter().find(|child| grids.contains(*child)) else {
            continue;
        };
        let columns = data.column_count();
        if let Ok(mut node) = grids.get_mut(grid) {
            node.grid_template_columns = RepeatedGridTrack::auto(columns as u16);
        }

        commands.entity(grid).despawn_related::<Children>();
        commands.entity(grid).with_children(|grid| {
            if data.rows.is_empty() {
                grid.spawn((
                    Node {
                        grid_column: GridPlacement::span(columns as u16),
                        ..default()
                    },
                    children![text(
                        &theme,
                        data.placeholder.clone(),
                        theme.metrics.font_sm,
                        Role::TextMuted,
                    )],
                ));
            } else {
                for label in &data.columns {
                    grid.spawn(header_cell(&theme, label));
                }
                for row in &data.rows {
                    for index in 0..columns {
                        let cell = row.cells.get(index).cloned().unwrap_or_default();
                        grid.spawn(body_cell(&theme, cell));
                    }
                }
            }
        });

        bind.last = Some(data);
    }
}

/// A header cell: muted label over the column, separated by a bottom border.
fn header_cell(theme: &Theme, label: &str) -> impl Bundle {
    (
        Node {
            border: UiRect::bottom(Val::Px(1.0)),
            padding: UiRect::bottom(Val::Px(theme.metrics.space_xs)),
            overflow: Overflow::clip(),
            ..default()
        },
        BorderColor::all(theme.color(Role::Border)),
        ThemedBorder(Role::Border),
        children![text(theme, label, theme.metrics.font_sm, Role::TextMuted)],
    )
}

/// A body cell in the cell's role, clipped rather than wrapped when long.
fn body_cell(theme: &Theme, cell: TableCell) -> impl Bundle {
    (
        Node {
            overflow: Overflow::clip(),
            ..default()
        },
        children![text(
            theme,
            cell.text,
            theme.metrics.font_sm,
            cell.role.unwrap_or(Role::Text),
        )],
    )
}

/// Installs the table driver.
pub struct TablePlugin;

impl Plugin for TablePlugin {
    fn build(&self, app: &mut App) {
        app.add_systems(Update, drive_bind_table);
    }
}
