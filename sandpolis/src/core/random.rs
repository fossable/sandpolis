use std::num::NonZeroU32;

use rand::distributions::{Alphanumeric, DistString};
use rand::{thread_rng, Rng};

pub fn next_alphanumeric(length: usize) -> String {
    Alphanumeric.sample_string(&mut rand::thread_rng(), length)
}

pub fn next_nonzero_u32() -> NonZeroU32 {
    let mut rng = rand::thread_rng();
    rng.gen()
}
