use bevy::prelude::bevy_main;
use sandpolis::CommandLine;

#[bevy_main]
fn main() {
    sandpolis::client::main(CommandLine::default());
}
