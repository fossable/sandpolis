//! Pure-Rust ARGB rotation/mirroring with libyuv-compatible signatures.
//!
//! Replaces the libyuv FFI bindings that scrap generated at build time, so no
//! native libyuv is needed. Pixels are treated as opaque 4-byte units, so any
//! 32-bit format (ARGB/BGRA/...) works.

#![allow(non_snake_case, non_camel_case_types)]

const BPP: usize = 4;

#[repr(i32)]
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum RotationMode {
    kRotate0 = 0,
    kRotate90 = 90,
    kRotate180 = 180,
    kRotate270 = 270,
}

#[inline]
fn px(base: *const u8, stride: usize, row: usize, col: usize) -> *const u8 {
    unsafe { base.add(row * stride + col * BPP) }
}

#[inline]
fn px_mut(base: *mut u8, stride: usize, row: usize, col: usize) -> *mut u8 {
    unsafe { base.add(row * stride + col * BPP) }
}

#[inline]
unsafe fn copy_px(src: *const u8, dst: *mut u8) {
    std::ptr::copy_nonoverlapping(src, dst, BPP);
}

/// Rotate a 32-bit image. `width`/`height` are the *source* dimensions; for
/// 90°/270° rotations the destination is `height` x `width`. Following
/// libyuv, a negative `height` reads the source bottom-up.
///
/// # Safety
/// `src` must contain `height` rows of `src_stride` bytes and `dst` must have
/// room for the rotated image with `dst_stride`-byte rows. The buffers must
/// not overlap.
pub unsafe fn ARGBRotate(
    src: *const u8,
    src_stride: i32,
    dst: *mut u8,
    dst_stride: i32,
    width: i32,
    height: i32,
    mode: RotationMode,
) -> i32 {
    if src.is_null() || dst.is_null() || width <= 0 || height == 0 {
        return -1;
    }

    let w = width as usize;
    let h = height.unsigned_abs() as usize;
    // Negative height means the source is stored bottom-up.
    let flipped = height < 0;
    let ss = src_stride as usize;
    let ds = dst_stride as usize;

    let src_row = |r: usize| if flipped { h - 1 - r } else { r };

    unsafe {
        match mode {
            RotationMode::kRotate0 => {
                for r in 0..h {
                    std::ptr::copy_nonoverlapping(
                        px(src, ss, src_row(r), 0),
                        px_mut(dst, ds, r, 0),
                        w * BPP,
                    );
                }
            }
            // 90° clockwise: dst is h x w, dst(r, c) = src(h-1-c, r)
            RotationMode::kRotate90 => {
                for r in 0..w {
                    for c in 0..h {
                        copy_px(px(src, ss, src_row(h - 1 - c), r), px_mut(dst, ds, r, c));
                    }
                }
            }
            RotationMode::kRotate180 => {
                for r in 0..h {
                    for c in 0..w {
                        copy_px(
                            px(src, ss, src_row(h - 1 - r), w - 1 - c),
                            px_mut(dst, ds, r, c),
                        );
                    }
                }
            }
            // 270° clockwise (90° CCW): dst is h x w, dst(r, c) = src(c, w-1-r)
            RotationMode::kRotate270 => {
                for r in 0..w {
                    for c in 0..h {
                        copy_px(px(src, ss, src_row(c), w - 1 - r), px_mut(dst, ds, r, c));
                    }
                }
            }
        }
    }

    0
}

/// Mirror a 32-bit image horizontally. Following libyuv, a negative `height`
/// reads the source bottom-up (mirror + vertical flip).
///
/// # Safety
/// Same buffer requirements as [`ARGBRotate`]; `src` and `dst` must not
/// overlap.
pub unsafe fn ARGBMirror(
    src: *const u8,
    src_stride: i32,
    dst: *mut u8,
    dst_stride: i32,
    width: i32,
    height: i32,
) -> i32 {
    if src.is_null() || dst.is_null() || width <= 0 || height == 0 {
        return -1;
    }

    let w = width as usize;
    let h = height.unsigned_abs() as usize;
    let flipped = height < 0;
    let ss = src_stride as usize;
    let ds = dst_stride as usize;

    unsafe {
        for r in 0..h {
            let sr = if flipped { h - 1 - r } else { r };
            for c in 0..w {
                copy_px(px(src, ss, sr, w - 1 - c), px_mut(dst, ds, r, c));
            }
        }
    }

    0
}

#[cfg(test)]
mod tests {
    use super::*;

    // 2x3 image (rows of pixels labeled by first byte)
    fn src() -> Vec<u8> {
        let mut v = Vec::new();
        for p in 0..6u8 {
            v.extend_from_slice(&[p, 100 + p, 200 + p.wrapping_mul(2), 255]);
        }
        v
    }

    fn first_bytes(buf: &[u8], n: usize) -> Vec<u8> {
        (0..n).map(|i| buf[i * BPP]).collect()
    }

    #[test]
    fn rotate90() {
        // src (3 wide, 2 high):  0 1 2      dst (2 wide, 3 high):  3 0
        //                        3 4 5                             4 1
        //                                                          5 2
        let s = src();
        let mut d = vec![0u8; s.len()];
        let ret = unsafe { ARGBRotate(s.as_ptr(), 12, d.as_mut_ptr(), 8, 3, 2, RotationMode::kRotate90) };
        assert_eq!(ret, 0);
        assert_eq!(first_bytes(&d, 6), vec![3, 0, 4, 1, 5, 2]);
    }

    #[test]
    fn rotate180() {
        let s = src();
        let mut d = vec![0u8; s.len()];
        unsafe { ARGBRotate(s.as_ptr(), 12, d.as_mut_ptr(), 12, 3, 2, RotationMode::kRotate180) };
        assert_eq!(first_bytes(&d, 6), vec![5, 4, 3, 2, 1, 0]);
    }

    #[test]
    fn rotate270() {
        // dst (2 wide, 3 high):  2 5
        //                        1 4
        //                        0 3
        let s = src();
        let mut d = vec![0u8; s.len()];
        unsafe { ARGBRotate(s.as_ptr(), 12, d.as_mut_ptr(), 8, 3, 2, RotationMode::kRotate270) };
        assert_eq!(first_bytes(&d, 6), vec![2, 5, 1, 4, 0, 3]);
    }

    #[test]
    fn mirror() {
        let s = src();
        let mut d = vec![0u8; s.len()];
        let ret = unsafe { ARGBMirror(s.as_ptr(), 12, d.as_mut_ptr(), 12, 3, 2) };
        assert_eq!(ret, 0);
        assert_eq!(first_bytes(&d, 6), vec![2, 1, 0, 5, 4, 3]);
    }
}
