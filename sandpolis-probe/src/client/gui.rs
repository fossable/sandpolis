//! GUI components for the Probe layer.
//!
//! Provides probe-node spawning/physics, the probe node controller, and the
//! layer's client plugin.
//!
//! Note: the exhaustive per-protocol registration form (host/port/credentials and
//! protocol-specific fields, ~25 inputs across 12 probe types) is deferred — its
//! submission path was unimplemented. The native controller surfaces the
//! registered-probe list and the working Wake-on-LAN action.

use bevy::input_focus::InputFocus;
use bevy::prelude::*;
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
use sandpolis_client::gui::ui::widgets::{button, heading, muted, row, text};
use sandpolis_client::gui::node::{NeedsScaling, NodeEntity};
use sandpolis_instance::{InstanceId, InstanceLayer, InstanceType, LayerName};

use crate::config::WolProbeConfig;
use crate::{ProbeConfig, ProbeType, RegisteredProbe};

/// Marker component for probe nodes (smaller nodes attached to agents).
#[derive(Component)]
pub struct ProbeNode {
    /// The probe ID.
    pub probe_id: u64,
    /// The probe type.
    pub probe_type: ProbeType,
    /// The parent gateway instance this probe is attached to.
    pub gateway: InstanceId,
}

/// The visual diameter for probe nodes (smaller than regular nodes).
pub const PROBE_NODE_VISUAL_DIAMETER: f32 = 50.0;

/// Bundle for spawning probe nodes.
#[derive(Bundle)]
pub struct ProbeNodeBundle {
    pub probe_node: ProbeNode,
    pub node_entity: NodeEntity,
    pub collider: Collider,
    pub rigid_body: RigidBody,
    pub velocity: Velocity,
    pub external_force: ExternalForce,
    pub damping: Damping,
    pub restitution: Restitution,
    pub transform: Transform,
    pub visibility: Visibility,
}

/// Spawn a probe node in the world view.
pub fn spawn_probe_node(
    asset_server: &AssetServer,
    commands: &mut Commands,
    probe: &RegisteredProbe,
    parent_position: Vec3,
    visible: bool,
) {
    // Use the gateway's instance ID for the node entity
    let instance_id = probe.gateway;

    // Position probe nodes in an orbit around the parent
    // Use the probe ID to determine angle for consistent placement
    let angle =
        (probe.id as f32 * 0.618033988749895 * std::f32::consts::TAU) % std::f32::consts::TAU;
    let orbit_radius = 120.0; // Distance from parent node
    let x = parent_position.x + orbit_radius * angle.cos();
    let y = parent_position.y + orbit_radius * angle.sin();

    // Get the SVG path for this probe type
    let svg_path = get_probe_svg(probe.probe_type);

    // Spawn parent node with physics components (smaller collider for probes)
    let node_entity = commands
        .spawn(ProbeNodeBundle {
            probe_node: ProbeNode {
                probe_id: probe.id,
                probe_type: probe.probe_type,
                gateway: probe.gateway,
            },
            node_entity: NodeEntity { instance_id },
            collider: Collider::ball(25.0), // Smaller collider
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

    // Spawn SVG as a child entity. Note: deliberately not tagged with NodeSvg
    // so the layer visual systems don't replace the probe icon or rescale it
    // to the regular node size.
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

/// Marker component for probe node SVGs (for scaling to smaller size).
#[derive(Component)]
pub struct ProbeNodeSvg;

/// System to scale probe SVGs to a smaller uniform size once they're loaded.
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
                // Scale to fit within PROBE_NODE_VISUAL_DIAMETER
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

/// System to update probe nodes based on registered probes.
/// This spawns/despawns probe nodes to match the database state.
pub fn update_probe_nodes(
    mut commands: Commands,
    asset_server: Res<AssetServer>,
    current_layer: Res<sandpolis_client::gui::input::CurrentLayer>,
    registry: Res<LayerRegistry>,
    existing_probes: Query<(Entity, &ProbeNode)>,
    parent_nodes: Query<(&Transform, &NodeEntity), Without<ProbeNode>>,
) {
    // Probes spawned while another layer is active must start hidden
    let show_probes = registry.show_probe_nodes(&current_layer);

    // Build a map of gateway positions
    let gateway_positions: std::collections::HashMap<InstanceId, Vec3> = parent_nodes
        .iter()
        .map(|(transform, node)| (node.instance_id, transform.translation))
        .collect();

    // Get all registered probes from database
    // For now, we query all gateways
    let mut all_probes = Vec::new();
    for (_, node) in parent_nodes.iter() {
        all_probes.extend(query_probes(node.instance_id));
    }

    // Build set of existing probe IDs
    let existing_probe_ids: std::collections::HashSet<u64> = existing_probes
        .iter()
        .map(|(_, probe)| probe.probe_id)
        .collect();

    // Spawn new probes that don't exist yet
    for probe in &all_probes {
        if !existing_probe_ids.contains(&probe.id) {
            if let Some(&parent_pos) = gateway_positions.get(&probe.gateway) {
                spawn_probe_node(&asset_server, &mut commands, probe, parent_pos, show_probes);
            }
        }
    }

    // Despawn probes that no longer exist in database
    let db_probe_ids: std::collections::HashSet<u64> = all_probes.iter().map(|p| p.id).collect();
    for (entity, probe) in existing_probes.iter() {
        if !db_probe_ids.contains(&probe.probe_id) {
            commands.entity(entity).despawn();
        }
    }
}

/// System to apply spring forces between probe nodes and their parent gateways.
/// This keeps probes orbiting near their parent nodes.
pub fn apply_probe_spring_forces(
    mut probe_query: Query<(&Transform, &mut ExternalForce, &ProbeNode)>,
    parent_query: Query<(&Transform, &NodeEntity), Without<ProbeNode>>,
) {
    // Build gateway position lookup
    let gateway_positions: std::collections::HashMap<InstanceId, Vec3> = parent_query
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

            // Hooke's law: attract probe toward rest_length distance from gateway
            let displacement = distance - rest_length;
            let force_magnitude = (spring_strength * displacement).clamp(-max_force, max_force);

            let force_direction = delta.normalize_or_zero();
            let spring_force = force_direction * force_magnitude;

            force.force += spring_force.truncate();
        }
    }
}

/// System to update probe node visibility based on the current layer.
///
/// Probe nodes are only visible when the current layer shows probe nodes (the
/// Probe layer).
pub fn update_probe_node_visibility(
    current_layer: Res<sandpolis_client::gui::input::CurrentLayer>,
    registry: Res<LayerRegistry>,
    mut probe_query: Query<&mut Visibility, With<ProbeNode>>,
) {
    // Only update when layer changes
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

/// Query registered probes for a gateway.
pub fn query_probes(gateway: InstanceId) -> Vec<RegisteredProbe> {
    crate::REGISTERED_PROBES
        .read()
        .unwrap()
        .iter()
        .filter(|probe| probe.gateway == gateway)
        .cloned()
        .collect()
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
        crate::wol::WolPacketResponse::Ok => {
            format!("Magic packet sent to {}", wol.mac_address)
        }
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

/// The probe layer's node controller (registered-probe manager).
pub struct ProbeController;

impl NodeController for ProbeController {
    fn title(&self) -> &str {
        "Probe Manager"
    }

    fn build(&self, commands: &mut Commands, body: Entity, instance: InstanceId, theme: &Theme) {
        let probes = query_probes(instance);

        commands.entity(body).with_children(|p| {
            p.spawn((
                heading(theme, "Registered Probes"),
                bind_text(move || {
                    let n = query_probes(instance).len();
                    format!("Registered Probes ({})", n)
                }),
            ));

            if probes.is_empty() {
                p.spawn(muted(theme, "No probes registered on this node.", theme.metrics.font_md));
            }

            for probe in &probes {
                let status = if probe.online { "●" } else { "○" };
                let label = format!(
                    "{} {} ({})",
                    status,
                    probe.name,
                    probe.probe_type.display_name()
                );
                p.spawn(row(theme.metrics.space_sm)).with_children(|row_node| {
                    row_node.spawn(text(theme, label, theme.metrics.font_md, Role::Text));

                    // Wake-on-LAN is the one working action.
                    if let ProbeConfig::Wol(wol) = &probe.config {
                        let wol = wol.clone();
                        row_node
                            .spawn(button(theme, "Wake"))
                            .observe(move |_: On<Activate>| {
                                info!("{}", send_wake(&wol));
                            });
                    }

                    let probe_name = probe.name.clone();
                    row_node
                        .spawn(button(theme, "Test"))
                        .observe(move |_: On<Activate>| {
                            info!("Probe: test connection to {}", probe_name);
                        });
                });
            }

            p.spawn(muted(
                theme,
                "Registration form is deferred; register probes via the CLI.",
                theme.metrics.font_sm,
            ));
        });
    }
}

/// State of the "register probe" dialog.
///
/// Minimal scope: registers a Wake-on-LAN probe (the one type with a working
/// action) into the in-process [`crate::REGISTERED_PROBES`] store. Additional
/// probe types/fields are deferred.
#[derive(Resource, Default)]
pub struct RegisterProbeDialogState {
    pub show: bool,
    pub name: String,
    pub mac_address: String,
}

/// Modal root marker.
#[derive(Component)]
pub struct RegisterProbeRoot;

/// Probe-name input marker.
#[derive(Component)]
pub struct RegisterProbeNameInput;

/// MAC-address input marker.
#[derive(Component)]
pub struct RegisterProbeMacInput;

/// Spawn/despawn the register-probe modal.
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
                            width: Val::Px(360.0),
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
                        p.spawn(heading(&theme, "Register Probe"));
                        p.spawn(muted(&theme, "Name", theme.metrics.font_sm));
                        p.spawn((RegisterProbeNameInput, text_input(&theme, "probe name", false)));
                        p.spawn(muted(&theme, "MAC address (Wake-on-LAN)", theme.metrics.font_sm));
                        p.spawn((RegisterProbeMacInput, text_input(&theme, "00:11:22:33:44:55", false)));
                        p.spawn(muted(
                            &theme,
                            "Only Wake-on-LAN probes are supported for now.",
                            theme.metrics.font_sm,
                        ));
                        p.spawn(Node {
                            column_gap: Val::Px(8.0),
                            ..default()
                        })
                        .with_children(|row| {
                            row.spawn(button(&theme, "Register"))
                                .observe(on_register_probe_submit);
                            row.spawn(button(&theme, "Cancel"))
                                .observe(on_register_probe_cancel);
                        });
                    });
            });
    } else if !state.show && exists {
        for entity in &root {
            commands.entity(entity).despawn();
        }
        focus.0 = None;
    }
}

/// Focus the name field when the dialog opens.
pub fn focus_register_probe_input(
    inputs: Query<Entity, Added<RegisterProbeNameInput>>,
    mut focus: ResMut<InputFocus>,
) {
    if let Ok(entity) = inputs.single() {
        focus.0 = Some(entity);
    }
}

/// Copy the input contents into [`RegisterProbeDialogState`].
pub fn sync_register_probe_inputs(
    mut state: ResMut<RegisterProbeDialogState>,
    name: Query<&TextInput, With<RegisterProbeNameInput>>,
    mac: Query<&TextInput, With<RegisterProbeMacInput>>,
) {
    if let Ok(input) = name.single() {
        if state.name != input.value {
            state.name = input.value.clone();
        }
    }
    if let Ok(input) = mac.single() {
        if state.mac_address != input.value {
            state.mac_address = input.value.clone();
        }
    }
}

fn on_register_probe_submit(
    _activate: On<Activate>,
    mut state: ResMut<RegisterProbeDialogState>,
    instance_layer: Res<InstanceLayer>,
) {
    let mut probes = crate::REGISTERED_PROBES.write().unwrap();
    let next_id = probes.iter().map(|p| p.id).max().unwrap_or(0) + 1;
    probes.push(RegisteredProbe {
        id: next_id,
        probe_type: ProbeType::Wol,
        name: state.name.clone(),
        gateway: instance_layer.instance_id,
        config: ProbeConfig::Wol(WolProbeConfig {
            mac_address: state.mac_address.clone(),
            broadcast_address: None,
            port: None,
            hostname: None,
        }),
        online: false,
        status_message: None,
    });
    drop(probes);

    state.show = false;
    state.name.clear();
    state.mac_address.clear();
}

fn on_register_probe_cancel(_activate: On<Activate>, mut state: ResMut<RegisterProbeDialogState>) {
    state.show = false;
    state.name.clear();
    state.mac_address.clear();
}

/// The probe layer's client plugin.
pub struct ProbeClientPlugin;

impl Plugin for ProbeClientPlugin {
    fn build(&self, app: &mut App) {
        app.init_resource::<RegisterProbeDialogState>();
        app.add_systems(
            Update,
            (
                scale_probe_node_svgs,
                update_probe_nodes,
                apply_probe_spring_forces,
                manage_register_probe,
                focus_register_probe_input,
                sync_register_probe_inputs,
            ),
        );
        // Must run after the generic node visibility system (which also matches
        // probe nodes) so probe-specific visibility wins.
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
                .with_toolbar_action(
                    "Register probe",
                    "toolbar/register_probe.svg",
                    |commands| {
                        commands.queue(|world: &mut World| {
                            if let Some(mut state) =
                                world.get_resource_mut::<RegisterProbeDialogState>()
                            {
                                state.show = true;
                            }
                        });
                    },
                ),
        );
    }
}
