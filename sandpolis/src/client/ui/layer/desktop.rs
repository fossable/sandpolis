use bevy::prelude::*;

use crate::{client::ui::CurrentLayer, core::Layer};

pub fn check_layer_active(current_layer: Res<CurrentLayer>) -> bool {
    return **current_layer == Layer::Desktop;
}

pub fn handle_layer(mut commands: Commands) {}
