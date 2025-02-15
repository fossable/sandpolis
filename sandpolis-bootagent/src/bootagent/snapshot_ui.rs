use alloc::vec::Vec;
use core::fmt::format;
use log::{debug, error};

enum UiBlockState {
    /// The block has not been read
    Unseen,

    // The block is currently being read into memory and hashed
    Hashing,

    // The block has been hashed and can be removed from memory if it does not need to be synced
    Hashed,

    // The block is currently being transferred to or from a remote location
    Syncing,

    // The block has been synced successfully and can be removed from memory
    Synced,
}

/// UiBlock is a representation of a filesystem block in the UI.
struct UiBlock {
    /// The current state of this block
    pub state: UiBlockState,

    /// The block's global Y coordinate
    pub y: i32,

    /// The block's global X coordinate
    pub x: i32,

    /// The block's global index
    pub index: i32,
}

impl UiBlock {
    pub fn refresh(&self) {
        // Determine the block's appearance according to state
        let ch = match self.state {
            UiBlockState::Unseen => ".",
            UiBlockState::Hashed => "▢",
            UiBlockState::Synced => "▣",
            _ => ".",
        };

        //self.win.mvaddstr(self.y, self.x, &ch);
        //self.win.refresh();
    }
}

fn init_win_header(state: &UiState) {

    // Write sixel image
    //if let Some(sixel) = BinaryAssets::get("sandpolis.sixel") {
    //	state.win_header.mv(0, 0);
    //	print!(String::from_utf8_lossy(&sixel));
    //}
}

// Initialize the stats window and static content
fn init_win_stats(state: &UiState) {

    //state.win_stats.mvaddstr(0, 0, "Creating snapshot of device: /dev/sda1. Please do not interrupt this process.");

    //state.win_stats.mvaddstr(2, 0, "Time remaining");
    //state.win_stats.mvaddstr(3, 0, "Network upload");
    //state.win_stats.mvaddstr(4, 0, "Network download");
    //state.win_stats.mvaddstr(5, 0, "Disk Read");
    //state.win_stats.mvaddstr(6, 0, "Disk Write");

    //state.win_stats.refresh();
}

fn update_win_stats(state: &UiState) {
    let time_remaining = "";
    let net_upload = format!("↑ /s");
    let net_download = format!("↓ /s");
    let disk_read = format!("↑ /s");
    let disk_write = format!("↓ /s");

    //state.win_stats.mvaddstr(2, UiState::DIALOG_WIDTH - time_remaining.chars().count() as i32, time_remaining);
    //state.win_stats.mvaddstr(3, UiState::DIALOG_WIDTH - net_upload.chars().count() as i32, net_upload);
    //state.win_stats.mvaddstr(4, UiState::DIALOG_WIDTH - net_download.chars().count() as i32, net_download);
    //state.win_stats.mvaddstr(5, UiState::DIALOG_WIDTH - disk_read.chars().count() as i32, disk_read);
    //state.win_stats.mvaddstr(6, UiState::DIALOG_WIDTH - disk_write.chars().count() as i32, disk_write);

    //state.win_stats.refresh();
}

pub struct CenterArea {
    width: u32,
    height: u32,

    top_left_y: u32,
    top_left_x: u32,
    top_right_y: u32,
    top_right_x: u32,
    bottom_left_y: u32,
    bottom_left_x: u32,
    bottom_right_y: u32,
    bottom_right_x: u32,

    disk_read: Textline,
    disk_write: Textline,
}

impl CenterArea {
    pub fn new(screen_width: u32, screen_height: u32) {
        // Center area is sized according to screen height
        let height = screen_height / 4;
        let width = height * 1.618;

        CenterArea {
            width: width,
            height: height,

            top_left_y: (screen_height - height) / 2,
            top_left_x: (screen_width - width) / 2,
            top_right_y: (screen_height - height) / 2,
            top_right_x: (screen_width + width) / 2,
            bottom_left_y: (screen_height + height) / 2,
            bottom_left_x: (screen_width - width) / 2,
            bottom_right_y: (screen_height + height) / 2,
            bottom_right_x: (screen_width + width) / 2,
        }
    }
}

pub struct SnapshotUI<'a> {
    ui: &'a MainUI,
    blocks: Vec<UiBlock>,
}

impl SnapshotUI<'_> {
    pub fn new(ui: MainUI) -> SnapshotUI {
        // Compute the center area
        let center = CenterArea::new(ui.width, ui.height);

        // Allocate blocks
        let mut blocks = Vec::new();

        // Prepare an offset which will be used to create a rectangular gap
        let mut offset = 0;
        let mut i = -1;

        while i < 10 {
            i += 1;

            // Convert block index to absolute coordinates
            let mut y = (i + offset) / ui.width;
            let mut x = (i + offset) % ui.height;

            // If we're about to enter the dialog, add offset and retry this iteration
            if y >= center.top_left_y && y <= center.bottom_left_y && x == center.top_left_x {
                offset += center.width;
                i -= 1;
                continue;
            }

            // Create the block
            blocks.push(UiBlock {
                state: UiBlockState::Unseen,
                y: y,
                x: x,
                index: i,
            });
        }

        SnapshotUI {
            ui: ui,
            center: center,
            blocks: blocks,
        }
    }

    pub fn start(&self) {
        for block in state.blocks {
            block.refresh();
        }

        // Start UI update loop
        loop {
            self.stall(200_000);
        }
    }
}
