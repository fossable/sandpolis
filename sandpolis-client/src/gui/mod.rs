//! Core GUI components for the Sandpolis client.
//!
//! This module provides the complete GUI infrastructure for the Sandpolis client,
//! including layer-agnostic components and Bevy systems.

pub mod activity;
pub mod core_toolbar;
pub mod assets;
pub mod database_browser;
pub mod drag;
pub mod edges;
pub mod input;
pub mod instance_layer;
pub mod layer_picker;
pub mod layer_toolbar;
pub mod layer_ui;
pub mod layer_visuals;
pub mod layout;
pub mod listeners;
pub mod login;
pub mod minimap;
pub mod node;
pub mod node_effects;
pub mod node_panel;
pub mod node_picker;
pub mod queries;
pub mod realm_select;
pub mod responsive;
pub mod services_panel;
pub mod terrain;
pub mod terrain_layout;
pub mod theme;
pub mod toast;
pub mod ui;
