use crate::FilesystemLayer;
use chrono::{DateTime, Local};
use ratatui::crossterm::event::{Event, KeyCode, KeyEventKind, KeyModifiers};
use ratatui::layout::{Constraint, Direction, Layout, Rect};
use ratatui::style::{Color, Modifier, Style};
use ratatui::widgets::{Block, Borders, List, ListItem, ListState, Paragraph, Widget, WidgetRef};
use ratatui::text::{Line, Span, Text};
use sandpolis_client::tui::EventHandler;
use sandpolis_core::InstanceId;
use std::path::{Path, PathBuf};
use std::fs;
use std::time::SystemTime;

/// Represents a filesystem entry with metadata
#[derive(Debug, Clone)]
pub struct FileEntry {
    pub name: String,
    pub path: PathBuf,
    pub is_directory: bool,
    pub size: Option<u64>,
    pub modified: Option<SystemTime>,
    pub permissions: Option<String>,
}

impl FileEntry {
    fn from_path(path: &Path) -> Option<Self> {
        let metadata = fs::metadata(path).ok()?;
        let name = path.file_name()?.to_string_lossy().to_string();
        
        Some(FileEntry {
            name,
            path: path.to_path_buf(),
            is_directory: metadata.is_dir(),
            size: if metadata.is_file() { Some(metadata.len()) } else { None },
            modified: metadata.modified().ok(),
            permissions: Self::format_permissions(&metadata),
        })
    }

    #[cfg(unix)]
    fn format_permissions(metadata: &fs::Metadata) -> Option<String> {
        use std::os::unix::fs::PermissionsExt;
        let mode = metadata.permissions().mode();
        Some(format!(
            "{}{}{}{}{}{}{}{}{}{}",
            if metadata.is_dir() { 'd' } else { '-' },
            if mode & 0o400 != 0 { 'r' } else { '-' },
            if mode & 0o200 != 0 { 'w' } else { '-' },
            if mode & 0o100 != 0 { 'x' } else { '-' },
            if mode & 0o040 != 0 { 'r' } else { '-' },
            if mode & 0o020 != 0 { 'w' } else { '-' },
            if mode & 0o010 != 0 { 'x' } else { '-' },
            if mode & 0o004 != 0 { 'r' } else { '-' },
            if mode & 0o002 != 0 { 'w' } else { '-' },
            if mode & 0o001 != 0 { 'x' } else { '-' },
        ))
    }

    #[cfg(not(unix))]
    fn format_permissions(_metadata: &fs::Metadata) -> Option<String> {
        None
    }

    fn format_size(&self) -> String {
        match self.size {
            Some(size) => format_bytes(size),
            None if self.is_directory => "<DIR>".to_string(),
            None => "-".to_string(),
        }
    }

    fn format_modified(&self) -> String {
        match self.modified {
            Some(time) => {
                let datetime: DateTime<Local> = time.into();
                datetime.format("%Y-%m-%d %H:%M").to_string()
            }
            None => "-".to_string(),
        }
    }
}

/// Helper function to format file sizes in human-readable format
fn format_bytes(bytes: u64) -> String {
    const UNITS: &[&str] = &["B", "KB", "MB", "GB", "TB"];
    let mut size = bytes as f64;
    let mut unit_index = 0;

    while size >= 1024.0 && unit_index < UNITS.len() - 1 {
        size /= 1024.0;
        unit_index += 1;
    }

    if unit_index == 0 {
        format!("{} {}", size as u64, UNITS[unit_index])
    } else {
        format!("{:.1} {}", size, UNITS[unit_index])
    }
}

/// Main filesystem viewer widget
pub struct FilesystemViewerWidget {
    pub instance: InstanceId,
    pub filesystem: FilesystemLayer,
    pub current_path: PathBuf,
    pub entries: Vec<FileEntry>,
    pub list_state: ListState,
    pub show_hidden: bool,
    pub sort_by: SortBy,
    pub sort_ascending: bool,
    pub status_message: String,
    pub view_mode: ViewMode,
}

#[derive(Debug, Clone, Copy, PartialEq)]
pub enum SortBy {
    Name,
    Size,
    Modified,
    Type,
}

#[derive(Debug, Clone, Copy, PartialEq)]
pub enum ViewMode {
    List,
    Detailed,
}

impl FilesystemViewerWidget {
    pub fn new(instance: InstanceId, filesystem: FilesystemLayer, initial_path: Option<PathBuf>) -> Self {
        let current_path = initial_path.unwrap_or_else(|| std::env::current_dir().unwrap_or_else(|_| PathBuf::from("/")));
        
        let mut widget = Self {
            instance,
            filesystem,
            current_path,
            entries: Vec::new(),
            list_state: ListState::default(),
            show_hidden: false,
            sort_by: SortBy::Name,
            sort_ascending: true,
            status_message: "Ready".to_string(),
            view_mode: ViewMode::Detailed,
        };
        
        widget.refresh_directory();
        widget
    }

    /// Refresh the current directory listing
    pub fn refresh_directory(&mut self) {
        match self.load_directory(&self.current_path.clone()) {
            Ok(entries) => {
                self.entries = entries;
                self.sort_entries();
                self.status_message = format!("Loaded {} items", self.entries.len());
                
                // Reset selection to first item
                if !self.entries.is_empty() {
                    self.list_state.select(Some(0));
                } else {
                    self.list_state.select(None);
                }
            }
            Err(e) => {
                self.status_message = format!("Error: {}", e);
                self.entries.clear();
                self.list_state.select(None);
            }
        }
    }

    /// Load directory contents
    fn load_directory(&self, path: &Path) -> Result<Vec<FileEntry>, std::io::Error> {
        let mut entries = Vec::new();
        
        // Add parent directory entry if not at root
        if path.parent().is_some() {
            let parent_entry = FileEntry {
                name: "..".to_string(),
                path: path.parent().unwrap().to_path_buf(),
                is_directory: true,
                size: None,
                modified: None,
                permissions: None,
            };
            entries.push(parent_entry);
        }

        // Read directory entries
        for entry in fs::read_dir(path)? {
            let entry = entry?;
            let file_name = entry.file_name().to_string_lossy().to_string();
            
            // Skip hidden files if not showing them
            if !self.show_hidden && file_name.starts_with('.') {
                continue;
            }

            if let Some(file_entry) = FileEntry::from_path(&entry.path()) {
                entries.push(file_entry);
            }
        }

        Ok(entries)
    }

    /// Sort entries based on current sort settings
    fn sort_entries(&mut self) {
        self.entries.sort_by(|a, b| {
            // Always keep ".." at the top
            if a.name == ".." {
                return std::cmp::Ordering::Less;
            }
            if b.name == ".." {
                return std::cmp::Ordering::Greater;
            }

            // Directories first, then files
            match (a.is_directory, b.is_directory) {
                (true, false) => return std::cmp::Ordering::Less,
                (false, true) => return std::cmp::Ordering::Greater,
                _ => {}
            }

            let ordering = match self.sort_by {
                SortBy::Name => a.name.to_lowercase().cmp(&b.name.to_lowercase()),
                SortBy::Size => a.size.unwrap_or(0).cmp(&b.size.unwrap_or(0)),
                SortBy::Modified => a.modified.cmp(&b.modified),
                SortBy::Type => {
                    let a_ext = a.path.extension().unwrap_or_default();
                    let b_ext = b.path.extension().unwrap_or_default();
                    a_ext.cmp(&b_ext)
                }
            };

            if self.sort_ascending {
                ordering
            } else {
                ordering.reverse()
            }
        });
    }

    /// Navigate to the selected directory or file
    pub fn navigate_to_selected(&mut self) {
        if let Some(selected) = self.list_state.selected() {
            if let Some(entry) = self.entries.get(selected) {
                if entry.is_directory {
                    self.current_path = entry.path.clone();
                    self.refresh_directory();
                } else {
                    self.status_message = format!("Selected file: {}", entry.name);
                }
            }
        }
    }

    /// Navigate up one directory level
    pub fn navigate_up(&mut self) {
        if let Some(parent) = self.current_path.parent() {
            self.current_path = parent.to_path_buf();
            self.refresh_directory();
        }
    }

    /// Select next item in the list
    pub fn select_next(&mut self) {
        if self.entries.is_empty() {
            return;
        }

        let selected = match self.list_state.selected() {
            Some(i) => {
                if i >= self.entries.len() - 1 {
                    0
                } else {
                    i + 1
                }
            }
            None => 0,
        };
        self.list_state.select(Some(selected));
    }

    /// Select previous item in the list
    pub fn select_previous(&mut self) {
        if self.entries.is_empty() {
            return;
        }

        let selected = match self.list_state.selected() {
            Some(i) => {
                if i == 0 {
                    self.entries.len() - 1
                } else {
                    i - 1
                }
            }
            None => 0,
        };
        self.list_state.select(Some(selected));
    }

    /// Toggle hidden files visibility
    pub fn toggle_hidden(&mut self) {
        self.show_hidden = !self.show_hidden;
        self.refresh_directory();
    }

    /// Change sort order
    pub fn cycle_sort(&mut self) {
        self.sort_by = match self.sort_by {
            SortBy::Name => SortBy::Size,
            SortBy::Size => SortBy::Modified,
            SortBy::Modified => SortBy::Type,
            SortBy::Type => SortBy::Name,
        };
        self.sort_entries();
    }

    /// Toggle sort direction
    pub fn toggle_sort_direction(&mut self) {
        self.sort_ascending = !self.sort_ascending;
        self.sort_entries();
    }

    /// Toggle view mode
    pub fn toggle_view_mode(&mut self) {
        self.view_mode = match self.view_mode {
            ViewMode::List => ViewMode::Detailed,
            ViewMode::Detailed => ViewMode::List,
        };
    }
}

impl WidgetRef for FilesystemViewerWidget {
    fn render_ref(&self, area: Rect, buf: &mut ratatui::buffer::Buffer) {
        // Split the area into main content and status bar
        let chunks = Layout::default()
            .direction(Direction::Vertical)
            .constraints([
                Constraint::Length(3),  // Header
                Constraint::Min(5),     // File list
                Constraint::Length(3),  // Status bar
            ])
            .split(area);

        // Render header
        self.render_header(chunks[0], buf);

        // Render file list
        self.render_file_list(chunks[1], buf);

        // Render status bar
        self.render_status_bar(chunks[2], buf);
    }
}

impl FilesystemViewerWidget {
    fn render_header(&self, area: Rect, buf: &mut ratatui::buffer::Buffer) {
        let current_path_display = self.current_path.display().to_string();
        let sort_indicator = match self.sort_by {
            SortBy::Name => "Name",
            SortBy::Size => "Size", 
            SortBy::Modified => "Modified",
            SortBy::Type => "Type",
        };
        let sort_direction = if self.sort_ascending { "↑" } else { "↓" };

        let header_text = vec![
            Line::from(vec![
                Span::styled("Path: ", Style::default().fg(Color::Yellow)),
                Span::raw(&current_path_display),
            ]),
            Line::from(vec![
                Span::styled("Sort: ", Style::default().fg(Color::Cyan)),
                Span::raw(format!("{} {}", sort_indicator, sort_direction)),
                Span::raw("  "),
                Span::styled("View: ", Style::default().fg(Color::Cyan)),
                Span::raw(format!("{:?}", self.view_mode)),
                Span::raw("  "),
                Span::styled("Hidden: ", Style::default().fg(Color::Cyan)),
                Span::raw(if self.show_hidden { "On" } else { "Off" }),
            ]),
        ];

        let header = Paragraph::new(Text::from(header_text))
            .block(Block::default()
                .title("Filesystem Viewer")
                .borders(Borders::ALL)
                .border_style(Style::default().fg(Color::Green)));

        header.render(area, buf);
    }

    fn render_file_list(&self, area: Rect, buf: &mut ratatui::buffer::Buffer) {
        let items: Vec<ListItem> = self.entries
            .iter()
            .map(|entry| {
                let style = if entry.is_directory {
                    Style::default().fg(Color::Blue).add_modifier(Modifier::BOLD)
                } else {
                    Style::default()
                };

                let line = match self.view_mode {
                    ViewMode::List => {
                        Line::from(vec![
                            Span::styled(
                                format!("{}{}", 
                                    if entry.is_directory { "📁 " } else { "📄 " },
                                    entry.name
                                ),
                                style
                            )
                        ])
                    }
                    ViewMode::Detailed => {
                        Line::from(vec![
                            Span::styled(
                                format!("{:<30}", entry.name),
                                style
                            ),
                            Span::styled(
                                format!("{:>10}", entry.format_size()),
                                Style::default().fg(Color::Gray)
                            ),
                            Span::raw("  "),
                            Span::styled(
                                format!("{:>16}", entry.format_modified()),
                                Style::default().fg(Color::Gray)
                            ),
                            if let Some(perms) = &entry.permissions {
                                Span::styled(
                                    format!("  {}", perms),
                                    Style::default().fg(Color::Gray)
                                )
                            } else {
                                Span::raw("")
                            },
                        ])
                    }
                };

                ListItem::new(line)
            })
            .collect();

        let list = List::new(items)
            .block(Block::default()
                .title(format!("Contents ({} items)", self.entries.len()))
                .borders(Borders::ALL))
            .highlight_style(Style::default().bg(Color::DarkGray))
            .highlight_symbol(">> ");

        ratatui::widgets::StatefulWidget::render(list, area, buf, &mut self.list_state.clone());
    }

    fn render_status_bar(&self, area: Rect, buf: &mut ratatui::buffer::Buffer) {
        let selected_info = if let Some(selected) = self.list_state.selected() {
            if let Some(entry) = self.entries.get(selected) {
                format!("Selected: {} ({})", entry.name, entry.path.display())
            } else {
                "No selection".to_string()
            }
        } else {
            "No selection".to_string()
        };

        let status_text = vec![
            Line::from(vec![
                Span::styled("Status: ", Style::default().fg(Color::Yellow)),
                Span::raw(&self.status_message),
            ]),
            Line::from(vec![
                Span::raw(&selected_info),
            ]),
            Line::from(vec![
                Span::styled("Controls: ", Style::default().fg(Color::Cyan)),
                Span::raw("↑/↓ Navigate, Enter Open, Backspace Up, H Toggle Hidden, S Sort, V View Mode"),
            ]),
        ];

        let status = Paragraph::new(Text::from(status_text))
            .block(Block::default()
                .title("Status")
                .borders(Borders::ALL)
                .border_style(Style::default().fg(Color::Blue)));

        status.render(area, buf);
    }
}

impl EventHandler for FilesystemViewerWidget {
    fn handle_event(&mut self, event: &Event) {
        if let Event::Key(key) = event {
            if key.kind == KeyEventKind::Press {
                match key.code {
                    KeyCode::Up | KeyCode::Char('k') => {
                        self.select_previous();
                    }
                    KeyCode::Down | KeyCode::Char('j') => {
                        self.select_next();
                    }
                    KeyCode::Enter => {
                        self.navigate_to_selected();
                    }
                    KeyCode::Backspace => {
                        self.navigate_up();
                    }
                    KeyCode::Char('h') | KeyCode::Char('H') => {
                        self.toggle_hidden();
                    }
                    KeyCode::Char('s') | KeyCode::Char('S') => {
                        self.cycle_sort();
                    }
                    KeyCode::Char('r') | KeyCode::Char('R') => {
                        self.toggle_sort_direction();
                    }
                    KeyCode::Char('v') | KeyCode::Char('V') => {
                        self.toggle_view_mode();
                    }
                    KeyCode::Char('5') if key.modifiers.contains(KeyModifiers::CONTROL) => {
                        self.refresh_directory();
                    }
                    KeyCode::Home => {
                        if !self.entries.is_empty() {
                            self.list_state.select(Some(0));
                        }
                    }
                    KeyCode::End => {
                        if !self.entries.is_empty() {
                            self.list_state.select(Some(self.entries.len() - 1));
                        }
                    }
                    _ => {}
                }
            }
        }
    }
}