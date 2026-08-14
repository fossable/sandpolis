//! The node panel abstraction layers build on.
//!
//! Every visible node in the world view carries a panel anchored beneath it. The
//! host in [`crate::gui::node_panel`] owns the chrome — the identity line, the
//! pin and close buttons, the anchoring — and calls into the active layer's
//! [`NodePanel`] for the body.
//!
//! A panel has two bodies. The collapsed one is a read-only summary whose depth
//! follows the world view's zoom ([`Verbosity`]); the expanded one is the full
//! detail view, built when the node is the only selected node. Both are spawned
//! once rather than redrawn per frame, so live values come from [`super::bind`]
//! and [`super::gauge`].

use super::theme::Theme;
use bevy::prelude::*;
use sandpolis_instance::InstanceId;

/// What a node panel describes.
///
/// This is a *stable* identity rather than the node's `Entity`, because layers
/// key their live sessions on it and a node entity can be despawned and
/// respawned (e.g. when an instance reconnects) without the session it stands
/// for going away.
///
/// Most nodes *are* an instance, so `sub` is `None`. Nodes carrying a
/// [`SubNode`](crate::gui::node::SubNode) stand in for something finer-grained:
/// a probe device borrows its gateway server's `InstanceId` and only the sub key
/// separates the two, while an account node has no instance at all.
#[derive(Clone, Copy, PartialEq, Eq, Hash, Debug)]
pub struct PanelTarget {
    /// The instance this node stands for, when it is one.
    pub instance: Option<InstanceId>,
    /// The node's [`SubNode`](crate::gui::node::SubNode) id, when the node isn't
    /// an instance itself.
    pub sub: Option<u64>,
}

impl PanelTarget {
    /// A target for a plain instance node.
    pub fn instance(instance: InstanceId) -> Self {
        Self {
            instance: Some(instance),
            sub: None,
        }
    }
}

/// How much detail a collapsed panel shows, derived from how far the world view
/// is zoomed in.
#[derive(Clone, Copy, PartialEq, Eq, Debug, Default)]
pub enum Verbosity {
    /// Zoomed out: the node's identity and nothing else.
    Minimal,
    /// The default: identity plus a one-line summary.
    #[default]
    Normal,
    /// Zoomed in: identity plus everything the summary has to say.
    Detailed,
}

impl Verbosity {
    /// Whether this level shows anything beyond the identity line.
    pub fn shows_summary(self) -> bool {
        self != Verbosity::Minimal
    }

    /// Whether this level shows the summary's fuller form.
    pub fn is_detailed(self) -> bool {
        self == Verbosity::Detailed
    }
}

/// Everything a [`NodePanel`] needs to build its body.
pub struct PanelCtx<'a, 'w, 's> {
    pub commands: &'a mut Commands<'w, 's>,
    /// Spawn the body as children of this entity.
    pub body: Entity,
    /// The world entity of the node being described. Useful for reading
    /// layer-owned components off the node; note it is not stable across
    /// respawns, so don't key anything long-lived on it — use
    /// [`target`](Self::target).
    pub node: Entity,
    pub target: PanelTarget,
    pub theme: &'a Theme,
    /// How verbose the collapsed body should be. Always
    /// [`Verbosity::Detailed`] for an expanded panel.
    pub verbosity: Verbosity,
}

impl PanelCtx<'_, '_, '_> {
    /// Spawn the body's children.
    ///
    /// Shorthand for `ctx.commands.entity(ctx.body).with_children(..)`, which is
    /// how nearly every panel starts.
    pub fn children(&mut self, spawn: impl FnOnce(&mut ChildSpawnerCommands)) {
        self.commands.entity(self.body).with_children(spawn);
    }
}

/// A layer's contribution to the node panel.
///
/// To react to clicks in [`build_detail`](NodePanel::build_detail), attach an
/// observer to a spawned button capturing `target`, e.g.
/// `child.spawn(button(theme, "Go")).observe(move |_: On<Activate>, ...| { .. })`.
/// For live-updating labels use [`super::bind::bind_text`], and for live
/// progress bars [`super::gauge::bind_gauge`].
pub trait NodePanel: Send + Sync + 'static {
    /// Build the collapsed body: a read-only summary, scaled to
    /// [`PanelCtx::verbosity`]. Interactive controls belong in
    /// [`build_detail`](NodePanel::build_detail) — a collapsed panel is a label,
    /// not a control surface.
    fn build_summary(&self, ctx: &mut PanelCtx) {
        let _ = ctx;
    }

    /// Build the expanded body: full detail and actions.
    ///
    /// Anything this node's panel exists to show should start here rather than
    /// behind a button — a shell session, a desktop stream, a camera feed. The
    /// panel expanding *is* the request to see it.
    fn build_detail(&self, ctx: &mut PanelCtx);

    /// Tear down whatever [`build_detail`](NodePanel::build_detail) started, when
    /// the panel collapses or the node goes away.
    ///
    /// The default keeps everything, which is right for sessions that hold state
    /// worth reattaching to (a terminal); streams that can be reopened from
    /// scratch should stop here instead of running unwatched.
    fn on_collapse(&self, commands: &mut Commands, target: PanelTarget) {
        let _ = (commands, target);
    }
}
