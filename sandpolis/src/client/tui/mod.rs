use ratatui_image::picker::Picker;
use std::sync::LazyLock;

pub mod agent_list;
pub mod server_list;

/// Terminal graphics capabilities (sixel/kitty/etc.), queried once. Used by
/// widgets that render images, e.g. the server banner in the server list.
pub static GRAPHICS: LazyLock<Option<Picker>> = LazyLock::new(|| Picker::from_query_stdio().ok());
