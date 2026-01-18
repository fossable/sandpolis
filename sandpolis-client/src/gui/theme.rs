//! Theme system for the Sandpolis GUI.
//!
//! Provides customizable color themes including dark, light, and high contrast options.

use bevy::prelude::*;
use bevy_egui::egui;

/// Available theme presets for the application.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum ThemePreset {
    #[default]
    SandpolisDark,
    SandpolisLight,
    Classic,
    HighContrast,
}

impl ThemePreset {
    /// Get a human-readable name for the theme.
    pub fn name(&self) -> &'static str {
        match self {
            ThemePreset::SandpolisDark => "Sandpolis Dark",
            ThemePreset::SandpolisLight => "Sandpolis Light",
            ThemePreset::Classic => "Classic Dark",
            ThemePreset::HighContrast => "High Contrast",
        }
    }

    /// Get a description for the theme.
    pub fn description(&self) -> &'static str {
        match self {
            ThemePreset::SandpolisDark => "Default dark theme with blue and purple accents",
            ThemePreset::SandpolisLight => "Light theme for bright environments",
            ThemePreset::Classic => "Standard egui dark theme",
            ThemePreset::HighContrast => "High contrast theme for accessibility",
        }
    }

    /// Get all available theme presets.
    pub fn all() -> &'static [ThemePreset] {
        &[
            ThemePreset::SandpolisDark,
            ThemePreset::SandpolisLight,
            ThemePreset::Classic,
            ThemePreset::HighContrast,
        ]
    }
}

/// Resource that tracks the current active theme.
/// This can be updated at runtime to switch themes dynamically.
#[derive(Resource, Debug, Clone, Default)]
pub struct CurrentTheme {
    pub preset: ThemePreset,
    pub custom_visuals: Option<egui::Visuals>,
}

impl CurrentTheme {
    /// Get the egui visuals for the current theme.
    pub fn get_visuals(&self) -> egui::Visuals {
        if let Some(custom) = &self.custom_visuals {
            custom.clone()
        } else {
            create_theme_visuals(self.preset)
        }
    }

    /// Set a new theme preset (will override custom visuals).
    pub fn set_preset(&mut self, preset: ThemePreset) {
        self.preset = preset;
        self.custom_visuals = None;
    }

    /// Set fully custom visuals (will override preset).
    pub fn set_custom_visuals(&mut self, visuals: egui::Visuals) {
        self.custom_visuals = Some(visuals);
    }
}

/// Create egui visuals for a specific theme preset.
pub fn create_theme_visuals(preset: ThemePreset) -> egui::Visuals {
    match preset {
        ThemePreset::SandpolisDark => sandpolis_dark_theme(),
        ThemePreset::SandpolisLight => sandpolis_light_theme(),
        ThemePreset::Classic => egui::Visuals::dark(),
        ThemePreset::HighContrast => high_contrast_theme(),
    }
}

/// Sandpolis dark theme with custom colors.
fn sandpolis_dark_theme() -> egui::Visuals {
    let mut visuals = egui::Visuals::dark();

    // Color palette - dark theme with blue/purple accents
    let bg_color = egui::Color32::from_rgb(24, 26, 32); // Very dark blue-gray
    let panel_bg = egui::Color32::from_rgb(32, 34, 42); // Slightly lighter panel
    let widget_bg = egui::Color32::from_rgb(40, 43, 54); // Widget background
    let accent_blue = egui::Color32::from_rgb(98, 114, 164); // Muted blue accent
    let accent_purple = egui::Color32::from_rgb(139, 104, 181); // Purple accent
    let text_color = egui::Color32::from_rgb(220, 223, 228); // Light gray text
    let warning_yellow = egui::Color32::from_rgb(200, 160, 60); // Warning
    let error_red = egui::Color32::from_rgb(200, 80, 80); // Error/danger

    // Override colors
    visuals.override_text_color = Some(text_color);
    visuals.hyperlink_color = accent_blue;
    visuals.faint_bg_color = bg_color;
    visuals.extreme_bg_color = egui::Color32::from_rgb(16, 18, 24); // Even darker
    visuals.code_bg_color = widget_bg;
    visuals.warn_fg_color = warning_yellow;
    visuals.error_fg_color = error_red;

    // Window styling
    visuals.window_fill = panel_bg;
    visuals.window_stroke = egui::Stroke::new(1.0, accent_blue.linear_multiply(0.5));
    visuals.window_shadow.color = egui::Color32::from_black_alpha(80);

    // Panel styling
    visuals.panel_fill = panel_bg;

    // Widget colors
    visuals.widgets.noninteractive.bg_fill = widget_bg;
    visuals.widgets.noninteractive.bg_stroke = egui::Stroke::new(1.0, bg_color);
    visuals.widgets.noninteractive.fg_stroke = egui::Stroke::new(1.0, text_color);

    visuals.widgets.inactive.bg_fill = widget_bg;
    visuals.widgets.inactive.bg_stroke = egui::Stroke::new(1.0, accent_blue.linear_multiply(0.3));
    visuals.widgets.inactive.fg_stroke = egui::Stroke::new(1.0, text_color);

    visuals.widgets.hovered.bg_fill = accent_blue.linear_multiply(0.3);
    visuals.widgets.hovered.bg_stroke = egui::Stroke::new(1.5, accent_blue);
    visuals.widgets.hovered.fg_stroke = egui::Stroke::new(1.5, text_color);

    visuals.widgets.active.bg_fill = accent_purple.linear_multiply(0.4);
    visuals.widgets.active.bg_stroke = egui::Stroke::new(2.0, accent_purple);
    visuals.widgets.active.fg_stroke = egui::Stroke::new(2.0, text_color);

    visuals.widgets.open.bg_fill = accent_blue.linear_multiply(0.2);
    visuals.widgets.open.bg_stroke = egui::Stroke::new(1.0, accent_blue);
    visuals.widgets.open.fg_stroke = egui::Stroke::new(1.0, text_color);

    // Selection colors
    visuals.selection.bg_fill = accent_purple.linear_multiply(0.4);
    visuals.selection.stroke = egui::Stroke::new(1.0, accent_purple);

    // Separator
    visuals.widgets.noninteractive.bg_stroke.color = accent_blue.linear_multiply(0.2);

    visuals
}

/// Sandpolis light theme.
fn sandpolis_light_theme() -> egui::Visuals {
    let mut visuals = egui::Visuals::light();

    // Color palette - light theme with blue accents
    let bg_color = egui::Color32::from_rgb(240, 242, 245); // Light gray background
    let panel_bg = egui::Color32::from_rgb(250, 251, 253); // Almost white panels
    let widget_bg = egui::Color32::from_rgb(255, 255, 255); // Pure white widgets
    let accent_blue = egui::Color32::from_rgb(60, 90, 180); // Strong blue accent
    let accent_purple = egui::Color32::from_rgb(110, 70, 160); // Purple accent
    let text_color = egui::Color32::from_rgb(40, 44, 52); // Dark text
    let warning_yellow = egui::Color32::from_rgb(180, 140, 40);
    let error_red = egui::Color32::from_rgb(180, 60, 60);

    visuals.override_text_color = Some(text_color);
    visuals.hyperlink_color = accent_blue;
    visuals.faint_bg_color = bg_color;
    visuals.extreme_bg_color = egui::Color32::from_rgb(230, 232, 236);
    visuals.code_bg_color = egui::Color32::from_rgb(245, 246, 248);
    visuals.warn_fg_color = warning_yellow;
    visuals.error_fg_color = error_red;

    visuals.window_fill = panel_bg;
    visuals.window_stroke = egui::Stroke::new(1.0, accent_blue.linear_multiply(0.4));
    visuals.window_shadow.color = egui::Color32::from_black_alpha(40);

    visuals.panel_fill = panel_bg;

    visuals.widgets.noninteractive.bg_fill = widget_bg;
    visuals.widgets.noninteractive.bg_stroke = egui::Stroke::new(1.0, bg_color);
    visuals.widgets.noninteractive.fg_stroke = egui::Stroke::new(1.0, text_color);

    visuals.widgets.inactive.bg_fill = widget_bg;
    visuals.widgets.inactive.bg_stroke = egui::Stroke::new(1.0, accent_blue.linear_multiply(0.3));
    visuals.widgets.inactive.fg_stroke = egui::Stroke::new(1.0, text_color);

    visuals.widgets.hovered.bg_fill = accent_blue.linear_multiply(0.15);
    visuals.widgets.hovered.bg_stroke = egui::Stroke::new(1.5, accent_blue);
    visuals.widgets.hovered.fg_stroke = egui::Stroke::new(1.5, text_color);

    visuals.widgets.active.bg_fill = accent_purple.linear_multiply(0.25);
    visuals.widgets.active.bg_stroke = egui::Stroke::new(2.0, accent_purple);
    visuals.widgets.active.fg_stroke = egui::Stroke::new(2.0, text_color);

    visuals.widgets.open.bg_fill = accent_blue.linear_multiply(0.12);
    visuals.widgets.open.bg_stroke = egui::Stroke::new(1.0, accent_blue);
    visuals.widgets.open.fg_stroke = egui::Stroke::new(1.0, text_color);

    visuals.selection.bg_fill = accent_purple.linear_multiply(0.3);
    visuals.selection.stroke = egui::Stroke::new(1.0, accent_purple);

    visuals.widgets.noninteractive.bg_stroke.color = accent_blue.linear_multiply(0.15);

    visuals
}

/// High contrast theme for accessibility.
fn high_contrast_theme() -> egui::Visuals {
    let mut visuals = egui::Visuals::dark();

    // High contrast colors
    let bg_color = egui::Color32::BLACK;
    let panel_bg = egui::Color32::from_rgb(20, 20, 20);
    let widget_bg = egui::Color32::from_rgb(30, 30, 30);
    let accent_color = egui::Color32::from_rgb(255, 200, 0); // Bright yellow accent
    let text_color = egui::Color32::WHITE;
    let warning_yellow = egui::Color32::from_rgb(255, 220, 0);
    let error_red = egui::Color32::from_rgb(255, 60, 60);

    visuals.override_text_color = Some(text_color);
    visuals.hyperlink_color = accent_color;
    visuals.faint_bg_color = bg_color;
    visuals.extreme_bg_color = bg_color;
    visuals.code_bg_color = widget_bg;
    visuals.warn_fg_color = warning_yellow;
    visuals.error_fg_color = error_red;

    visuals.window_fill = panel_bg;
    visuals.window_stroke = egui::Stroke::new(2.0, accent_color);

    visuals.panel_fill = panel_bg;

    visuals.widgets.noninteractive.bg_fill = widget_bg;
    visuals.widgets.noninteractive.bg_stroke =
        egui::Stroke::new(2.0, text_color.linear_multiply(0.3));
    visuals.widgets.noninteractive.fg_stroke = egui::Stroke::new(2.0, text_color);

    visuals.widgets.inactive.bg_fill = widget_bg;
    visuals.widgets.inactive.bg_stroke = egui::Stroke::new(2.0, accent_color.linear_multiply(0.5));
    visuals.widgets.inactive.fg_stroke = egui::Stroke::new(2.0, text_color);

    visuals.widgets.hovered.bg_fill = widget_bg;
    visuals.widgets.hovered.bg_stroke = egui::Stroke::new(3.0, accent_color);
    visuals.widgets.hovered.fg_stroke = egui::Stroke::new(3.0, text_color);

    visuals.widgets.active.bg_fill = accent_color.linear_multiply(0.3);
    visuals.widgets.active.bg_stroke = egui::Stroke::new(3.0, accent_color);
    visuals.widgets.active.fg_stroke = egui::Stroke::new(3.0, text_color);

    visuals.widgets.open.bg_fill = widget_bg;
    visuals.widgets.open.bg_stroke = egui::Stroke::new(2.0, accent_color);
    visuals.widgets.open.fg_stroke = egui::Stroke::new(2.0, text_color);

    visuals.selection.bg_fill = accent_color.linear_multiply(0.5);
    visuals.selection.stroke = egui::Stroke::new(2.0, accent_color);

    visuals
}

/// Resource to track theme picker UI state.
#[derive(Resource, Default)]
pub struct ThemePickerState {
    pub show: bool,
}

/// System that applies the current theme to all egui contexts
/// This runs every frame to ensure theme changes are applied immediately
pub fn apply_theme_to_egui(mut contexts: bevy_egui::EguiContexts, theme: Res<CurrentTheme>) {
    // Only update if the theme has changed
    if !theme.is_changed() {
        return;
    }

    let Ok(ctx) = contexts.ctx_mut() else {
        return;
    };

    // Apply the theme visuals
    ctx.set_visuals(theme.get_visuals());
}

/// System to initialize the theme on startup
/// This ensures the theme is applied even on the first frame
pub fn initialize_theme(mut contexts: bevy_egui::EguiContexts, theme: Res<CurrentTheme>) {
    let Ok(ctx) = contexts.ctx_mut() else {
        return;
    };

    ctx.set_visuals(theme.get_visuals());
}

/// System to handle keyboard shortcut to toggle theme picker
pub fn handle_theme_picker_toggle(
    mut contexts: bevy_egui::EguiContexts,
    keyboard: Res<ButtonInput<KeyCode>>,
    mut picker_state: ResMut<ThemePickerState>,
) {
    // Don't handle if egui wants keyboard input
    let Ok(ctx) = contexts.ctx_mut() else {
        return;
    };
    if ctx.wants_keyboard_input() {
        return;
    }

    // Toggle theme picker with 'T' key
    if keyboard.just_pressed(KeyCode::KeyT) {
        picker_state.show = !picker_state.show;
    }
}

/// System to render the theme picker UI
pub fn render_theme_picker(
    mut contexts: bevy_egui::EguiContexts,
    mut picker_state: ResMut<ThemePickerState>,
    mut theme: ResMut<CurrentTheme>,
    windows: Query<&Window>,
) {
    if !picker_state.show {
        return;
    }

    let Ok(window) = windows.single() else {
        return;
    };

    let Ok(ctx) = contexts.ctx_mut() else {
        return;
    };

    let window_size = Vec2::new(window.width(), window.height());
    let is_mobile = window_size.x < 800.0;

    // Panel dimensions
    let panel_width = if is_mobile {
        window_size.x * 0.9
    } else {
        450.0
    };
    let panel_height = if is_mobile {
        window_size.y * 0.7
    } else {
        400.0
    };

    // Center the panel
    let panel_pos = egui::Pos2::new(
        (window_size.x - panel_width) / 2.0,
        (window_size.y - panel_height) / 2.0,
    );

    let mut should_close = false;

    egui::Window::new("Theme Selector")
        .fixed_pos(panel_pos)
        .fixed_size([panel_width, panel_height])
        .collapsible(false)
        .resizable(false)
        .show(ctx, |ui| {
            ui.heading("Choose Your Theme");
            ui.separator();
            ui.add_space(8.0);

            ui.label("Select a theme to apply it immediately:");
            ui.add_space(12.0);

            // Display all available themes
            egui::ScrollArea::vertical().show(ui, |ui| {
                for preset in ThemePreset::all() {
                    let is_current = theme.preset == *preset && theme.custom_visuals.is_none();

                    ui.group(|ui| {
                        ui.set_min_width(panel_width - 60.0);

                        let button_text = if is_current {
                            format!("* {}", preset.name())
                        } else {
                            preset.name().to_string()
                        };

                        let button = egui::Button::new(
                            egui::RichText::new(&button_text)
                                .size(if is_mobile { 18.0 } else { 16.0 })
                                .strong(),
                        )
                        .fill(if is_current {
                            ui.visuals().widgets.active.bg_fill
                        } else {
                            ui.visuals().widgets.inactive.bg_fill
                        })
                        .min_size(egui::vec2(
                            panel_width - 80.0,
                            if is_mobile { 50.0 } else { 40.0 },
                        ));

                        if ui.add(button).clicked() {
                            theme.set_preset(*preset);
                            tracing::info!("Theme changed to: {}", preset.name());
                        }

                        ui.add_space(4.0);
                        ui.label(
                            egui::RichText::new(preset.description())
                                .size(if is_mobile { 13.0 } else { 12.0 })
                                .color(ui.visuals().weak_text_color()),
                        );
                    });

                    ui.add_space(if is_mobile { 10.0 } else { 8.0 });
                }
            });

            ui.add_space(8.0);
            ui.separator();
            ui.add_space(8.0);

            // Current theme info
            ui.horizontal(|ui| {
                ui.label(
                    egui::RichText::new("Current Theme:")
                        .strong()
                        .size(if is_mobile { 15.0 } else { 14.0 }),
                );
                ui.label(
                    egui::RichText::new(theme.preset.name())
                        .color(ui.visuals().hyperlink_color)
                        .size(if is_mobile { 15.0 } else { 14.0 }),
                );
            });

            ui.add_space(12.0);

            // Close button at bottom
            ui.with_layout(egui::Layout::bottom_up(egui::Align::Center), |ui| {
                if ui
                    .add_sized(
                        [panel_width - 40.0, if is_mobile { 45.0 } else { 35.0 }],
                        egui::Button::new(egui::RichText::new("Close (T)").size(if is_mobile {
                            18.0
                        } else {
                            16.0
                        })),
                    )
                    .clicked()
                {
                    should_close = true;
                }

                ui.add_space(4.0);
                ui.label(
                    egui::RichText::new("Press 'T' to toggle theme picker")
                        .size(12.0)
                        .color(egui::Color32::from_gray(150)),
                );
            });
        });

    if should_close {
        picker_state.show = false;
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_theme_presets() {
        // Ensure all theme presets can be created without panicking
        for preset in [
            ThemePreset::SandpolisDark,
            ThemePreset::SandpolisLight,
            ThemePreset::Classic,
            ThemePreset::HighContrast,
        ] {
            let _visuals = create_theme_visuals(preset);
        }
    }

    #[test]
    fn test_current_theme_default() {
        let theme = CurrentTheme::default();
        assert_eq!(theme.preset, ThemePreset::SandpolisDark);
        assert!(theme.custom_visuals.is_none());
    }

    #[test]
    fn test_theme_switching() {
        let mut theme = CurrentTheme::default();

        // Test preset switching
        theme.set_preset(ThemePreset::SandpolisLight);
        assert_eq!(theme.preset, ThemePreset::SandpolisLight);
        assert!(theme.custom_visuals.is_none());

        // Test custom visuals
        let custom = egui::Visuals::dark();
        theme.set_custom_visuals(custom.clone());
        assert!(theme.custom_visuals.is_some());
    }
}
