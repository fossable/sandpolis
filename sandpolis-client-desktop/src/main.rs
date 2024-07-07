use dioxus::prelude::*;
use sandpolis::client::world::{World, WorldSvg};

fn main() {
    let mut world = World::new();
    launch_desktop(WorldSvg);
}
