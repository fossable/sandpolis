//! Generic database viewer.
//!
//! Lists every `#[data]` table registered in
//! [`sandpolis_instance::database::browse::BROWSE`] with live row counts, an
//! instance filter (whole database or a single instance's rows), and the
//! selected table's rows pretty-printed as JSON. Since `bind_text` closures run
//! every frame, all database reads sit behind ~1s caches.

use crate::gui::ui::Activate;
use crate::gui::ui::bind::bind_text;
use crate::gui::ui::panel::{PanelClosed, spawn_floating_panel};
use crate::gui::ui::theme::{Role, Theme, ThemedBorder, ThemedButton};
use crate::gui::ui::widgets::{row, text};
use bevy::ecs::world::CommandQueue;
use bevy::prelude::*;
use bevy_ui_widgets::Button;
use sandpolis_instance::InstanceId;
use sandpolis_instance::database::RealmDatabase;
use sandpolis_instance::database::browse::BROWSE;
use sandpolis_instance::realm::RealmName;
use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use std::time::{Duration, Instant};

/// Marker for the (single) browser panel root.
#[derive(Component)]
pub struct DatabaseBrowserPanel;

/// Minimum age before a cached query is re-run.
const REFRESH: Duration = Duration::from_secs(1);

#[derive(Default)]
struct BrowserState {
    /// Selected table: (model_id, display name).
    selected: Option<(u32, &'static str)>,
    /// `None` = whole database.
    instance: Option<InstanceId>,
    /// Cached rows text for `(model_id, instance)`.
    rows_cache: Option<(Instant, (u32, Option<InstanceId>), String)>,
    /// Cached per-table count labels, refreshed together.
    counts_cache: Option<(Instant, Option<InstanceId>, HashMap<u32, String>)>,
}

type Shared = Arc<Mutex<BrowserState>>;

/// The client's default-realm database, if sync has been initialized.
fn realm_db() -> Option<RealmDatabase> {
    crate::sync::client_database()?
        .realm(RealmName::default())
        .ok()
}

/// Open the database browser (no-op if one is already open).
pub fn open(commands: &mut Commands) {
    // The toolbar callback only carries `Commands`, so reach the `Theme` and
    // window size through a world command (same pattern as the toolbar itself).
    commands.queue(|world: &mut World| {
        if world
            .query_filtered::<(), With<DatabaseBrowserPanel>>()
            .iter(world)
            .next()
            .is_some()
        {
            return;
        }
        let theme = world.resource::<Theme>().clone();
        let (win_w, win_h) = world
            .query::<&Window>()
            .iter(world)
            .next()
            .map(|w| (w.width(), w.height()))
            .unwrap_or((1280.0, 720.0));
        let size = Vec2::new((win_w * 0.8).min(900.0), (win_h * 0.8).min(600.0));
        let pos = Vec2::new((win_w - size.x) / 2.0, (win_h - size.y) / 2.0);

        let mut queue = CommandQueue::default();
        let mut commands = Commands::new(&mut queue, world);
        let panel = spawn_floating_panel(&mut commands, &theme, "Database", pos, size);
        commands
            .entity(panel.root)
            .insert(DatabaseBrowserPanel)
            // The panel's close button only triggers `PanelClosed`; the host
            // is responsible for despawning.
            .observe(|closed: On<PanelClosed>, mut commands: Commands| {
                commands.entity(closed.entity).despawn();
            });
        build_body(&mut commands, panel.body, &theme);
        queue.apply(world);
    });
}

/// A themed button with no label child, so callers can attach a live
/// [`bind_text`] label instead of the static one baked into `widgets::button`.
fn blank_button(theme: &Theme) -> impl Bundle {
    (
        Button,
        ThemedButton,
        Interaction::default(),
        Node {
            padding: UiRect::axes(
                Val::Px(theme.metrics.space_md),
                Val::Px(theme.metrics.space_sm),
            ),
            align_items: AlignItems::Center,
            justify_content: JustifyContent::Center,
            border: UiRect::all(Val::Px(1.0)),
            ..default()
        },
        BackgroundColor(theme.color(Role::Surface)),
        BorderColor::all(theme.color(Role::Border)),
        ThemedBorder(Role::Border),
    )
}

fn build_body(commands: &mut Commands, body: Entity, theme: &Theme) {
    let state: Shared = Arc::new(Mutex::new(BrowserState::default()));
    let tables = BROWSE.tables();

    commands.entity(body).with_children(|p| {
        // Filter row: a single button whose live label shows the current
        // instance filter; clicking cycles All -> each known instance -> All.
        p.spawn(row(theme.metrics.space_sm)).with_children(|r| {
            let click_state = state.clone();
            let label_state = state.clone();
            r.spawn(blank_button(theme))
                .observe(move |_: On<Activate>| cycle_instance(&click_state))
                .with_child((
                    text(theme, "", theme.metrics.font_md, Role::Text),
                    bind_text(move || instance_label(&label_state)),
                ));
        });

        // Content: table list on the left, selected table's rows on the right.
        p.spawn(Node {
            flex_direction: FlexDirection::Row,
            column_gap: Val::Px(theme.metrics.space_md),
            flex_grow: 1.0,
            overflow: Overflow::clip(),
            ..default()
        })
        .with_children(|content| {
            content
                .spawn(Node {
                    flex_direction: FlexDirection::Column,
                    row_gap: Val::Px(theme.metrics.space_xs),
                    width: Val::Px(260.0),
                    flex_shrink: 0.0,
                    overflow: Overflow::scroll_y(),
                    ..default()
                })
                .with_children(|list| {
                    for (model_id, name) in tables {
                        let select = state.clone();
                        let label = state.clone();
                        list.spawn(blank_button(theme))
                            .observe(move |_: On<Activate>| {
                                select.lock().unwrap().selected = Some((model_id, name));
                            })
                            .with_child((
                                text(theme, "", theme.metrics.font_sm, Role::Text),
                                bind_text(move || table_label(&label, model_id, name)),
                            ));
                    }
                });

            content
                .spawn(Node {
                    flex_direction: FlexDirection::Column,
                    flex_grow: 1.0,
                    overflow: Overflow::scroll_y(),
                    ..default()
                })
                .with_children(|pane| {
                    let rows_state = state.clone();
                    pane.spawn((
                        text(theme, "", theme.metrics.font_sm, Role::TextMuted),
                        bind_text(move || rows_text(&rows_state)),
                    ));
                });
        });
    });
}

/// Advance the instance filter: All -> id1 -> id2 -> ... -> All.
fn cycle_instance(state: &Shared) {
    let known = realm_db()
        .map(|db| BROWSE.instances(&db))
        .unwrap_or_default();
    let mut s = state.lock().unwrap();
    s.instance = match s.instance {
        None => known.first().copied(),
        Some(current) => known
            .iter()
            .position(|id| *id == current)
            .and_then(|i| known.get(i + 1))
            .copied(),
    };
}

fn instance_label(state: &Shared) -> String {
    match state.lock().unwrap().instance {
        Some(id) => format!("Instance: {id}"),
        None => "Instance: All (whole database)".into(),
    }
}

/// "Name (count)" for the table list. All counts refresh together at most once
/// per [`REFRESH`]; tables that can't be scanned (e.g. not defined in the
/// database's models) show "?".
fn table_label(state: &Shared, model_id: u32, name: &str) -> String {
    let mut s = state.lock().unwrap();
    let stale = match &s.counts_cache {
        Some((at, instance, _)) => at.elapsed() >= REFRESH || *instance != s.instance,
        None => true,
    };
    if stale {
        let instance = s.instance;
        let mut counts = HashMap::new();
        if let Some(db) = realm_db() {
            for (id, _) in BROWSE.tables() {
                counts.insert(
                    id,
                    match BROWSE.count(&db, id, instance) {
                        Ok(n) => n.to_string(),
                        Err(_) => "?".into(),
                    },
                );
            }
        }
        s.counts_cache = Some((Instant::now(), instance, counts));
    }
    let count = s
        .counts_cache
        .as_ref()
        .and_then(|(_, _, counts)| counts.get(&model_id).cloned())
        .unwrap_or_else(|| "?".into());
    format!("{name} ({count})")
}

/// Pretty-printed JSON rows of the selected table, cached for [`REFRESH`].
fn rows_text(state: &Shared) -> String {
    let mut s = state.lock().unwrap();
    let Some((model_id, name)) = s.selected else {
        return "Select a table to view its rows.".into();
    };
    let key = (model_id, s.instance);
    if let Some((at, cached_key, cached)) = &s.rows_cache {
        if *cached_key == key && at.elapsed() < REFRESH {
            return cached.clone();
        }
    }
    let rendered = match realm_db() {
        None => "Database not initialized.".into(),
        Some(db) => match BROWSE.rows(&db, model_id, s.instance) {
            Err(e) => format!("{name}: query failed: {e}"),
            Ok(rows) => {
                let mut out = format!("{name}: {} row(s)\n\n", rows.len());
                for row in &rows {
                    out.push_str(&serde_json::to_string_pretty(row).unwrap_or_default());
                    out.push_str("\n\n");
                }
                out
            }
        },
    };
    s.rows_cache = Some((Instant::now(), key, rendered.clone()));
    rendered
}
