//! Layer-specific GUI extension trait.
//!
//! This module defines the `LayerGuiExtension` trait that layer crates can implement
//! to provide GUI-specific functionality. Layer implementations are collected at
//! compile time using the `inventory` crate pattern.

use bevy::prelude::*;
use bevy_egui::egui;
use sandpolis_core::{InstanceId, InstanceType, LayerName};

/// Trait for layer-specific GUI extensions.
///
/// Layer crates implement this trait to provide their GUI-specific functionality,
/// which is then collected and used by the main GUI system.
pub trait LayerGuiExtension: Send + Sync + 'static {
    /// Returns the layer this extension is for.
    fn layer(&self) -> &LayerName;

    /// Returns a brief description of what this layer provides.
    fn description(&self) -> &'static str;

    /// Render the controller UI for this layer.
    ///
    /// This is called when the user opens a controller window for a node
    /// while this layer is active.
    fn render_controller(&self, ui: &mut egui::Ui, instance_id: InstanceId);

    /// Get the display name for this layer's controller.
    fn controller_name(&self) -> &'static str;

    /// Get the SVG asset path for a node on this layer.
    ///
    /// This is used to display layer-specific icons for nodes.
    fn get_node_svg(&self, instance_id: InstanceId) -> &'static str;

    /// Get the color tint for a node on this layer.
    ///
    /// This is used to provide visual feedback based on layer-specific state.
    fn get_node_color(&self, instance_id: InstanceId) -> Color;

    /// Get the icon emoji for the preview panel.
    fn preview_icon(&self) -> &'static str;

    /// Get the detail text for the preview panel.
    fn preview_details(&self, instance_id: InstanceId) -> String;

    /// Get the color for edges on this layer.
    fn edge_color(&self) -> Color;

    /// Get activity types available for this layer.
    fn activity_types(&self) -> Vec<ActivityTypeInfo> {
        vec![]
    }

    /// Register layer-specific Bevy systems.
    fn register_systems(&self, app: &mut App) {
        let _ = app;
    }

    /// Returns which instance types should be visible when this layer is active.
    ///
    /// By default, all instance types are visible. Layers can override this
    /// to filter which nodes appear in the world view.
    fn visible_instance_types(&self) -> &'static [InstanceType] {
        &[InstanceType::Server, InstanceType::Agent, InstanceType::Client]
    }

    /// Returns whether probe nodes should be visible when this layer is active.
    ///
    /// Probes are special nodes that are attached to agents. By default, probes
    /// are not visible. Only the Probe layer should return true.
    fn show_probe_nodes(&self) -> bool {
        false
    }
}

/// Information about an activity type that can be visualized.
#[derive(Clone, Debug)]
pub struct ActivityTypeInfo {
    /// Unique identifier for this activity type.
    pub id: &'static str,
    /// Display name for this activity type.
    pub name: &'static str,
    /// Color used for visualization.
    pub color: Color,
    /// Size of the activity indicator.
    pub size: f32,
}

// Use inventory to collect LayerGuiExtension implementations
inventory::collect!(&'static dyn LayerGuiExtension);

/// Get all registered layer GUI extensions.
pub fn get_layer_extensions() -> impl Iterator<Item = &'static &'static dyn LayerGuiExtension> {
    inventory::iter::<&'static dyn LayerGuiExtension>()
}

/// Find the layer extension for a specific layer.
pub fn get_extension_for_layer(layer: &LayerName) -> Option<&'static dyn LayerGuiExtension> {
    get_layer_extensions()
        .find(|ext| ext.layer() == layer)
        .copied()
}
