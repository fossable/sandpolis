/// Format a lowercase UUID in the standard format.
#[inline]
pub const fn uuid(src: u128) -> [u8; 36] {
    let src: [u8; 16] = src.to_be_bytes();
    let lut = [
        b'0', b'1', b'2', b'3', b'4', b'5', b'6', b'7', b'8', b'9', b'a', b'b', b'c', b'd', b'e',
        b'f',
    ];

    let groups = [(0, 8), (9, 13), (14, 18), (19, 23), (24, 36)];
    let mut dst = [0; 36];

    let mut group_idx = 0;
    let mut i = 0;
    while group_idx < 5 {
        let (start, end) = groups[group_idx];
        let mut j = start;
        while j < end {
            let x = src[i];
            i += 1;

            dst[j] = lut[(x >> 4) as usize];
            dst[j + 1] = lut[(x & 0x0f) as usize];
            j += 2;
        }
        if group_idx < 4 {
            dst[end] = b'-';
        }
        group_idx += 1;
    }
    dst
}
