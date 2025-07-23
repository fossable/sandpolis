use image::Rgba;
use image::{DynamicImage, ImageBuffer, RgbaImage};
use ratatui::buffer::Buffer;
use ratatui::crossterm::event::{Event, KeyCode, KeyEventKind, KeyModifiers};
use ratatui::layout::{Constraint, Direction, Layout, Rect};
use ratatui::prelude::StatefulWidget;
use ratatui::style::{Color, Style};
use ratatui::text::{Line, Span, Text};
use ratatui::widgets::{Block, Borders, Clear, Paragraph, Widget, WidgetRef};
use ratatui_image::{
    FontSize, StatefulImage,
    protocol::{ImageSource, StatefulProtocol, StatefulProtocolType, halfblocks::Halfblocks},
};
use sandpolis_client::tui::EventHandler;
use sandpolis_core::InstanceId;
use std::collections::VecDeque;
use std::path::PathBuf;

pub struct DesktopFrame {
    pub image: DynamicImage,
    pub timestamp: std::time::Instant,
}

impl DesktopFrame {
    pub fn new(data: Vec<u8>, width: u32, height: u32) -> Result<Self, Box<dyn std::error::Error>> {
        // Assume data is RGBA format
        let image_buffer = ImageBuffer::from_raw(width, height, data)
            .ok_or("Failed to create image buffer from raw data")?;
        let rgba_image = RgbaImage::from(image_buffer);
        let dynamic_image = DynamicImage::ImageRgba8(rgba_image);

        Ok(Self {
            image: dynamic_image,
            timestamp: std::time::Instant::now(),
        })
    }

    pub fn from_rgb(
        data: Vec<u8>,
        width: u32,
        height: u32,
    ) -> Result<Self, Box<dyn std::error::Error>> {
        // Convert RGB to RGBA
        let mut rgba_data = Vec::with_capacity(data.len() * 4 / 3);
        for chunk in data.chunks(3) {
            if chunk.len() == 3 {
                rgba_data.extend_from_slice(chunk);
                rgba_data.push(255); // Alpha channel
            }
        }

        Self::new(rgba_data, width, height)
    }

    pub fn width(&self) -> u32 {
        self.image.width()
    }

    pub fn height(&self) -> u32 {
        self.image.height()
    }
}

pub struct DesktopViewerWidget {
    pub instance: InstanceId,
    pub frames: VecDeque<DesktopFrame>,
    pub current_frame: Option<DesktopFrame>,
    pub image_state: Option<StatefulProtocol>,
    pub connected: bool,
    pub status_message: String,
    pub max_frames: usize,
    pub zoom_level: f32,
    pub pan_x: i32,
    pub pan_y: i32,
    pub show_stats: bool,
    pub fps_counter: u32,
    pub last_fps_update: std::time::Instant,
    pub frame_count: u32,
}

impl DesktopViewerWidget {
    pub fn new(instance: InstanceId) -> Self {
        // Initialize with sixel protocol for terminal graphics
        let image_state = ratatui_image::picker::Picker::from_query_stdio()
            .ok()
            .and_then(|picker| {
                let font_size = picker.font_size();
                // Create a dummy image for initialization
                let dummy_image = image::DynamicImage::new_rgba8(1, 1);
                Some(StatefulProtocol::new(
                    ImageSource::new(
                        dummy_image,
                        (font_size.0, font_size.1),
                        Rgba([0, 0, 0, 255]),
                    ),
                    (font_size.0, font_size.1),
                    StatefulProtocolType::Halfblocks(Halfblocks::default()),
                ))
            });

        Self {
            instance,
            frames: VecDeque::new(),
            current_frame: None,
            image_state,
            connected: false,
            status_message: "Ready to connect".to_string(),
            max_frames: 5,
            zoom_level: 1.0,
            pan_x: 0,
            pan_y: 0,
            show_stats: false,
            fps_counter: 0,
            last_fps_update: std::time::Instant::now(),
            frame_count: 0,
        }
    }

    pub fn connect(&mut self) {
        self.connected = true;
        self.status_message = "Connected to desktop".to_string();
        self.frames.clear();
        self.current_frame = None;
        self.frame_count = 0;

        // Reinitialize image state if needed
        if self.image_state.is_none() {
            self.image_state = ratatui_image::picker::Picker::from_query_stdio()
                .ok()
                .and_then(|picker| {
                    let font_size = picker.font_size();
                    let dummy_image = image::DynamicImage::new_rgba8(1, 1);
                    Some(StatefulProtocol::new(
                        ImageSource::new(
                            dummy_image,
                            (font_size.0, font_size.1),
                            Rgba([0, 0, 0, 255]),
                        ),
                        (font_size.0, font_size.1),
                        StatefulProtocolType::Halfblocks(Halfblocks::default()),
                    ))
                });
        }
    }

    pub fn disconnect(&mut self) {
        self.connected = false;
        self.status_message = "Disconnected".to_string();
        self.frames.clear();
        self.current_frame = None;
    }

    pub fn add_frame(&mut self, frame: DesktopFrame) {
        if !self.connected {
            return;
        }

        // Update FPS counter
        self.frame_count += 1;
        let now = std::time::Instant::now();
        if now.duration_since(self.last_fps_update).as_secs() >= 1 {
            self.fps_counter = self.frame_count;
            self.frame_count = 0;
            self.last_fps_update = now;
        }

        // Store the current frame
        self.current_frame = Some(frame);

        // Add to frame buffer
        if let Some(current) = &self.current_frame {
            self.frames.push_back(DesktopFrame {
                image: current.image.clone(),
                timestamp: current.timestamp,
            });
        }

        // Keep only the last N frames
        while self.frames.len() > self.max_frames {
            self.frames.pop_front();
        }
    }

    pub fn zoom_in(&mut self) {
        self.zoom_level = (self.zoom_level * 1.2).min(5.0);
    }

    pub fn zoom_out(&mut self) {
        self.zoom_level = (self.zoom_level / 1.2).max(0.1);
    }

    pub fn reset_view(&mut self) {
        self.zoom_level = 1.0;
        self.pan_x = 0;
        self.pan_y = 0;
    }

    pub fn pan(&mut self, dx: i32, dy: i32) {
        self.pan_x += dx;
        self.pan_y += dy;
    }

    pub fn toggle_stats(&mut self) {
        self.show_stats = !self.show_stats;
    }

    fn render_frame_with_ratatui_image(&mut self, area: Rect, buf: &mut ratatui::buffer::Buffer) {
        if let (Some(frame), Some(image_state)) = (&self.current_frame, &mut self.image_state) {
            // Update the protocol state with the current frame image
            *image_state = StatefulProtocol::new(
                ImageSource::new(frame.image.clone(), (8, 16), Rgba([0, 0, 0, 255])),
                (8, 16),
                StatefulProtocolType::Halfblocks(Halfblocks::default()),
            );

            // Create StatefulImage widget and render
            let image_widget = StatefulImage::new();
            image_widget.render(area, buf, image_state);
        } else if self.current_frame.is_none() {
            // No frame available
            self.render_info_message(area, buf, "Waiting for desktop capture...");
        } else {
            // No image state (graphics not supported)
            self.render_error_message(area, buf, "Image display not supported in this terminal");
        }
    }

    fn render_error_message(&self, area: Rect, buf: &mut ratatui::buffer::Buffer, message: &str) {
        let error_text = vec![
            Line::from(vec![Span::styled("Error", Style::default().fg(Color::Red))]),
            Line::from(vec![Span::raw(message)]),
        ];

        let paragraph = Paragraph::new(Text::from(error_text));
        paragraph.render(area, buf);
    }

    fn render_info_message(&self, area: Rect, buf: &mut ratatui::buffer::Buffer, message: &str) {
        let info_text = vec![
            Line::from(vec![Span::styled(
                "Desktop Viewer",
                Style::default().fg(Color::Yellow),
            )]),
            Line::from(vec![Span::raw(message)]),
            Line::from(""),
            Line::from(vec![Span::raw(format!("Zoom: {:.1}x", self.zoom_level))]),
        ];

        let paragraph = Paragraph::new(Text::from(info_text));
        paragraph.render(area, buf);
    }

    fn render_stats(&self, area: Rect, buf: &mut ratatui::buffer::Buffer) {
        if !self.show_stats {
            return;
        }

        let stats_text = vec![
            Line::from(vec![Span::styled(
                "Desktop Stats",
                Style::default().fg(Color::Yellow),
            )]),
            Line::from(vec![Span::raw(format!("FPS: {}", self.fps_counter))]),
            Line::from(vec![Span::raw(format!(
                "Frames buffered: {}",
                self.frames.len()
            ))]),
            Line::from(vec![Span::raw(format!("Zoom: {:.1}x", self.zoom_level))]),
            Line::from(vec![Span::raw(format!(
                "Pan: ({}, {})",
                self.pan_x, self.pan_y
            ))]),
        ];

        let stats_paragraph = Paragraph::new(Text::from(stats_text)).block(
            Block::default()
                .title("Stats")
                .borders(Borders::ALL)
                .border_style(Style::default().fg(Color::Cyan)),
        );

        // Render in top-right corner
        let stats_area = Rect {
            x: area.x + area.width.saturating_sub(25),
            y: area.y,
            width: 25.min(area.width),
            height: 8.min(area.height),
        };

        Clear.render(stats_area, buf);
        stats_paragraph.render(stats_area, buf);
    }
}

impl Widget for DesktopViewerWidget {
    fn render(mut self, area: Rect, buf: &mut ratatui::buffer::Buffer) {
        // Split the area into main viewer and status bar
        let chunks = Layout::default()
            .direction(Direction::Vertical)
            .constraints([
                Constraint::Min(3),    // Viewer area
                Constraint::Length(3), // Status bar
            ])
            .split(area);

        // Render the main viewer block
        let viewer_block = Block::default()
            .title("Desktop Viewer (Sixel)")
            .borders(Borders::ALL)
            .border_style(if self.connected {
                Style::default().fg(Color::Green)
            } else {
                Style::default().fg(Color::Red)
            });

        // Calculate inner area for viewer content
        let viewer_inner = viewer_block.inner(chunks[0]);
        viewer_block.render(chunks[0], buf);

        // Render the desktop frame using ratatui-image with sixel graphics
        self.render_frame_with_ratatui_image(viewer_inner, buf);

        // Render stats overlay if enabled
        self.render_stats(chunks[0], buf);

        // Render status bar
        let mut status_lines = vec![Line::from(vec![
            Span::styled("Status: ", Style::default().fg(Color::Yellow)),
            Span::raw(&self.status_message),
        ])];

        if self.connected {
            status_lines.push(Line::from(vec![
                Span::styled("Controls: ", Style::default().fg(Color::Cyan)),
                Span::raw("Space=Connect, +/-=Zoom, Arrows=Pan, S=Stats, Ctrl+Q=Quit"),
            ]));
        } else {
            status_lines.push(Line::from(vec![
                Span::styled("Controls: ", Style::default().fg(Color::Cyan)),
                Span::raw("Space=Connect, Ctrl+Q=Quit"),
            ]));
        }

        let status_paragraph = Paragraph::new(Text::from(status_lines)).block(
            Block::default()
                .title("Status")
                .borders(Borders::ALL)
                .border_style(Style::default().fg(Color::Blue)),
        );

        status_paragraph.render(chunks[1], buf);
    }
}

impl EventHandler for DesktopViewerWidget {
    fn handle_event(&mut self, event: Event) -> Option<Event> {
        if let Event::Key(key) = event {
            if key.kind == KeyEventKind::Press {
                match key.code {
                    KeyCode::Char(' ') => {
                        if !self.connected {
                            self.connect();

                            // Simulate adding a test frame with proper error handling
                            let test_data = vec![128u8; 1920 * 1080 * 4]; // RGBA data
                            match DesktopFrame::new(test_data, 1920, 1080) {
                                Ok(test_frame) => {
                                    self.add_frame(test_frame);
                                }
                                Err(e) => {
                                    self.status_message =
                                        format!("Failed to create test frame: {}", e);
                                }
                            }
                        } else {
                            self.disconnect();
                        }
                        return None;
                    }
                    KeyCode::Char('+') | KeyCode::Char('=') => {
                        if self.connected {
                            self.zoom_in();
                        }
                        return None;
                    }
                    KeyCode::Char('-') => {
                        if self.connected {
                            self.zoom_out();
                        }
                        return None;
                    }
                    KeyCode::Char('0') => {
                        if self.connected {
                            self.reset_view();
                        }
                        return None;
                    }
                    KeyCode::Char('s') | KeyCode::Char('S') => {
                        if self.connected {
                            self.toggle_stats();
                        }
                        return None;
                    }
                    KeyCode::Up => {
                        if self.connected {
                            self.pan(0, -10);
                        }
                        return None;
                    }
                    KeyCode::Down => {
                        if self.connected {
                            self.pan(0, 10);
                        }
                        return None;
                    }
                    KeyCode::Left => {
                        if self.connected {
                            self.pan(-10, 0);
                        }
                        return None;
                    }
                    KeyCode::Right => {
                        if self.connected {
                            self.pan(10, 0);
                        }
                        return None;
                    }
                    KeyCode::Char('q') if key.modifiers.contains(KeyModifiers::CONTROL) => {
                        // This should be handled by the parent application
                    }
                    _ => {}
                }
            }
        } else if let Event::Resize(width, height) = event {
            // Handle terminal resize - could adjust frame rendering accordingly
            self.status_message = format!("Terminal resized to {}x{}", width, height);
        }

        Some(event)
    }
}

/// Desktop capture settings widget for configuring capture parameters
pub struct DesktopSettingsWidget {
    pub capture_fps: u32,
    pub capture_quality: u8,
    pub show_cursor: bool,
    pub show_settings: bool,
    pub selected_option: usize,
}

impl DesktopSettingsWidget {
    pub fn new() -> Self {
        Self {
            capture_fps: 30,
            capture_quality: 80,
            show_cursor: true,
            show_settings: false,
            selected_option: 0,
        }
    }

    pub fn show(&mut self) {
        self.show_settings = true;
    }

    pub fn hide(&mut self) {
        self.show_settings = false;
    }

    pub fn select_next(&mut self) {
        self.selected_option = (self.selected_option + 1) % 3;
    }

    pub fn select_previous(&mut self) {
        self.selected_option = if self.selected_option == 0 {
            2
        } else {
            self.selected_option - 1
        };
    }

    pub fn adjust_value(&mut self, increase: bool) {
        match self.selected_option {
            0 => {
                // FPS adjustment
                if increase {
                    self.capture_fps = (self.capture_fps + 5).min(60);
                } else {
                    self.capture_fps = (self.capture_fps.saturating_sub(5)).max(5);
                }
            }
            1 => {
                // Quality adjustment
                if increase {
                    self.capture_quality = (self.capture_quality + 10).min(100);
                } else {
                    self.capture_quality = (self.capture_quality.saturating_sub(10)).max(10);
                }
            }
            2 => {
                // Cursor toggle
                self.show_cursor = !self.show_cursor;
            }
            _ => {}
        }
    }
}

impl WidgetRef for DesktopSettingsWidget {
    fn render_ref(&self, area: Rect, buf: &mut Buffer) {
        if !self.show_settings {
            return;
        }

        // Create a centered popup
        let popup_area = centered_rect(50, 40, area);

        // Clear the area
        Clear.render(popup_area, buf);

        let options = [
            format!("FPS: {}", self.capture_fps),
            format!("Quality: {}%", self.capture_quality),
            format!(
                "Show Cursor: {}",
                if self.show_cursor { "Yes" } else { "No" }
            ),
        ];

        let mut lines = vec![
            Line::from(vec![Span::styled(
                "Desktop Capture Settings",
                Style::default().fg(Color::Yellow),
            )]),
            Line::from(""),
        ];

        for (i, option) in options.iter().enumerate() {
            let style = if i == self.selected_option {
                Style::default().bg(Color::Blue).fg(Color::White)
            } else {
                Style::default()
            };

            lines.push(Line::from(vec![Span::styled(
                format!("  {}", option),
                style,
            )]));
        }

        lines.push(Line::from(""));
        lines.push(Line::from(vec![
            Span::styled("Controls: ", Style::default().fg(Color::Cyan)),
            Span::raw("Up/Down Navigate, Left/Right Adjust, Enter Apply, Esc Cancel"),
        ]));

        let paragraph = Paragraph::new(Text::from(lines)).block(
            Block::default()
                .title("Settings")
                .borders(Borders::ALL)
                .border_style(Style::default().fg(Color::Green)),
        );

        paragraph.render(popup_area, buf);
    }
}

impl EventHandler for DesktopSettingsWidget {
    fn handle_event(&mut self, event: Event) -> Option<Event> {
        if !self.show_settings {
            return Some(event);
        }

        if let Event::Key(key) = event {
            if key.kind == KeyEventKind::Press {
                match key.code {
                    KeyCode::Up | KeyCode::Char('k') => {
                        self.select_previous();
                        return None;
                    }
                    KeyCode::Down | KeyCode::Char('j') => {
                        self.select_next();
                        return None;
                    }
                    KeyCode::Left | KeyCode::Char('h') => {
                        self.adjust_value(false);
                        return None;
                    }
                    KeyCode::Right | KeyCode::Char('l') => {
                        self.adjust_value(true);
                        return None;
                    }
                    KeyCode::Enter => {
                        self.hide();
                        return None;
                    }
                    KeyCode::Esc => {
                        self.hide();
                        return None;
                    }
                    _ => {}
                }
            }
        }
        Some(event)
    }
}

// Helper function to create a centered rectangle
fn centered_rect(percent_x: u16, percent_y: u16, r: Rect) -> Rect {
    let popup_layout = Layout::default()
        .direction(Direction::Vertical)
        .constraints([
            Constraint::Percentage((100 - percent_y) / 2),
            Constraint::Percentage(percent_y),
            Constraint::Percentage((100 - percent_y) / 2),
        ])
        .split(r);

    Layout::default()
        .direction(Direction::Horizontal)
        .constraints([
            Constraint::Percentage((100 - percent_x) / 2),
            Constraint::Percentage(percent_x),
            Constraint::Percentage((100 - percent_x) / 2),
        ])
        .split(popup_layout[1])[1]
}
