use bevy::prelude::*;
use bevy_egui::EguiContexts;
use bevy_rapier2d::{
    dynamics::RigidBody,
    geometry::{Collider, Restitution},
};

use crate::core::InstanceId;

#[derive(Component, Clone)]
pub struct NodeId(InstanceId);

#[derive(Bundle)]
pub struct Node {
    pub id: NodeId,
    pub collier: Collider,
    pub rigid_body: RigidBody,
    // /// User indication of whether an entity is visible
    pub visibility: Visibility,
    pub sprite: SpriteBundle,
    // // Inherited visibility of an entity.
    // pub inherited_visibility: InheritedVisibility,
    pub restitution: Restitution,
    // // Indication of whether an entity is visible in any view.
    // pub view_visibility: ViewVisibility,
}

pub fn spawn_node(
    asset_server: &AssetServer,
    commands: &mut Commands,
    contexts: &mut EguiContexts,
    instance_id: InstanceId,
    os_type: os_info::Type,
) {
    let node = Node {
        id: NodeId(instance_id),
        collier: Collider::ball(50.0),
        rigid_body: RigidBody::Dynamic,
        // visibility: todo!(),
        restitution: Restitution::coefficient(0.7),
        visibility: Visibility::Visible,
        sprite: SpriteBundle {
            texture: asset_server.load(get_os_image(os_type)),
            ..default()
        },
    };

    // TODO store a handle somehow
    // egui::Window::new("Hello")
    //     .resizable(false)
    //     .movable(false)
    //     .show(contexts.ctx_mut(), |ui| {
    //         ui.label("world");
    //     });

    commands.spawn(node);
}

pub fn get_os_image(os_type: os_info::Type) -> String {
    match os_type {
        os_info::Type::AIX => todo!(),
        os_info::Type::AlmaLinux => todo!(),
        os_info::Type::Alpaquita => todo!(),
        os_info::Type::Alpine => todo!(),
        os_info::Type::Amazon => todo!(),
        os_info::Type::Android => todo!(),
        os_info::Type::Arch => "os/arch_linux.png",
        os_info::Type::Artix => todo!(),
        os_info::Type::CentOS => todo!(),
        os_info::Type::Debian => todo!(),
        os_info::Type::DragonFly => todo!(),
        os_info::Type::Emscripten => todo!(),
        os_info::Type::EndeavourOS => todo!(),
        os_info::Type::Fedora => todo!(),
        os_info::Type::FreeBSD => todo!(),
        os_info::Type::Garuda => todo!(),
        os_info::Type::Gentoo => todo!(),
        os_info::Type::HardenedBSD => todo!(),
        os_info::Type::Illumos => todo!(),
        os_info::Type::Kali => todo!(),
        os_info::Type::Linux => todo!(),
        os_info::Type::Mabox => todo!(),
        os_info::Type::Macos => todo!(),
        os_info::Type::Manjaro => todo!(),
        os_info::Type::Mariner => todo!(),
        os_info::Type::MidnightBSD => todo!(),
        os_info::Type::Mint => todo!(),
        os_info::Type::NetBSD => todo!(),
        os_info::Type::NixOS => todo!(),
        os_info::Type::OpenBSD => todo!(),
        os_info::Type::OpenCloudOS => todo!(),
        os_info::Type::openEuler => todo!(),
        os_info::Type::openSUSE => todo!(),
        os_info::Type::OracleLinux => todo!(),
        os_info::Type::Pop => todo!(),
        os_info::Type::Raspbian => todo!(),
        os_info::Type::Redhat => todo!(),
        os_info::Type::RedHatEnterprise => todo!(),
        os_info::Type::Redox => todo!(),
        os_info::Type::RockyLinux => todo!(),
        os_info::Type::Solus => todo!(),
        os_info::Type::SUSE => todo!(),
        os_info::Type::Ubuntu => todo!(),
        os_info::Type::Ultramarine => todo!(),
        os_info::Type::Void => todo!(),
        os_info::Type::Unknown => todo!(),
        os_info::Type::Windows => todo!(),
        _ => todo!(),
    }
    .to_string()
}
