//! Retained-mode node controller abstraction.
//!
//! This replaces the old immediate-mode `LayerGuiExtension::render_controller`.
//! A layer provides a [`NodeController`] whose [`NodeController::build`] spawns a
//! `bevy_ui` subtree once (when the controller opens), rather than redrawing every
//! frame. Live values come from [`super::bind`] components.
//!
//! Layers register themselves with [`RegisterLayerClient::register_layer_client`]
//! from their `LayerClientPlugin`, replacing the compile-time `inventory`
//! collection. The [`LayerRegistry`] resource then drives the controller host, the
//! layer picker, and per-layer node visibility.

use super::theme::Theme;
use bevy::prelude::*;
use sandpolis_instance::{InstanceId, InstanceType, LayerName};
use std::sync::Arc;

/// A layer's retained-mode controller.
///
/// [`build`](NodeController::build) is called once when the controller panel opens
/// for an instance; it should spawn the body UI as children of `body`. To react to
/// clicks, attach an observer to a spawned button capturing `instance`, e.g.
/// `child.spawn(button(theme, "Go")).observe(move |_: On<Activate>, ...| { .. })`.
/// For live-updating labels use [`super::bind::bind_text`].
pub trait NodeController: Send + Sync + 'static {
    /// Title shown in the controller panel's titlebar.
    fn title(&self) -> &str;

    /// Build the controller body as children of `body`.
    fn build(&self, commands: &mut Commands, body: Entity, instance: InstanceId, theme: &Theme);
}

/// Callback run when a layer toolbar button is clicked. It receives `Commands`
/// so it can queue work (e.g. open a dialog) without the registry needing to know
/// any layer-specific resource types.
pub type ToolbarCallback = Arc<dyn Fn(&mut Commands) + Send + Sync>;

/// Predicate deciding whether a toolbar button is currently enabled. It reads the
/// `World` (e.g. a layer-specific selection resource) so the registry needs no
/// knowledge of layer-specific types. Evaluated every frame.
pub type ToolbarEnabledFn = Arc<dyn Fn(&World) -> bool + Send + Sync>;

/// A single button shown in the layer toolbar while a layer is active.
#[derive(Clone)]
pub struct ToolbarAction {
    /// Full-text label, shown as the button's hover tooltip.
    pub label: &'static str,
    /// SVG icon path under the icon asset root (e.g. `"toolbar/login.svg"`).
    pub icon: &'static str,
    /// Invoked when the button is clicked.
    pub on_click: ToolbarCallback,
    /// Whether the button is currently enabled. Disabled buttons are dimmed and
    /// ignore clicks. Defaults to always-enabled.
    pub enabled: ToolbarEnabledFn,
}

/// Everything the client needs to know about a layer, registered by its
/// `LayerClientPlugin`. Replaces the old `LayerGuiExtension` trait object.
#[derive(Clone)]
pub struct LayerClientInfo {
    /// The layer this describes.
    pub layer: LayerName,
    /// One-line description (shown in the layer picker).
    pub description: &'static str,
    /// Which instance types are visible while this layer is active.
    pub visible_instance_types: &'static [InstanceType],
    /// Whether probe nodes are shown while this layer is active (only Probe).
    pub show_probe_nodes: bool,
    /// The layer's node controller, if it has one.
    pub controller: Option<Arc<dyn NodeController>>,
    /// Buttons shown in the layer toolbar while this layer is active.
    pub toolbar_actions: Vec<ToolbarAction>,
}

impl LayerClientInfo {
    /// Create an info for `layer` with sensible defaults (servers + agents visible,
    /// no probes, no controller).
    pub fn new(layer: impl Into<LayerName>, description: &'static str) -> Self {
        Self {
            layer: layer.into(),
            description,
            visible_instance_types: &[InstanceType::Server, InstanceType::Agent],
            show_probe_nodes: false,
            controller: None,
            toolbar_actions: Vec::new(),
        }
    }

    /// Attach a controller.
    pub fn with_controller(mut self, controller: impl NodeController) -> Self {
        self.controller = Some(Arc::new(controller));
        self
    }

    /// Add a button to this layer's toolbar.
    pub fn with_toolbar_action(
        mut self,
        label: &'static str,
        icon: &'static str,
        on_click: impl Fn(&mut Commands) + Send + Sync + 'static,
    ) -> Self {
        self.toolbar_actions.push(ToolbarAction {
            label,
            icon,
            on_click: Arc::new(on_click),
            enabled: Arc::new(|_| true),
        });
        self
    }

    /// Add a toolbar button whose enabled state is decided each frame by
    /// `enabled` (e.g. a button active only while something is selected).
    pub fn with_toolbar_action_gated(
        mut self,
        label: &'static str,
        icon: &'static str,
        on_click: impl Fn(&mut Commands) + Send + Sync + 'static,
        enabled: impl Fn(&World) -> bool + Send + Sync + 'static,
    ) -> Self {
        self.toolbar_actions.push(ToolbarAction {
            label,
            icon,
            on_click: Arc::new(on_click),
            enabled: Arc::new(enabled),
        });
        self
    }

    /// Override which instance types are visible.
    pub fn with_visible_instance_types(mut self, types: &'static [InstanceType]) -> Self {
        self.visible_instance_types = types;
        self
    }

    /// Mark this layer as the one that shows probe nodes.
    pub fn showing_probe_nodes(mut self) -> Self {
        self.show_probe_nodes = true;
        self
    }
}

/// Registry of all layers' client info. Populated at app build time by each
/// `LayerClientPlugin` via [`RegisterLayerClient`].
#[derive(Resource, Default)]
pub struct LayerRegistry {
    layers: Vec<LayerClientInfo>,
}

impl LayerRegistry {
    /// Look up a layer's info.
    pub fn get(&self, layer: &LayerName) -> Option<&LayerClientInfo> {
        self.layers.iter().find(|info| &info.layer == layer)
    }

    /// Iterate over all registered layers.
    pub fn iter(&self) -> impl Iterator<Item = &LayerClientInfo> {
        self.layers.iter()
    }

    /// Whether the given layer shows probe nodes.
    pub fn show_probe_nodes(&self, layer: &LayerName) -> bool {
        self.get(layer).map(|i| i.show_probe_nodes).unwrap_or(false)
    }

    /// The toolbar actions for the given layer (empty when unregistered).
    pub fn toolbar_actions(&self, layer: &LayerName) -> &[ToolbarAction] {
        self.get(layer).map(|i| i.toolbar_actions.as_slice()).unwrap_or(&[])
    }
}

/// App extension for registering a layer's client info from its plugin.
pub trait RegisterLayerClient {
    /// Register a layer's [`LayerClientInfo`]. Idempotent w.r.t. the registry
    /// resource (it is created on first use).
    fn register_layer_client(&mut self, info: LayerClientInfo) -> &mut Self;
}

impl RegisterLayerClient for App {
    fn register_layer_client(&mut self, info: LayerClientInfo) -> &mut Self {
        self.init_resource::<LayerRegistry>();
        self.world_mut()
            .resource_mut::<LayerRegistry>()
            .layers
            .push(info);
        self
    }
}
