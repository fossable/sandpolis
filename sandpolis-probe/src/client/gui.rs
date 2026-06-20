//! GUI components for the Probe layer.
//!
//! One graph node per registered *device*; its node controller shows a tab per
//! protocol the device exposes (RTSP renders live video, Wake-on-LAN offers a
//! Wake button, etc.). Devices are added/deleted over the management stream, which
//! the server persists to `sandpolis.ron` and broadcasts back to all clients.

use bevy::asset::RenderAssetUsages;
use bevy::ecs::hierarchy::ChildSpawnerCommands;
use bevy::image::Image;
use bevy::input_focus::{FocusCause, InputFocus};
use bevy::prelude::*;
use bevy::render::render_resource::{Extent3d, TextureDimension, TextureFormat};
use bevy_rapier2d::dynamics::{Damping, ExternalForce, RigidBody, Velocity};
use bevy_rapier2d::geometry::{Collider, Restitution};
use bevy_svg::prelude::{Origin, Svg2d};
use sandpolis_client::gui::ui::Activate;
use sandpolis_client::gui::ui::bind::bind_text;
use sandpolis_client::gui::ui::controller::{
    LayerClientInfo, LayerRegistry, NodeController, RegisterLayerClient,
};
use sandpolis_client::gui::ui::panel::modal_scrim;
use sandpolis_client::gui::ui::text_input::{TextInput, text_input};
use sandpolis_client::gui::ui::theme::{Role, Theme, ThemedBg, ThemedBorder};
use sandpolis_client::gui::ui::widgets::{button, heading, muted, row};
use sandpolis_client::gui::node::{ExcludeFromSelection, NeedsScaling, NodeEntity};
use sandpolis_instance::network::stream::StreamMessage;
use sandpolis_instance::{InstanceId, InstanceLayer, InstanceType, LayerName};
use std::collections::HashMap;
use tokio::sync::mpsc::{Receiver, Sender, UnboundedReceiver, channel};

use crate::config::{DeviceConfig, RtspProbeConfig, WolProbeConfig};
use crate::rtsp::{
    RtspFrameRgba, RtspSessionStreamRequest, RtspSessionStreamRequester, RtspStreamEvent,
    RtspTransport,
};
use crate::{ProbeType, RegisteredDevice};

/// Marker component for device nodes (smaller nodes attached to gateways).
#[derive(Component)]
pub struct ProbeNode {
    /// The device ID.
    pub device_id: u64,
    /// The protocol used for the node's icon.
    pub icon: ProbeType,
    /// The gateway instance this device is attached to.
    pub gateway: InstanceId,
}

/// The visual diameter for device nodes (smaller than regular nodes).
pub const PROBE_NODE_VISUAL_DIAMETER: f32 = 50.0;

/// Bundle for spawning device nodes.
#[derive(Bundle)]
pub struct ProbeNodeBundle {
    pub probe_node: ProbeNode,
    pub node_entity: NodeEntity,
    pub exclude: ExcludeFromSelection,
    pub collider: Collider,
    pub rigid_body: RigidBody,
    pub velocity: Velocity,
    pub external_force: ExternalForce,
    pub damping: Damping,
    pub restitution: Restitution,
    pub transform: Transform,
    pub visibility: Visibility,
}

/// Spawn a device node in the world view.
pub fn spawn_probe_node(
    asset_server: &AssetServer,
    commands: &mut Commands,
    device: &RegisteredDevice,
    parent_position: Vec3,
    visible: bool,
) {
    let instance_id = device.gateway;
    let icon = device.device.primary().unwrap_or(ProbeType::Http);

    // Position device nodes in an orbit around the parent, using the device id for
    // consistent golden-angle placement.
    let angle =
        (device.id as f32 * 0.618033988749895 * std::f32::consts::TAU) % std::f32::consts::TAU;
    let orbit_radius = 120.0;
    let x = parent_position.x + orbit_radius * angle.cos();
    let y = parent_position.y + orbit_radius * angle.sin();

    let svg_path = get_probe_svg(icon);

    let node_entity = commands
        .spawn(ProbeNodeBundle {
            probe_node: ProbeNode {
                device_id: device.id,
                icon,
                gateway: device.gateway,
            },
            node_entity: NodeEntity { instance_id },
            exclude: ExcludeFromSelection,
            collider: Collider::ball(25.0),
            rigid_body: RigidBody::Dynamic,
            velocity: Velocity::zero(),
            external_force: ExternalForce::default(),
            damping: Damping {
                linear_damping: 0.0,
                angular_damping: 1.0,
            },
            restitution: Restitution::coefficient(0.7),
            transform: Transform::from_xyz(x, y, 0.0),
            visibility: if visible {
                Visibility::Inherited
            } else {
                Visibility::Hidden
            },
        })
        .id();

    // Deliberately not tagged with NodeSvg so the layer visual systems don't
    // replace the icon or rescale it to regular node size.
    commands.entity(node_entity).with_children(|parent| {
        parent.spawn((
            Svg2d(asset_server.load(svg_path)),
            Origin::Center,
            Transform::default(),
            NeedsScaling,
            ProbeNodeSvg,
        ));
    });
}

/// Marker component for device node SVGs (for scaling to smaller size).
#[derive(Component)]
pub struct ProbeNodeSvg;

/// System to scale device SVGs to a smaller uniform size once loaded.
pub fn scale_probe_node_svgs(
    mut commands: Commands,
    svg_assets: Res<Assets<bevy_svg::prelude::Svg>>,
    mut nodes_needing_scale: Query<
        (Entity, &Svg2d, &mut Transform),
        (With<NeedsScaling>, With<ProbeNodeSvg>),
    >,
) {
    for (entity, svg_handle, mut transform) in nodes_needing_scale.iter_mut() {
        if let Some(svg) = svg_assets.get(&svg_handle.0) {
            let svg_size = svg.size;
            let max_dimension = svg_size.x.max(svg_size.y);

            if max_dimension > 0.0 {
                let scale = PROBE_NODE_VISUAL_DIAMETER / max_dimension;
                transform.scale = Vec3::splat(scale);

                let scaled_size = svg_size * scale;
                transform.translation.x = -scaled_size.x / 2.0;
                transform.translation.y = scaled_size.y / 2.0;

                commands.entity(entity).remove::<NeedsScaling>();
            }
        }
    }
}

/// Spawn/despawn device nodes to match the registered device list.
pub fn update_probe_nodes(
    mut commands: Commands,
    asset_server: Res<AssetServer>,
    current_layer: Res<sandpolis_client::gui::input::CurrentLayer>,
    registry: Res<LayerRegistry>,
    existing_probes: Query<(Entity, &ProbeNode)>,
    parent_nodes: Query<(&Transform, &NodeEntity), Without<ProbeNode>>,
) {
    let show_probes = registry.show_probe_nodes(&current_layer);

    let gateway_positions: HashMap<InstanceId, Vec3> = parent_nodes
        .iter()
        .map(|(transform, node)| (node.instance_id, transform.translation))
        .collect();

    let mut all_devices = Vec::new();
    for (_, node) in parent_nodes.iter() {
        all_devices.extend(query_devices(node.instance_id));
    }

    let existing_ids: std::collections::HashSet<u64> = existing_probes
        .iter()
        .map(|(_, probe)| probe.device_id)
        .collect();

    for device in &all_devices {
        if !existing_ids.contains(&device.id) {
            if let Some(&parent_pos) = gateway_positions.get(&device.gateway) {
                spawn_probe_node(&asset_server, &mut commands, device, parent_pos, show_probes);
            }
        }
    }

    let db_ids: std::collections::HashSet<u64> = all_devices.iter().map(|d| d.id).collect();
    for (entity, probe) in existing_probes.iter() {
        if !db_ids.contains(&probe.device_id) {
            commands.entity(entity).despawn();
        }
    }
}

/// Keep device nodes orbiting near their parent gateways.
pub fn apply_probe_spring_forces(
    mut probe_query: Query<(&Transform, &mut ExternalForce, &ProbeNode)>,
    parent_query: Query<(&Transform, &NodeEntity), Without<ProbeNode>>,
) {
    let gateway_positions: HashMap<InstanceId, Vec3> = parent_query
        .iter()
        .map(|(transform, node)| (node.instance_id, transform.translation))
        .collect();

    let spring_strength = 0.05;
    let rest_length = 120.0;
    let max_force = 500.0;

    for (transform, mut force, probe) in probe_query.iter_mut() {
        if let Some(&gateway_pos) = gateway_positions.get(&probe.gateway) {
            let delta = gateway_pos - transform.translation;
            let distance = delta.length().max(1.0);
            let displacement = distance - rest_length;
            let force_magnitude = (spring_strength * displacement).clamp(-max_force, max_force);
            let force_direction = delta.normalize_or_zero();
            force.force += (force_direction * force_magnitude).truncate();
        }
    }
}

/// Update device node visibility based on the current layer.
pub fn update_probe_node_visibility(
    current_layer: Res<sandpolis_client::gui::input::CurrentLayer>,
    registry: Res<LayerRegistry>,
    mut probe_query: Query<&mut Visibility, With<ProbeNode>>,
) {
    if !current_layer.is_changed() {
        return;
    }
    let show_probes = registry.show_probe_nodes(&current_layer);
    for mut visibility in probe_query.iter_mut() {
        *visibility = if show_probes {
            Visibility::Inherited
        } else {
            Visibility::Hidden
        };
    }
}

/// Query registered devices for a gateway.
pub fn query_devices(gateway: InstanceId) -> Vec<RegisteredDevice> {
    crate::REGISTERED_DEVICES
        .read()
        .unwrap()
        .iter()
        .filter(|device| device.gateway == gateway)
        .cloned()
        .collect()
}

/// Look up a single device by id.
fn device_by_id(id: u64) -> Option<RegisteredDevice> {
    crate::REGISTERED_DEVICES
        .read()
        .unwrap()
        .iter()
        .find(|d| d.id == id)
        .cloned()
}

/// Send a Wake-on-LAN magic packet and describe the outcome.
fn send_wake(wol: &WolProbeConfig) -> String {
    let mac_address = match wol.mac_address.parse::<macaddr::MacAddr6>() {
        Ok(mac) => mac,
        Err(e) => return format!("Invalid MAC address: {}", e),
    };

    match crate::wol::send_wol_packet(&crate::wol::WolPacketRequest {
        mac_address,
        broadcast_address: wol.broadcast_address.clone(),
        port: wol.port,
    }) {
        crate::wol::WolPacketResponse::Ok => format!("Magic packet sent to {}", wol.mac_address),
        crate::wol::WolPacketResponse::InvalidBroadcastAddress(addr) => {
            format!("Invalid broadcast address: {}", addr)
        }
        crate::wol::WolPacketResponse::SendFailed(e) => format!("Send failed: {}", e),
    }
}

/// Get the SVG asset path for a probe type.
pub fn get_probe_svg(probe_type: ProbeType) -> &'static str {
    match probe_type {
        ProbeType::Rdp => "probe/rdp.svg",
        ProbeType::Ssh => "probe/ssh.svg",
        ProbeType::Ups => "probe/ups.svg",
        ProbeType::Vnc => "probe/vnc.svg",
        ProbeType::Wol => "probe/wol.svg",
        ProbeType::Http => "probe/http.svg",
        ProbeType::Ipmi => "probe/ipmi.svg",
        ProbeType::Rtsp => "probe/rtsp.svg",
        ProbeType::Snmp => "probe/snmp.svg",
        ProbeType::Onvif => "probe/onvif.svg",
        ProbeType::Docker => "probe/docker.svg",
        ProbeType::Libvirt => "probe/libvirt.svg",
    }
}

/// Build the RTSP URL from the device's IP and the probe's port/path/credentials.
fn build_rtsp_url(ip: std::net::IpAddr, cfg: &RtspProbeConfig) -> String {
    let port = cfg.port.unwrap_or(554);
    let creds = match (cfg.username.as_deref(), cfg.password.as_deref()) {
        (Some(u), Some(p)) if !u.is_empty() => format!("{}:{}@", u, p),
        (Some(u), _) if !u.is_empty() => format!("{}@", u),
        _ => String::new(),
    };
    let path = cfg.path.trim_start_matches('/');
    format!("rtsp://{}{}:{}/{}", creds, ip, port, path)
}

fn rtsp_transport(cfg: &RtspProbeConfig) -> RtspTransport {
    match cfg.transport.as_deref().map(|s| s.to_ascii_lowercase()) {
        Some(ref s) if s == "udp" => RtspTransport::Udp,
        _ => RtspTransport::Tcp,
    }
}

/// Active client-side RTSP sessions, keyed by device id.
#[derive(Resource, Default)]
struct ProbeStreams {
    streams: HashMap<u64, StreamSession>,
}

struct StreamSession {
    events: UnboundedReceiver<RtspStreamEvent>,
    outbound: Sender<RtspSessionStreamRequest>,
    view: Entity,
    size: Option<(u32, u32)>,
}

/// The display node showing a device's RTSP stream.
#[derive(Component)]
struct RtspStreamView {
    device_id: u64,
}

/// The set of currently selected device nodes (by id).
#[derive(Resource, Default)]
pub struct DeviceSelectionSet {
    pub selected: Vec<u64>,
}

/// Marker for a selected device node entity.
#[derive(Component)]
struct DeviceSelected;

/// A device controller's tab bar; `active` is the visible tab index.
#[derive(Component)]
struct DeviceTabBar {
    device_id: u64,
    active: usize,
}

/// One tab's content panel within a device controller.
#[derive(Component)]
struct DeviceTabContent {
    device_id: u64,
    index: usize,
}

/// The probe layer's node controller: lists the gateway's devices, each with a
/// tab per protocol.
pub struct ProbeController;

impl NodeController for ProbeController {
    fn title(&self) -> &str {
        "Devices"
    }

    fn build(&self, commands: &mut Commands, body: Entity, instance: InstanceId, theme: &Theme) {
        let devices = query_devices(instance);

        commands.entity(body).with_children(|p| {
            p.spawn((
                heading(theme, "Devices"),
                bind_text(move || format!("Devices ({})", query_devices(instance).len())),
            ));

            if devices.is_empty() {
                p.spawn(muted(
                    theme,
                    "No devices registered on this node.",
                    theme.metrics.font_md,
                ));
            }

            for device in &devices {
                build_device_section(p, theme, device);
            }
        });
    }
}

/// Build one device's section: header + protocol tab bar + tab contents.
fn build_device_section(
    parent: &mut ChildSpawnerCommands,
    theme: &Theme,
    device: &RegisteredDevice,
) {
    let device_id = device.id;
    let protocols = device.device.protocols();

    parent
        .spawn((
            Node {
                flex_direction: FlexDirection::Column,
                row_gap: Val::Px(theme.metrics.space_sm),
                margin: UiRect::bottom(Val::Px(theme.metrics.space_md)),
                padding: UiRect::all(Val::Px(theme.metrics.space_sm)),
                border: UiRect::all(Val::Px(1.0)),
                ..default()
            },
            BorderColor::all(theme.color(Role::Border)),
            ThemedBorder(Role::Border),
        ))
        .with_children(|section| {
            section.spawn(heading(theme, device.display_name()));

            // Tab bar.
            section
                .spawn((
                    DeviceTabBar {
                        device_id,
                        active: 0,
                    },
                    row(theme.metrics.space_sm),
                ))
                .with_children(|bar| {
                    for (index, proto) in protocols.iter().enumerate() {
                        let proto = *proto;
                        bar.spawn(button(theme, proto.display_name())).observe(
                            move |_: On<Activate>, mut bars: Query<&mut DeviceTabBar>| {
                                for mut b in &mut bars {
                                    if b.device_id == device_id {
                                        b.active = index;
                                    }
                                }
                            },
                        );
                    }
                });

            // Tab contents (only the active one is visible).
            for (index, proto) in protocols.iter().enumerate() {
                section
                    .spawn((
                        DeviceTabContent { device_id, index },
                        Node {
                            flex_direction: FlexDirection::Column,
                            row_gap: Val::Px(theme.metrics.space_sm),
                            ..default()
                        },
                        if index == 0 {
                            Visibility::Inherited
                        } else {
                            Visibility::Hidden
                        },
                    ))
                    .with_children(|content| {
                        build_tab_content(content, theme, device, *proto);
                    });
            }
        });
}

/// Build the content for one protocol tab.
fn build_tab_content(
    content: &mut ChildSpawnerCommands,
    theme: &Theme,
    device: &RegisteredDevice,
    proto: ProbeType,
) {
    let device_id = device.id;
    match proto {
        ProbeType::Rtsp => {
            // Live video display (transparent until frames arrive).
            content.spawn((
                RtspStreamView { device_id },
                Node {
                    width: Val::Percent(100.0),
                    height: Val::Px(240.0),
                    ..default()
                },
                BackgroundColor(Color::srgb_u8(30, 30, 30)),
                ImageNode {
                    color: Color::NONE,
                    ..default()
                },
            ));
            content.spawn(row(theme.metrics.space_sm)).with_children(|controls| {
                controls
                    .spawn(button(theme, "Start stream"))
                    .observe(
                        move |_: On<Activate>,
                              mut streams: ResMut<ProbeStreams>,
                              views: Query<(Entity, &RtspStreamView)>| {
                            start_rtsp_stream(device_id, &mut streams, &views);
                        },
                    );
                controls
                    .spawn(button(theme, "Stop stream"))
                    .observe(
                        move |_: On<Activate>,
                              mut streams: ResMut<ProbeStreams>,
                              mut nodes: Query<&mut ImageNode>| {
                            if let Some(session) = streams.streams.remove(&device_id) {
                                let _ = session.outbound.try_send(RtspSessionStreamRequest::Stop);
                                if let Ok(mut node) = nodes.get_mut(session.view) {
                                    node.color = Color::NONE;
                                }
                            }
                        },
                    );
            });
        }
        ProbeType::Wol => {
            if let Some(wol) = device.device.wol.clone() {
                content
                    .spawn(button(theme, "Wake"))
                    .observe(move |_: On<Activate>| {
                        info!("{}", send_wake(&wol));
                    });
            }
        }
        other => {
            content.spawn(muted(
                theme,
                format!("{} integration is not implemented yet.", other.display_name()),
                theme.metrics.font_sm,
            ));
        }
    }
}

/// Open an RTSP stream for `device_id` if one isn't already running.
fn start_rtsp_stream(
    device_id: u64,
    streams: &mut ProbeStreams,
    views: &Query<(Entity, &RtspStreamView)>,
) {
    if streams.streams.contains_key(&device_id) {
        return;
    }
    let Some(device) = device_by_id(device_id) else {
        return;
    };
    let Some(rtsp) = device.device.rtsp.clone() else {
        return;
    };
    let Some((view, _)) = views.iter().find(|(_, v)| v.device_id == device_id) else {
        return;
    };

    let (requester, events) = RtspSessionStreamRequester::channel();
    let (outbound, outbound_rx) = channel(16);
    let initial = RtspSessionStreamRequest::Start {
        url: build_rtsp_url(device.device.ip, &rtsp),
        transport: rtsp_transport(&rtsp),
    };
    spawn_stream(device.gateway, requester, initial, outbound_rx);

    streams.streams.insert(
        device_id,
        StreamSession {
            events,
            outbound,
            view,
            size: None,
        },
    );
    info!("RTSP stream requested for device {}", device_id);
}

/// Open a relayed RTSP stream to `gateway` and forward outbound requests (Stop)
/// over it until the channel closes.
fn spawn_stream(
    gateway: InstanceId,
    requester: RtspSessionStreamRequester,
    initial: RtspSessionStreamRequest,
    mut outbound_rx: Receiver<RtspSessionStreamRequest>,
) {
    let Some(conn) = sandpolis_client::sync::connection() else {
        warn!("No server connection; cannot start RTSP stream");
        return;
    };
    tokio::spawn(async move {
        let (id, msg_tx) = match conn.open_stream_to(gateway, requester, initial).await {
            Ok(v) => v,
            Err(e) => {
                warn!(error = %e, "Failed to open RTSP stream");
                return;
            }
        };
        while let Some(req) = outbound_rx.recv().await {
            let payload = match serde_cbor::to_vec(&req) {
                Ok(p) => p,
                Err(_) => continue,
            };
            if msg_tx
                .send(StreamMessage {
                    stream_id: id,
                    payload,
                    dst: Some(gateway),
                })
                .await
                .is_err()
            {
                break;
            }
        }
        conn.close_stream(id);
    });
}

/// Build an RGBA8 [`Image`] from a decoded frame.
fn image_from_rgba(width: u32, height: u32, rgba: Vec<u8>) -> Image {
    Image::new(
        Extent3d {
            width,
            height,
            depth_or_array_layers: 1,
        },
        TextureDimension::D2,
        rgba,
        TextureFormat::Rgba8UnormSrgb,
        RenderAssetUsages::RENDER_WORLD | RenderAssetUsages::MAIN_WORLD,
    )
}

/// Drain decoded RTSP frames and upload the latest to each display texture.
fn drive_probe_streams(
    mut streams: ResMut<ProbeStreams>,
    mut images: ResMut<Assets<Image>>,
    mut nodes: Query<&mut ImageNode>,
) {
    for session in streams.streams.values_mut() {
        let mut latest: Option<RtspFrameRgba> = None;
        while let Ok(event) = session.events.try_recv() {
            match event {
                RtspStreamEvent::Started { width, height } => {
                    session.size = Some((width, height));
                }
                RtspStreamEvent::Frame(frame) => latest = Some(frame),
                RtspStreamEvent::Stopped => {}
            }
        }

        if let Some(frame) = latest {
            if frame.width == 0 || frame.height == 0 {
                continue;
            }
            session.size = Some((frame.width, frame.height));
            let handle = images.add(image_from_rgba(frame.width, frame.height, frame.rgba));
            if let Ok(mut node) = nodes.get_mut(session.view) {
                node.image = handle;
                node.color = Color::WHITE;
            }
        }
    }
}

/// Switch which tab content is visible to match its tab bar's active index.
fn update_device_tabs(
    bars: Query<&DeviceTabBar>,
    mut contents: Query<(&DeviceTabContent, &mut Visibility)>,
) {
    for (content, mut vis) in &mut contents {
        if let Some(bar) = bars.iter().find(|b| b.device_id == content.device_id) {
            let want = if bar.active == content.index {
                Visibility::Inherited
            } else {
                Visibility::Hidden
            };
            if *vis != want {
                *vis = want;
            }
        }
    }
}

/// Click handling for device-node selection (single / Ctrl-multi), Probe layer
/// only. Mirrors the generic node selection but keys off device id.
fn handle_device_selection(
    ui_pointer: Res<sandpolis_client::gui::ui::gating::UiPointerState>,
    current_layer: Res<sandpolis_client::gui::input::CurrentLayer>,
    registry: Res<LayerRegistry>,
    mouse_button: Res<ButtonInput<MouseButton>>,
    keyboard: Res<ButtonInput<KeyCode>>,
    windows: Query<&Window, With<bevy::window::PrimaryWindow>>,
    camera_query: Query<(&Camera, &GlobalTransform), With<sandpolis_client::gui::node::WorldView>>,
    mut commands: Commands,
    probe_query: Query<(Entity, &Transform, &ProbeNode)>,
    mut selection: ResMut<DeviceSelectionSet>,
) {
    if !registry.show_probe_nodes(&current_layer) {
        return;
    }
    if ui_pointer.over_ui_blocking || !mouse_button.just_pressed(MouseButton::Left) {
        return;
    }
    let Ok(window) = windows.single() else {
        return;
    };
    let Some(cursor) = window.cursor_position() else {
        return;
    };
    let Ok((camera, camera_transform)) = camera_query.single() else {
        return;
    };
    let Ok(world_pos) = camera.viewport_to_world_2d(camera_transform, cursor) else {
        return;
    };

    const CLICK_RADIUS: f32 = 30.0;
    let mut clicked: Option<(Entity, u64)> = None;
    for (entity, transform, probe) in probe_query.iter() {
        if world_pos.distance(transform.translation.truncate()) <= CLICK_RADIUS {
            clicked = Some((entity, probe.device_id));
            break;
        }
    }

    let ctrl = keyboard.pressed(KeyCode::ControlLeft)
        || keyboard.pressed(KeyCode::ControlRight)
        || keyboard.pressed(KeyCode::SuperLeft)
        || keyboard.pressed(KeyCode::SuperRight);

    if let Some((entity, device_id)) = clicked {
        if ctrl {
            if selection.selected.contains(&device_id) {
                selection.selected.retain(|&id| id != device_id);
                commands.entity(entity).remove::<DeviceSelected>();
            } else {
                selection.selected.push(device_id);
                commands.entity(entity).insert(DeviceSelected);
            }
        } else {
            for (e, _, _) in probe_query.iter() {
                commands.entity(e).remove::<DeviceSelected>();
            }
            selection.selected.clear();
            selection.selected.push(device_id);
            commands.entity(entity).insert(DeviceSelected);
        }
    } else if !ctrl {
        for (e, _, _) in probe_query.iter() {
            commands.entity(e).remove::<DeviceSelected>();
        }
        selection.selected.clear();
    }
}

/// Selection-ring visuals for selected device nodes.
#[derive(Component)]
struct DeviceSelectionRing;

fn update_device_selection_visuals(
    mut commands: Commands,
    selected: Query<Entity, (With<ProbeNode>, With<DeviceSelected>)>,
    rings: Query<(Entity, &ChildOf), With<DeviceSelectionRing>>,
    mut meshes: ResMut<Assets<Mesh>>,
    mut materials: ResMut<Assets<ColorMaterial>>,
) {
    // Remove rings whose node is no longer selected.
    for (ring, parent) in rings.iter() {
        if !selected.contains(parent.parent()) {
            commands.entity(ring).despawn();
        }
    }
    // Add rings for newly selected nodes.
    for node in selected.iter() {
        let has_ring = rings.iter().any(|(_, p)| p.parent() == node);
        if !has_ring {
            let ring = Mesh::from(Circle::new(32.0));
            commands.entity(node).with_children(|parent| {
                parent.spawn((
                    Mesh2d(meshes.add(ring)),
                    MeshMaterial2d(
                        materials.add(ColorMaterial::from(Color::srgba(0.3, 0.8, 1.0, 0.6))),
                    ),
                    Transform::from_xyz(0.0, 0.0, -0.1),
                    DeviceSelectionRing,
                ));
            });
        }
    }
}

/// Delete the currently selected devices over the management stream.
fn delete_selected_devices(commands: &mut Commands) {
    commands.queue(|world: &mut World| {
        let ids: Vec<u64> = world
            .get_resource::<DeviceSelectionSet>()
            .map(|s| s.selected.clone())
            .unwrap_or_default();
        if ids.is_empty() {
            return;
        }
        if let Some(conn) = sandpolis_client::sync::connection() {
            for id in ids {
                crate::management::delete_device(conn.clone(), id);
            }
        }
        if let Some(mut sel) = world.get_resource_mut::<DeviceSelectionSet>() {
            sel.selected.clear();
        }
    });
}

/// State of the "register device" dialog.
#[derive(Resource, Default)]
pub struct RegisterProbeDialogState {
    pub show: bool,
    pub name: String,
    pub ip: String,
    pub rtsp_path: String,
    pub rtsp_port: String,
    pub rtsp_user: String,
    pub rtsp_pass: String,
    pub wol_mac: String,
}

#[derive(Component)]
pub struct RegisterProbeRoot;
#[derive(Component)]
struct NameInput;
#[derive(Component)]
struct IpInput;
#[derive(Component)]
struct RtspPathInput;
#[derive(Component)]
struct RtspPortInput;
#[derive(Component)]
struct RtspUserInput;
#[derive(Component)]
struct RtspPassInput;
#[derive(Component)]
struct WolMacInput;

/// Spawn/despawn the register-device modal.
pub fn manage_register_probe(
    mut commands: Commands,
    theme: Res<Theme>,
    state: Res<RegisterProbeDialogState>,
    root: Query<Entity, With<RegisterProbeRoot>>,
    mut focus: ResMut<InputFocus>,
) {
    let exists = !root.is_empty();
    if state.show && !exists {
        commands
            .spawn((RegisterProbeRoot, modal_scrim()))
            .with_children(|scrim| {
                scrim
                    .spawn((
                        Node {
                            flex_direction: FlexDirection::Column,
                            width: Val::Px(380.0),
                            padding: UiRect::all(Val::Px(16.0)),
                            row_gap: Val::Px(6.0),
                            border: UiRect::all(Val::Px(1.0)),
                            ..default()
                        },
                        BackgroundColor(theme.color(Role::Panel)),
                        ThemedBg(Role::Panel),
                        BorderColor::all(theme.color(Role::Border)),
                        ThemedBorder(Role::Border),
                    ))
                    .with_children(|p| {
                        p.spawn(heading(&theme, "Register Device"));
                        p.spawn(muted(&theme, "Name", theme.metrics.font_sm));
                        p.spawn((NameInput, text_input(&theme, "device name", false)));
                        p.spawn(muted(&theme, "IP address", theme.metrics.font_sm));
                        p.spawn((IpInput, text_input(&theme, "10.0.0.220", false)));

                        p.spawn(muted(&theme, "RTSP path", theme.metrics.font_sm));
                        p.spawn((RtspPathInput, text_input(&theme, "stream1 (optional)", false)));
                        p.spawn(muted(&theme, "RTSP port", theme.metrics.font_sm));
                        p.spawn((RtspPortInput, text_input(&theme, "554", false)));
                        p.spawn(muted(&theme, "RTSP username", theme.metrics.font_sm));
                        p.spawn((RtspUserInput, text_input(&theme, "username (optional)", false)));
                        p.spawn(muted(&theme, "RTSP password", theme.metrics.font_sm));
                        p.spawn((RtspPassInput, text_input(&theme, "password (optional)", true)));

                        p.spawn(muted(&theme, "Wake-on-LAN MAC", theme.metrics.font_sm));
                        p.spawn((WolMacInput, text_input(&theme, "00:11:22:33:44:55 (optional)", false)));

                        p.spawn(Node {
                            column_gap: Val::Px(8.0),
                            ..default()
                        })
                        .with_children(|row| {
                            row.spawn(button(&theme, "Register"))
                                .observe(on_register_submit);
                            row.spawn(button(&theme, "Cancel")).observe(on_register_cancel);
                        });
                    });
            });
    } else if !state.show && exists {
        for entity in &root {
            commands.entity(entity).despawn();
        }
        focus.clear();
    }
}

/// Focus the name field when the dialog opens.
pub fn focus_register_probe_input(
    inputs: Query<Entity, Added<NameInput>>,
    mut focus: ResMut<InputFocus>,
) {
    if let Ok(entity) = inputs.single() {
        focus.set(entity, FocusCause::Navigated);
    }
}

/// Copy dialog input contents into [`RegisterProbeDialogState`].
pub fn sync_register_probe_inputs(
    mut state: ResMut<RegisterProbeDialogState>,
    name: Query<&TextInput, With<NameInput>>,
    ip: Query<&TextInput, With<IpInput>>,
    path: Query<&TextInput, With<RtspPathInput>>,
    port: Query<&TextInput, With<RtspPortInput>>,
    user: Query<&TextInput, With<RtspUserInput>>,
    pass: Query<&TextInput, With<RtspPassInput>>,
    mac: Query<&TextInput, With<WolMacInput>>,
) {
    if let Ok(i) = name.single() {
        if state.name != i.value {
            state.name = i.value.clone();
        }
    }
    if let Ok(i) = ip.single() {
        if state.ip != i.value {
            state.ip = i.value.clone();
        }
    }
    if let Ok(i) = path.single() {
        if state.rtsp_path != i.value {
            state.rtsp_path = i.value.clone();
        }
    }
    if let Ok(i) = port.single() {
        if state.rtsp_port != i.value {
            state.rtsp_port = i.value.clone();
        }
    }
    if let Ok(i) = user.single() {
        if state.rtsp_user != i.value {
            state.rtsp_user = i.value.clone();
        }
    }
    if let Ok(i) = pass.single() {
        if state.rtsp_pass != i.value {
            state.rtsp_pass = i.value.clone();
        }
    }
    if let Ok(i) = mac.single() {
        if state.wol_mac != i.value {
            state.wol_mac = i.value.clone();
        }
    }
}

fn on_register_submit(
    _activate: On<Activate>,
    mut state: ResMut<RegisterProbeDialogState>,
    instance_layer: Res<InstanceLayer>,
) {
    let ip = match state.ip.trim().parse::<std::net::IpAddr>() {
        Ok(ip) => ip,
        Err(_) => {
            warn!("Register device: invalid IP address {:?}", state.ip);
            return;
        }
    };

    let mut device = DeviceConfig {
        name: (!state.name.is_empty()).then(|| state.name.clone()),
        ip,
        ..Default::default()
    };
    if !state.rtsp_path.is_empty() {
        device.rtsp = Some(RtspProbeConfig {
            port: state.rtsp_port.trim().parse::<u16>().ok(),
            path: state.rtsp_path.clone(),
            username: (!state.rtsp_user.is_empty()).then(|| state.rtsp_user.clone()),
            password: (!state.rtsp_pass.is_empty()).then(|| state.rtsp_pass.clone()),
            transport: None,
        });
    }
    if !state.wol_mac.is_empty() {
        device.wol = Some(WolProbeConfig {
            mac_address: state.wol_mac.clone(),
            ..Default::default()
        });
    }

    if device.protocols().is_empty() {
        warn!("Register device: no protocols specified");
        return;
    }

    if let Some(conn) = sandpolis_client::sync::connection() {
        crate::management::register_device(conn, instance_layer.instance_id, device);
    } else {
        warn!("No server connection; cannot register device");
    }

    *state = RegisterProbeDialogState::default();
}

fn on_register_cancel(_activate: On<Activate>, mut state: ResMut<RegisterProbeDialogState>) {
    *state = RegisterProbeDialogState::default();
}

/// Open the device-management subscription once a connection is available.
fn open_device_subscription(mut done: Local<bool>) {
    if *done {
        return;
    }
    if let Some(conn) = sandpolis_client::sync::connection() {
        crate::management::subscribe(conn);
        *done = true;
    }
}

/// The probe layer's client plugin.
pub struct ProbeClientPlugin;

impl Plugin for ProbeClientPlugin {
    fn build(&self, app: &mut App) {
        app.init_resource::<RegisterProbeDialogState>();
        app.init_resource::<ProbeStreams>();
        app.init_resource::<DeviceSelectionSet>();
        app.add_systems(
            Update,
            (
                scale_probe_node_svgs,
                update_probe_nodes,
                apply_probe_spring_forces,
                manage_register_probe,
                focus_register_probe_input,
                sync_register_probe_inputs,
                drive_probe_streams,
                update_device_tabs,
                handle_device_selection,
                update_device_selection_visuals,
                open_device_subscription,
            ),
        );
        // Must run after the generic node visibility system (which also matches
        // device nodes) so probe-specific visibility wins.
        app.add_systems(
            PostUpdate,
            update_probe_node_visibility
                .after(sandpolis_client::gui::layer_visuals::update_node_visibility_for_layer),
        );
        app.register_layer_client(
            LayerClientInfo::new(LayerName::from("Probe"), "Device monitoring probes")
                .with_controller(ProbeController)
                .with_visible_instance_types(&[InstanceType::Server, InstanceType::Agent])
                .showing_probe_nodes()
                .with_toolbar_action("Register probe", "toolbar/register_probe.svg", |commands| {
                    commands.queue(|world: &mut World| {
                        if let Some(mut state) =
                            world.get_resource_mut::<RegisterProbeDialogState>()
                        {
                            state.show = true;
                        }
                    });
                })
                .with_toolbar_action_gated(
                    "Delete probe",
                    "toolbar/delete_probe.svg",
                    delete_selected_devices,
                    |world: &World| {
                        world
                            .get_resource::<DeviceSelectionSet>()
                            .is_some_and(|s| !s.selected.is_empty())
                    },
                ),
        );
    }
}
