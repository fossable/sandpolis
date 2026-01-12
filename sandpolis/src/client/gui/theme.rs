use bevy::prelude::*;
use bevy_egui::egui;

/// Available theme presets for the application
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum ThemePreset {
    #[default]
    SandpolisDark,
    SandpolisLight,
    Classic,
    HighContrast,
}

/// Resource that tracks the current active theme
/// This can be updated at runtime to switch themes dynamically
#[derive(Resource, Debug, Clone)]
pub struct CurrentTheme {
    pub preset: ThemePreset,
    pub custom_visuals: Option<egui::Visuals>,
}

impl Default for CurrentTheme {
    fn default() -> Self {
        Self {
            preset: ThemePreset::default(),
            custom_visuals: None,
        }
    }
}

impl CurrentTheme {
    /// Get the egui visuals for the current theme
    pub fn get_visuals(&self) -> egui::Visuals {
        if let Some(custom) = &self.custom_visuals {
            custom.clone()
        } else {
            create_theme_visuals(self.preset)
        }
    }

    /// Set a new theme preset (will override custom visuals)
    pub fn set_preset(&mut self, preset: ThemePreset) {
        self.preset = preset;
        self.custom_visuals = None;
    }

    /// Set fully custom visuals (will override preset)
    pub fn set_custom_visuals(&mut self, visuals: egui::Visuals) {
        self.custom_visuals = Some(visuals);
    }
}

/// Create egui visuals for a specific theme preset
pub fn create_theme_visuals(preset: ThemePreset) -> egui::Visuals {
    match preset {
        ThemePreset::SandpolisDark => sandpolis_dark_theme(),
        ThemePreset::SandpolisLight => sandpolis_light_theme(),
        ThemePreset::Classic => egui::Visuals::dark(),
        ThemePreset::HighContrast => high_contrast_theme(),
    }
}

/// Sandpolis dark theme with custom colors
fn sandpolis_dark_theme() -> egui::Visuals {
    let mut visuals = egui::Visuals::dark();

    // Color palette - dark theme with blue/purple accents
    let bg_color = egui::Color32::from_rgb(24, 26, 32); // Very dark blue-gray
    let panel_bg = egui::Color32::from_rgb(32, 34, 42); // Slightly lighter panel
    let widget_bg = egui::Color32::from_rgb(40, 43, 54); // Widget background
    let accent_blue = egui::Color32::from_rgb(98, 114, 164); // Muted blue accent
    let accent_purple = egui::Color32::from_rgb(139, 104, 181); // Purple accent
    let text_color = egui::Color32::from_rgb(220, 223, 228); // Light gray text
    let success_green = egui::Color32::from_rgb(80, 160, 120); // Success/confirm
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

/// Sandpolis light theme
fn sandpolis_light_theme() -> egui::Visuals {
    let mut visuals = egui::Visuals::light();

    // Color palette - light theme with blue accents
    let bg_color = egui::Color32::from_rgb(240, 242, 245); // Light gray background
    let panel_bg = egui::Color32::from_rgb(250, 251, 253); // Almost white panels
    let widget_bg = egui::Color32::from_rgb(255, 255, 255); // Pure white widgets
    let accent_blue = egui::Color32::from_rgb(60, 90, 180); // Strong blue accent
    let accent_purple = egui::Color32::from_rgb(110, 70, 160); // Purple accent
    let text_color = egui::Color32::from_rgb(40, 44, 52); // Dark text
    let success_green = egui::Color32::from_rgb(60, 140, 100);
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

/// High contrast theme for accessibility
fn high_contrast_theme() -> egui::Visuals {
    let mut visuals = egui::Visuals::dark();

    // High contrast colors
    let bg_color = egui::Color32::BLACK;
    let panel_bg = egui::Color32::from_rgb(20, 20, 20);
    let widget_bg = egui::Color32::from_rgb(30, 30, 30);
    let accent_color = egui::Color32::from_rgb(255, 200, 0); // Bright yellow accent
    let text_color = egui::Color32::WHITE;
    let success_green = egui::Color32::from_rgb(0, 255, 100);
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
    visuals.widgets.noninteractive.bg_stroke = egui::Stroke::new(2.0, text_color.linear_multiply(0.3));
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

/// System that applies the current theme to all egui contexts
/// This runs every frame to ensure theme changes are applied immediately
pub fn apply_theme_to_egui(
    mut contexts: bevy_egui::EguiContexts,
    theme: Res<CurrentTheme>,
) {
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
pub fn initialize_theme(
    mut contexts: bevy_egui::EguiContexts,
    theme: Res<CurrentTheme>,
) {
    let Ok(ctx) = contexts.ctx_mut() else {
        return;
    };

    ctx.set_visuals(theme.get_visuals());
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
