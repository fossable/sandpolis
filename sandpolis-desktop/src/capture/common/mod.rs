use std::ffi::c_void;

cfg_if::cfg_if! {
    if #[cfg(quartz)] {
        mod quartz;
        pub use self::quartz::*;
    } else if #[cfg(x11)] {
        mod linux;
        mod wayland;
        mod x11;
        pub use self::linux::*;
        pub use self::wayland::set_map_err;
        pub use self::x11::PixelBuffer;
    } else if #[cfg(dxgi)] {
        mod dxgi;
        pub use self::dxgi::*;
    } else if #[cfg(target_os = "android")] {
        mod android;
        pub use self::android::*;
    } else {
        //TODO: Fallback implementation.
    }
}

mod rotate;
pub use self::rotate::*;

#[inline]
pub fn would_block_if_equal(old: &mut Vec<u8>, b: &[u8]) -> std::io::Result<()> {
    // does this really help?
    if b == &old[..] {
        return Err(std::io::ErrorKind::WouldBlock.into());
    }
    old.resize(b.len(), 0);
    old.copy_from_slice(b);
    Ok(())
}

pub trait TraitCapturer {
    fn frame<'a>(&'a mut self, timeout: std::time::Duration) -> std::io::Result<Frame<'a>>;

    #[cfg(windows)]
    fn is_gdi(&self) -> bool;
    #[cfg(windows)]
    fn set_gdi(&mut self) -> bool;
}

#[derive(Debug, Clone, Copy)]
pub struct AdapterDevice {
    pub device: *mut c_void,
    pub vendor_id: ::std::os::raw::c_uint,
    pub luid: i64,
}

impl Default for AdapterDevice {
    fn default() -> Self {
        Self {
            device: std::ptr::null_mut(),
            vendor_id: Default::default(),
            luid: Default::default(),
        }
    }
}

pub trait TraitPixelBuffer {
    fn data(&self) -> &[u8];

    fn width(&self) -> usize;

    fn height(&self) -> usize;

    fn stride(&self) -> Vec<usize>;

    fn pixfmt(&self) -> Pixfmt;
}

pub enum Frame<'a> {
    PixelBuffer(PixelBuffer<'a>),
    Texture((*mut c_void, usize)),
}

impl Frame<'_> {
    pub fn valid<'a>(&'a self) -> bool {
        match self {
            Frame::PixelBuffer(pixelbuffer) => !pixelbuffer.data().is_empty(),
            Frame::Texture((texture, _)) => !texture.is_null(),
        }
    }
}

#[derive(Debug, PartialEq, Eq, Clone, Copy)]
pub enum Pixfmt {
    BGRA,
    RGBA,
    RGB565LE,
    I420,
    NV12,
    I444,
}

impl Pixfmt {
    pub fn bpp(&self) -> usize {
        match self {
            Pixfmt::BGRA | Pixfmt::RGBA => 32,
            Pixfmt::RGB565LE => 16,
            Pixfmt::I420 | Pixfmt::NV12 => 12,
            Pixfmt::I444 => 24,
        }
    }

    pub fn bytes_per_pixel(&self) -> usize {
        (self.bpp() + 7) / 8
    }
}

#[cfg(x11)]
#[inline]
pub fn is_x11() -> bool {
    crate::platform::linux::is_x11_or_headless()
}

#[derive(Debug)]
pub enum Error {
    FailedCall(String),
    BadPtr(String),
}

impl std::fmt::Display for Error {
    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::result::Result<(), std::fmt::Error> {
        write!(f, "{:?}", self)
    }
}

impl std::error::Error for Error {}

pub type Result<T> = std::result::Result<T, Error>;
