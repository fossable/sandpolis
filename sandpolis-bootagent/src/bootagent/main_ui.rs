pub struct MainUI {
    gop: &mut GraphicsOutput,
    width: usize,
    height: usize,
}

impl MainUI {
    pub fn new(resolutions: Vec<(usize, usize)>) -> Option<MainUI> {
        let gop = bt
            .locate_protocol::<GraphicsOutput>()?
            .expect("Warnings encountered while opening GOP");
        let gop = unsafe { &mut *gop.get() };

        // Set screen resolution
        set_graphics_mode(&gop, resolutions)?;

        // Set background
        set_background(&gop, BltPixel::new(100, 149, 237));

        MainUI { gop: gop }
    }

    // Fill the entire screen with color.
    fn set_background(self, gop: &mut GraphicsOutput, color: BltPixel) {
        let op = BltOp::VideoFill {
            color: color,
            dest: (0, 0),
            dims: (self.width, self.height),
        };

        gop.blt(op);
    }
}

fn set_graphics_mode(gop: &mut GraphicsOutput, resolutions: Vec<(usize, usize)>) -> Result<()> {
    let modes = gop
        .modes()
        .map(|mode| mode.expect("Warnings encountered while querying mode"));

    for resolution in resolutions {
        match modes.find(|mode| {
            let info = mode.info();
            mode.info().resolution() == resolution
        }) {
            Some(mode) => match gop.set_mode(&mode) {
                Ok(()) => return Ok(()),
            },
        }
    }

    Err()
}

fn draw_image(gop: &mut GraphicsOutput, image: &[[[u8; 3]]], y: usize, x: usize) {
    let mut fb = gop.frame_buffer();

    for row in y1..y2 {
        for column in x1..x2 {
            unsafe {
                let pixel_index = (row * stride) + column;
                let pixel_base = 4 * pixel_index;
                fb.write_value(pixel_base, image[y][x]);
            }
        }
    }
}

// A Textline is a single line of bounded text.
pub struct Textline {
    text: String,
    y: u32,
    x: u32,
    width: u32,
    height: u32,
    font: Font,
}

impl Textline {
    pub fn render(&self) {
        let mut layout = Layout::new(CoordinateSystem::PositiveYDown);

        layout.append(
            &[self.font],
            &TextStyle::with_user_data(text, 35.0, 0, 10u8),
        );

        for glyph in layout.glyphs() {
            // TODO
        }
    }
}
