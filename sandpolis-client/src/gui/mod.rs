//! Core GUI components for the Sandpolis client.
//!
//! This module provides the complete GUI infrastructure for the Sandpolis client,
//! including layer-agnostic components and Bevy systems.

pub mod about;
pub mod activity;
pub mod components;
pub mod controller;
pub mod drag;
pub mod edges;
pub mod input;
pub mod layer_ext;
pub mod layer_switcher;
pub mod layer_ui;
pub mod layer_visuals;
pub mod layout;
pub mod listeners;
pub mod login;
pub mod minimap;
pub mod node;
pub mod node_picker;
pub mod preview;
pub mod queries;
pub mod responsive;
pub mod theme;

// Re-export commonly used types from about
pub use about::{
    handle_about_easter_egg, register_logo_click, render_about_screen, rotate_about_logo,
    spawn_about_logo, AboutCamera, AboutLogo, AboutScreenState,
};

// Re-export commonly used types from components
pub use components::{
    CurrentLayer, DatabaseUpdate, DatabaseUpdateChannel, DatabaseUpdateSender, LayerIndicator,
    LayerIndicatorState, Minimap, MinimapCamera, MinimapViewport, NodeEntity, Selected,
    SelectionSet, WorldView, ZoomLevel,
};

// Re-export commonly used types from layer_ext
pub use layer_ext::{
    get_extension_for_layer, get_layer_extensions, ActivityTypeInfo, LayerGuiExtension,
};

// Re-export commonly used types from theme
pub use theme::{
    apply_theme_to_egui, create_theme_visuals, handle_theme_picker_toggle, initialize_theme,
    render_theme_picker, CurrentTheme, ThemePickerState, ThemePreset,
};

// Re-export controller types
pub use controller::{
    close_controller_on_layer_change, handle_node_double_click, render_node_controller,
    ControllerType, NodeControllerState,
};

// Re-export drag types
pub use drag::{
    disable_forces_while_dragging, handle_node_selection, render_selection_ui, start_node_drag,
    stop_node_drag, update_node_drag, update_selection_visuals, DragState, Dragging,
    SelectionRing,
};

// Re-export input types
pub use input::{
    handle_camera, handle_keymap, handle_zoom, HelpScreenState, LayerChangeTimer,
    LoginDialogState, LoginPhase, MousePressed, PanningState,
};

// Re-export layout types
pub use layout::{
    apply_damping, apply_repulsion_forces, apply_spring_forces, check_stabilization, LayoutConfig,
    LayoutState,
};

// Re-export layer_switcher types
pub use layer_switcher::{
    handle_layer_switcher_toggle, render_layer_switcher_button, render_layer_switcher_panel,
    LayerSwitcherState,
};

// Re-export layer_ui types
pub use layer_ui::render_layer_indicator;

// Re-export minimap types
pub use minimap::render_minimap;

// Re-export node types
pub use node::{scale_node_svgs, spawn_node};

// Re-export node_picker types
pub use node_picker::{handle_node_picker_toggle, render_node_picker_panel, NodePickerState};

// Re-export preview types
pub use preview::{render_node_previews, toggle_node_preview_visibility, NodePreview};

// Re-export edges types
pub use edges::{
    render_edge_labels, render_edges, update_edge_visibility, update_edges_for_layer, Edge,
    EdgeLabel,
};

// Re-export activity types
pub use activity::{
    animate_activity_lines, cleanup_layer_activity_lines, despawn_completed_activity_lines,
    spawn_network_activity_lines, spawn_transfer_activity_lines, update_activity_line_positions,
    ActivityLine,
};

// Re-export layer_visuals types
pub use layer_visuals::{update_node_colors_for_layer, update_node_svgs_for_layer};

// Re-export responsive types
pub use responsive::update_responsive_ui;

// Re-export login types
pub use login::{check_saved_servers, handle_login_phase1, handle_login_phase2, LoginOperation};

// Re-export listeners types
pub use listeners::setup_all_listeners;

// Re-export queries types
pub use queries::{
    query_all_instances, query_instance_metadata, query_network_topology, InstanceMetadata,
    NetworkEdge,
};
