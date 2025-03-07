pub struct ServerListWidget {
    pub selected_index: u32,
}

impl ServerListWidget {
    pub fn new() -> Self {
        todo!()
    }
}

impl WidgetRef for &ServerListWidget {
    fn render_ref(&self, area: ratatui::layout::Rect, buf: &mut ratatui::buffer::Buffer) {}
}
