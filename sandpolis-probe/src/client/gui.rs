//! GUI components for the Probe layer.
//!
//! This module provides the probe controller and layer-specific GUI elements.

use bevy::prelude::*;
use bevy_egui::egui;
use bevy_rapier2d::dynamics::{Damping, ExternalForce, RigidBody, Velocity};
use bevy_rapier2d::geometry::{Collider, Restitution};
use bevy_svg::prelude::{Origin, Svg2d};
use sandpolis_client::gui::layer_ext::{ActivityTypeInfo, LayerGuiExtension};
use sandpolis_client::gui::node::{NeedsScaling, NodeEntity, NodeSvg};
use sandpolis_core::{InstanceId, LayerName};

use crate::{
    DockerProbeConfig, HttpProbeConfig, IpmiProbeConfig, LibvirtProbeConfig, OnvifProbeConfig,
    ProbeConfig, ProbeType, RdpProbeConfig, RegisteredProbe, RtspProbeConfig, SnmpProbeConfig,
    SnmpVersion, SshProbeConfig, UpsProbeConfig, VncProbeConfig, WolProbeConfig,
};

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
}

/// Spawn a probe node in the world view.
pub fn spawn_probe_node(
    asset_server: &AssetServer,
    commands: &mut Commands,
    probe: &RegisteredProbe,
    parent_position: Vec3,
) {
    // Use the gateway's instance ID for the node entity
    let instance_id = probe.gateway;

    // Position probe nodes in an orbit around the parent
    // Use the probe ID to determine angle for consistent placement
    let angle = (probe.id as f32 * 0.618033988749895 * std::f32::consts::TAU) % std::f32::consts::TAU;
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
        })
        .id();

    // Spawn SVG as a child entity
    commands.entity(node_entity).with_children(|parent| {
        parent.spawn((
            Svg2d(asset_server.load(svg_path)),
            Origin::Center,
            Transform::default(),
            NodeSvg,
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
    existing_probes: Query<(Entity, &ProbeNode)>,
    parent_nodes: Query<(&Transform, &NodeEntity)>,
) {
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
                spawn_probe_node(&asset_server, &mut commands, probe, parent_pos);
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
/// Probe nodes are only visible when the current layer's `show_probe_nodes()` returns true.
/// This is typically only the Probe layer.
pub fn update_probe_node_visibility(
    current_layer: Res<sandpolis_client::gui::input::CurrentLayer>,
    mut probe_query: Query<&mut Visibility, With<ProbeNode>>,
) {
    // Only update when layer changes
    if !current_layer.is_changed() {
        return;
    }

    // Check if the current layer should show probe nodes
    let show_probes = sandpolis_client::gui::layer_ext::get_extension_for_layer(&current_layer)
        .map(|ext| ext.show_probe_nodes())
        .unwrap_or(false);

    for mut visibility in probe_query.iter_mut() {
        *visibility = if show_probes {
            Visibility::Inherited
        } else {
            Visibility::Hidden
        };
    }
}

/// State for the probe controller persisted in egui memory.
#[derive(Clone, Default, serde::Serialize, serde::Deserialize)]
pub struct ProbeControllerState {
    /// Current view mode.
    pub view: ProbeView,
    /// Selected probe type for registration.
    pub selected_probe_type: Option<ProbeType>,
    /// Form state for new probe registration.
    pub form: ProbeFormState,
    /// Selected probe in the list view.
    pub selected_probe_id: Option<u64>,
}

/// View modes for the probe controller.
#[derive(Clone, Copy, Default, PartialEq, Eq, serde::Serialize, serde::Deserialize)]
pub enum ProbeView {
    /// List of registered probes.
    #[default]
    List,
    /// Register a new probe.
    Register,
}

/// Form state for probe registration.
#[derive(Clone, Default, serde::Serialize, serde::Deserialize)]
pub struct ProbeFormState {
    pub name: String,
    // Common fields
    pub host: String,
    pub port: String,
    pub username: String,
    pub password: String,
    // RDP specific
    pub domain: String,
    // SSH specific
    pub private_key_path: String,
    pub fingerprint: String,
    // UPS specific
    pub ups_name: String,
    // WOL specific
    pub mac_address: String,
    pub broadcast_address: String,
    pub wol_hostname: String,
    // HTTP specific
    pub url: String,
    pub expected_status: String,
    pub timeout_secs: String,
    pub verify_tls: bool,
    pub http_method: String,
    // IPMI specific
    pub interface_type: String,
    // RTSP specific
    pub transport: String,
    // SNMP specific
    pub snmp_version: SnmpVersion,
    pub community: String,
    pub auth_protocol: String,
    pub auth_password: String,
    pub priv_protocol: String,
    pub priv_password: String,
    // ONVIF specific
    pub profile_token: String,
    // Docker specific
    pub tls_ca_cert: String,
    pub tls_cert: String,
    pub tls_key: String,
    pub tls_verify: bool,
    // libvirt specific
    pub uri: String,
}

impl ProbeFormState {
    /// Reset form fields to defaults.
    pub fn reset(&mut self) {
        *self = Self::default();
    }

    /// Build a ProbeConfig from form state for the given probe type.
    pub fn build_config(&self, probe_type: ProbeType) -> ProbeConfig {
        match probe_type {
            ProbeType::Rdp => ProbeConfig::Rdp(RdpProbeConfig {
                host: self.host.clone(),
                port: self.port.parse().ok(),
                username: if self.username.is_empty() {
                    None
                } else {
                    Some(self.username.clone())
                },
                password: if self.password.is_empty() {
                    None
                } else {
                    Some(self.password.clone())
                },
                domain: if self.domain.is_empty() {
                    None
                } else {
                    Some(self.domain.clone())
                },
            }),
            ProbeType::Ssh => ProbeConfig::Ssh(SshProbeConfig {
                host: self.host.clone(),
                port: self.port.parse().ok(),
                username: if self.username.is_empty() {
                    None
                } else {
                    Some(self.username.clone())
                },
                password: if self.password.is_empty() {
                    None
                } else {
                    Some(self.password.clone())
                },
                private_key_path: if self.private_key_path.is_empty() {
                    None
                } else {
                    Some(self.private_key_path.clone())
                },
                fingerprint: if self.fingerprint.is_empty() {
                    None
                } else {
                    Some(self.fingerprint.clone())
                },
            }),
            ProbeType::Ups => ProbeConfig::Ups(UpsProbeConfig {
                host: self.host.clone(),
                port: self.port.parse().ok(),
                ups_name: self.ups_name.clone(),
                username: if self.username.is_empty() {
                    None
                } else {
                    Some(self.username.clone())
                },
                password: if self.password.is_empty() {
                    None
                } else {
                    Some(self.password.clone())
                },
            }),
            ProbeType::Vnc => ProbeConfig::Vnc(VncProbeConfig {
                host: self.host.clone(),
                port: self.port.parse().ok(),
                password: if self.password.is_empty() {
                    None
                } else {
                    Some(self.password.clone())
                },
            }),
            ProbeType::Wol => ProbeConfig::Wol(WolProbeConfig {
                mac_address: self.mac_address.clone(),
                broadcast_address: if self.broadcast_address.is_empty() {
                    None
                } else {
                    Some(self.broadcast_address.clone())
                },
                port: self.port.parse().ok(),
                hostname: if self.wol_hostname.is_empty() {
                    None
                } else {
                    Some(self.wol_hostname.clone())
                },
            }),
            ProbeType::Http => ProbeConfig::Http(HttpProbeConfig {
                url: self.url.clone(),
                expected_status: self.expected_status.parse().ok(),
                timeout_secs: self.timeout_secs.parse().ok(),
                verify_tls: Some(self.verify_tls),
                method: if self.http_method.is_empty() {
                    None
                } else {
                    Some(self.http_method.clone())
                },
                username: if self.username.is_empty() {
                    None
                } else {
                    Some(self.username.clone())
                },
                password: if self.password.is_empty() {
                    None
                } else {
                    Some(self.password.clone())
                },
            }),
            ProbeType::Ipmi => ProbeConfig::Ipmi(IpmiProbeConfig {
                host: self.host.clone(),
                port: self.port.parse().ok(),
                username: self.username.clone(),
                password: self.password.clone(),
                interface_type: if self.interface_type.is_empty() {
                    None
                } else {
                    Some(self.interface_type.clone())
                },
            }),
            ProbeType::Rtsp => ProbeConfig::Rtsp(RtspProbeConfig {
                url: self.url.clone(),
                username: if self.username.is_empty() {
                    None
                } else {
                    Some(self.username.clone())
                },
                password: if self.password.is_empty() {
                    None
                } else {
                    Some(self.password.clone())
                },
                transport: if self.transport.is_empty() {
                    None
                } else {
                    Some(self.transport.clone())
                },
            }),
            ProbeType::Snmp => ProbeConfig::Snmp(SnmpProbeConfig {
                host: self.host.clone(),
                port: self.port.parse().ok(),
                version: self.snmp_version,
                community: if self.community.is_empty() {
                    None
                } else {
                    Some(self.community.clone())
                },
                username: if self.username.is_empty() {
                    None
                } else {
                    Some(self.username.clone())
                },
                auth_protocol: if self.auth_protocol.is_empty() {
                    None
                } else {
                    Some(self.auth_protocol.clone())
                },
                auth_password: if self.auth_password.is_empty() {
                    None
                } else {
                    Some(self.auth_password.clone())
                },
                priv_protocol: if self.priv_protocol.is_empty() {
                    None
                } else {
                    Some(self.priv_protocol.clone())
                },
                priv_password: if self.priv_password.is_empty() {
                    None
                } else {
                    Some(self.priv_password.clone())
                },
            }),
            ProbeType::Onvif => ProbeConfig::Onvif(OnvifProbeConfig {
                host: self.host.clone(),
                port: self.port.parse().ok(),
                username: if self.username.is_empty() {
                    None
                } else {
                    Some(self.username.clone())
                },
                password: if self.password.is_empty() {
                    None
                } else {
                    Some(self.password.clone())
                },
                profile_token: if self.profile_token.is_empty() {
                    None
                } else {
                    Some(self.profile_token.clone())
                },
            }),
            ProbeType::Docker => ProbeConfig::Docker(DockerProbeConfig {
                host: self.host.clone(),
                tls_ca_cert: if self.tls_ca_cert.is_empty() {
                    None
                } else {
                    Some(self.tls_ca_cert.clone())
                },
                tls_cert: if self.tls_cert.is_empty() {
                    None
                } else {
                    Some(self.tls_cert.clone())
                },
                tls_key: if self.tls_key.is_empty() {
                    None
                } else {
                    Some(self.tls_key.clone())
                },
                tls_verify: Some(self.tls_verify),
            }),
            ProbeType::Libvirt => ProbeConfig::Libvirt(LibvirtProbeConfig {
                uri: self.uri.clone(),
                username: if self.username.is_empty() {
                    None
                } else {
                    Some(self.username.clone())
                },
                private_key_path: if self.private_key_path.is_empty() {
                    None
                } else {
                    Some(self.private_key_path.clone())
                },
            }),
        }
    }
}

/// Query registered probes for a gateway.
pub fn query_probes(_gateway: InstanceId) -> Vec<RegisteredProbe> {
    // TODO: Query from database
    vec![]
}

/// Render the probe controller.
pub fn render_probe_controller(ui: &mut egui::Ui, instance_id: InstanceId) {
    let state_id = egui::Id::new(format!("probe_controller_{}", instance_id));
    let mut state = ui.data_mut(|d| {
        d.get_persisted::<ProbeControllerState>(state_id)
            .unwrap_or_default()
    });

    // Header with view toggle
    ui.horizontal(|ui| {
        if ui
            .selectable_label(state.view == ProbeView::List, "Registered Probes")
            .clicked()
        {
            state.view = ProbeView::List;
        }
        if ui
            .selectable_label(state.view == ProbeView::Register, "Register New")
            .clicked()
        {
            state.view = ProbeView::Register;
            state.selected_probe_type = None;
            state.form.reset();
        }
    });

    ui.separator();

    match state.view {
        ProbeView::List => render_probe_list(ui, instance_id, &mut state),
        ProbeView::Register => render_probe_registration(ui, instance_id, &mut state),
    }

    ui.data_mut(|d| d.insert_persisted(state_id, state));
}

/// Render the list of registered probes.
fn render_probe_list(ui: &mut egui::Ui, instance_id: InstanceId, state: &mut ProbeControllerState) {
    let probes = query_probes(instance_id);

    if probes.is_empty() {
        ui.vertical_centered(|ui| {
            ui.add_space(20.0);
            ui.label("No probes registered on this node.");
            ui.add_space(10.0);
            if ui.button("Register a Probe").clicked() {
                state.view = ProbeView::Register;
            }
        });
    } else {
        egui::ScrollArea::vertical()
            .max_height(300.0)
            .show(ui, |ui| {
                for probe in &probes {
                    let is_selected = state.selected_probe_id == Some(probe.id);
                    let status_icon = if probe.online { "●" } else { "○" };
                    let status_color = if probe.online {
                        egui::Color32::from_rgb(100, 200, 100)
                    } else {
                        egui::Color32::from_rgb(150, 150, 150)
                    };

                    ui.horizontal(|ui| {
                        ui.colored_label(status_color, status_icon);
                        if ui
                            .selectable_label(
                                is_selected,
                                format!("{} ({})", probe.name, probe.probe_type.display_name()),
                            )
                            .clicked()
                        {
                            state.selected_probe_id = Some(probe.id);
                        }
                    });
                }
            });

        if let Some(probe_id) = state.selected_probe_id {
            if let Some(probe) = probes.iter().find(|p| p.id == probe_id) {
                ui.separator();
                render_probe_details(ui, probe);
            }
        }
    }
}

/// Render details for a selected probe.
fn render_probe_details(ui: &mut egui::Ui, probe: &RegisteredProbe) {
    ui.label(egui::RichText::new(&probe.name).strong());
    ui.label(format!("Type: {}", probe.probe_type.display_name()));

    if let Some(msg) = &probe.status_message {
        ui.label(format!("Status: {}", msg));
    }

    ui.horizontal(|ui| {
        if ui.button("Test Connection").clicked() {
            // TODO: Test probe connectivity
        }
        if ui.button("Delete").clicked() {
            // TODO: Delete probe
        }
    });
}

/// Render the probe registration form.
fn render_probe_registration(
    ui: &mut egui::Ui,
    _instance_id: InstanceId,
    state: &mut ProbeControllerState,
) {
    // Probe type selection
    if state.selected_probe_type.is_none() {
        ui.label("Select probe type:");
        ui.add_space(8.0);

        egui::ScrollArea::vertical()
            .max_height(320.0)
            .show(ui, |ui| {
                egui::Grid::new("probe_type_grid")
                    .num_columns(2)
                    .spacing([10.0, 8.0])
                    .show(ui, |ui| {
                        for (i, probe_type) in ProbeType::all().iter().enumerate() {
                            if ui
                                .button(format!(
                                    "{}\n{}",
                                    probe_type.display_name(),
                                    probe_type.description()
                                ))
                                .clicked()
                            {
                                state.selected_probe_type = Some(*probe_type);
                                state.form.reset();
                            }
                            if i % 2 == 1 {
                                ui.end_row();
                            }
                        }
                    });
            });
    } else {
        let probe_type = state.selected_probe_type.unwrap();

        // Back button and type header
        ui.horizontal(|ui| {
            if ui.button("< Back").clicked() {
                state.selected_probe_type = None;
                state.form.reset();
            }
            ui.label(egui::RichText::new(probe_type.display_name()).strong());
        });

        ui.separator();

        egui::ScrollArea::vertical()
            .max_height(280.0)
            .show(ui, |ui| {
                // Name field (common to all)
                ui.horizontal(|ui| {
                    ui.label("Name:");
                    ui.text_edit_singleline(&mut state.form.name);
                });

                ui.add_space(8.0);

                // Type-specific fields
                render_probe_form_fields(ui, probe_type, &mut state.form);
            });

        ui.add_space(8.0);
        ui.separator();

        ui.horizontal(|ui| {
            if ui.button("Test Connection").clicked() {
                // TODO: Test connectivity
            }
            if ui.button("Register").clicked() {
                // TODO: Submit registration
            }
        });
    }
}

/// Render form fields for a specific probe type.
fn render_probe_form_fields(ui: &mut egui::Ui, probe_type: ProbeType, form: &mut ProbeFormState) {
    match probe_type {
        ProbeType::Rdp => {
            render_host_port_fields(ui, form, 3389);
            ui.horizontal(|ui| {
                ui.label("Domain:");
                ui.text_edit_singleline(&mut form.domain);
            });
            render_credential_fields(ui, form);
        }
        ProbeType::Ssh => {
            render_host_port_fields(ui, form, 22);
            render_credential_fields(ui, form);
            ui.horizontal(|ui| {
                ui.label("Private Key:");
                ui.text_edit_singleline(&mut form.private_key_path);
            });
            ui.horizontal(|ui| {
                ui.label("Fingerprint:");
                ui.text_edit_singleline(&mut form.fingerprint);
            });
        }
        ProbeType::Ups => {
            render_host_port_fields(ui, form, 3493);
            ui.horizontal(|ui| {
                ui.label("UPS Name:");
                ui.text_edit_singleline(&mut form.ups_name);
            });
            render_credential_fields(ui, form);
        }
        ProbeType::Vnc => {
            render_host_port_fields(ui, form, 5900);
            ui.horizontal(|ui| {
                ui.label("Password:");
                ui.add(egui::TextEdit::singleline(&mut form.password).password(true));
            });
        }
        ProbeType::Wol => {
            ui.horizontal(|ui| {
                ui.label("MAC Address:");
                ui.text_edit_singleline(&mut form.mac_address);
            });
            ui.horizontal(|ui| {
                ui.label("Broadcast:");
                ui.text_edit_singleline(&mut form.broadcast_address);
            });
            ui.horizontal(|ui| {
                ui.label("Port:");
                ui.add(
                    egui::TextEdit::singleline(&mut form.port)
                        .hint_text("9")
                        .desired_width(60.0),
                );
            });
            ui.horizontal(|ui| {
                ui.label("Hostname:");
                ui.text_edit_singleline(&mut form.wol_hostname);
            });
        }
        ProbeType::Http => {
            ui.horizontal(|ui| {
                ui.label("URL:");
                ui.text_edit_singleline(&mut form.url);
            });
            ui.horizontal(|ui| {
                ui.label("Method:");
                ui.add(
                    egui::TextEdit::singleline(&mut form.http_method)
                        .hint_text("GET")
                        .desired_width(60.0),
                );
                ui.label("Status:");
                ui.add(
                    egui::TextEdit::singleline(&mut form.expected_status)
                        .hint_text("200")
                        .desired_width(50.0),
                );
            });
            ui.horizontal(|ui| {
                ui.label("Timeout (s):");
                ui.add(
                    egui::TextEdit::singleline(&mut form.timeout_secs)
                        .hint_text("30")
                        .desired_width(50.0),
                );
                ui.checkbox(&mut form.verify_tls, "Verify TLS");
            });
            render_credential_fields(ui, form);
        }
        ProbeType::Ipmi => {
            render_host_port_fields(ui, form, 623);
            render_credential_fields(ui, form);
            ui.horizontal(|ui| {
                ui.label("Interface:");
                ui.add(
                    egui::TextEdit::singleline(&mut form.interface_type)
                        .hint_text("lanplus")
                        .desired_width(80.0),
                );
            });
        }
        ProbeType::Rtsp => {
            ui.horizontal(|ui| {
                ui.label("RTSP URL:");
                ui.text_edit_singleline(&mut form.url);
            });
            render_credential_fields(ui, form);
            ui.horizontal(|ui| {
                ui.label("Transport:");
                egui::ComboBox::new("rtsp_transport", "")
                    .selected_text(if form.transport.is_empty() {
                        "UDP"
                    } else {
                        &form.transport
                    })
                    .show_ui(ui, |ui| {
                        ui.selectable_value(&mut form.transport, "UDP".to_string(), "UDP");
                        ui.selectable_value(&mut form.transport, "TCP".to_string(), "TCP");
                        ui.selectable_value(&mut form.transport, "HTTP".to_string(), "HTTP");
                    });
            });
        }
        ProbeType::Snmp => {
            render_host_port_fields(ui, form, 161);
            ui.horizontal(|ui| {
                ui.label("Version:");
                egui::ComboBox::new("snmp_version", "")
                    .selected_text(match form.snmp_version {
                        SnmpVersion::V1 => "v1",
                        SnmpVersion::V2c => "v2c",
                        SnmpVersion::V3 => "v3",
                    })
                    .show_ui(ui, |ui| {
                        ui.selectable_value(&mut form.snmp_version, SnmpVersion::V1, "v1");
                        ui.selectable_value(&mut form.snmp_version, SnmpVersion::V2c, "v2c");
                        ui.selectable_value(&mut form.snmp_version, SnmpVersion::V3, "v3");
                    });
            });
            match form.snmp_version {
                SnmpVersion::V1 | SnmpVersion::V2c => {
                    ui.horizontal(|ui| {
                        ui.label("Community:");
                        ui.text_edit_singleline(&mut form.community);
                    });
                }
                SnmpVersion::V3 => {
                    ui.horizontal(|ui| {
                        ui.label("Username:");
                        ui.text_edit_singleline(&mut form.username);
                    });
                    ui.horizontal(|ui| {
                        ui.label("Auth Protocol:");
                        egui::ComboBox::new("snmp_auth", "")
                            .selected_text(if form.auth_protocol.is_empty() {
                                "None"
                            } else {
                                &form.auth_protocol
                            })
                            .show_ui(ui, |ui| {
                                ui.selectable_value(&mut form.auth_protocol, "".to_string(), "None");
                                ui.selectable_value(
                                    &mut form.auth_protocol,
                                    "MD5".to_string(),
                                    "MD5",
                                );
                                ui.selectable_value(
                                    &mut form.auth_protocol,
                                    "SHA".to_string(),
                                    "SHA",
                                );
                            });
                    });
                    if !form.auth_protocol.is_empty() {
                        ui.horizontal(|ui| {
                            ui.label("Auth Password:");
                            ui.add(egui::TextEdit::singleline(&mut form.auth_password).password(true));
                        });
                    }
                    ui.horizontal(|ui| {
                        ui.label("Privacy Protocol:");
                        egui::ComboBox::new("snmp_priv", "")
                            .selected_text(if form.priv_protocol.is_empty() {
                                "None"
                            } else {
                                &form.priv_protocol
                            })
                            .show_ui(ui, |ui| {
                                ui.selectable_value(&mut form.priv_protocol, "".to_string(), "None");
                                ui.selectable_value(
                                    &mut form.priv_protocol,
                                    "AES".to_string(),
                                    "AES",
                                );
                                ui.selectable_value(
                                    &mut form.priv_protocol,
                                    "DES".to_string(),
                                    "DES",
                                );
                            });
                    });
                    if !form.priv_protocol.is_empty() {
                        ui.horizontal(|ui| {
                            ui.label("Privacy Password:");
                            ui.add(egui::TextEdit::singleline(&mut form.priv_password).password(true));
                        });
                    }
                }
            }
        }
        ProbeType::Onvif => {
            render_host_port_fields(ui, form, 80);
            render_credential_fields(ui, form);
            ui.horizontal(|ui| {
                ui.label("Profile Token:");
                ui.text_edit_singleline(&mut form.profile_token);
            });
        }
        ProbeType::Docker => {
            ui.horizontal(|ui| {
                ui.label("Host:");
                ui.text_edit_singleline(&mut form.host);
            });
            ui.label(
                egui::RichText::new("e.g., unix:///var/run/docker.sock or tcp://host:2375")
                    .small()
                    .weak(),
            );
            ui.add_space(4.0);
            ui.collapsing("TLS Settings", |ui| {
                ui.checkbox(&mut form.tls_verify, "Verify TLS");
                ui.horizontal(|ui| {
                    ui.label("CA Cert:");
                    ui.text_edit_singleline(&mut form.tls_ca_cert);
                });
                ui.horizontal(|ui| {
                    ui.label("Client Cert:");
                    ui.text_edit_singleline(&mut form.tls_cert);
                });
                ui.horizontal(|ui| {
                    ui.label("Client Key:");
                    ui.text_edit_singleline(&mut form.tls_key);
                });
            });
        }
        ProbeType::Libvirt => {
            ui.horizontal(|ui| {
                ui.label("URI:");
                ui.text_edit_singleline(&mut form.uri);
            });
            ui.label(
                egui::RichText::new("e.g., qemu:///system or qemu+ssh://user@host/system")
                    .small()
                    .weak(),
            );
            ui.add_space(4.0);
            ui.collapsing("SSH Settings", |ui| {
                ui.horizontal(|ui| {
                    ui.label("Username:");
                    ui.text_edit_singleline(&mut form.username);
                });
                ui.horizontal(|ui| {
                    ui.label("Private Key:");
                    ui.text_edit_singleline(&mut form.private_key_path);
                });
            });
        }
    }
}

/// Render common host/port fields.
fn render_host_port_fields(ui: &mut egui::Ui, form: &mut ProbeFormState, default_port: u16) {
    ui.horizontal(|ui| {
        ui.label("Host:");
        ui.text_edit_singleline(&mut form.host);
    });
    ui.horizontal(|ui| {
        ui.label("Port:");
        ui.add(
            egui::TextEdit::singleline(&mut form.port)
                .hint_text(default_port.to_string())
                .desired_width(60.0),
        );
    });
}

/// Render common username/password fields.
fn render_credential_fields(ui: &mut egui::Ui, form: &mut ProbeFormState) {
    ui.horizontal(|ui| {
        ui.label("Username:");
        ui.text_edit_singleline(&mut form.username);
    });
    ui.horizontal(|ui| {
        ui.label("Password:");
        ui.add(egui::TextEdit::singleline(&mut form.password).password(true));
    });
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

/// Probe layer GUI extension.
pub struct ProbeGuiExtension;

impl LayerGuiExtension for ProbeGuiExtension {
    fn layer(&self) -> &LayerName {
        static LAYER: std::sync::LazyLock<LayerName> =
            std::sync::LazyLock::new(|| LayerName::from("Probe"));
        &LAYER
    }

    fn description(&self) -> &'static str {
        "Device monitoring probes"
    }

    fn render_controller(&self, ui: &mut egui::Ui, instance_id: InstanceId) {
        render_probe_controller(ui, instance_id);
    }

    fn controller_name(&self) -> &'static str {
        "Probe Manager"
    }

    fn get_node_svg(&self, _instance_id: InstanceId) -> &'static str {
        "layer/Probe.svg"
    }

    fn get_node_color(&self, instance_id: InstanceId) -> Color {
        // Color based on probe health - green if all probes online, yellow if some offline, red if all offline
        let probes = query_probes(instance_id);
        if probes.is_empty() {
            return Color::WHITE;
        }

        let online_count = probes.iter().filter(|p| p.online).count();
        let total = probes.len();

        if online_count == total {
            Color::srgb(0.7, 1.0, 0.7) // Green - all online
        } else if online_count == 0 {
            Color::srgb(1.0, 0.7, 0.7) // Red - all offline
        } else {
            Color::srgb(1.0, 1.0, 0.7) // Yellow - some offline
        }
    }

    fn preview_icon(&self) -> &'static str {
        "Probe"
    }

    fn preview_details(&self, instance_id: InstanceId) -> String {
        let probes = query_probes(instance_id);
        if probes.is_empty() {
            "No probes registered".to_string()
        } else {
            let online = probes.iter().filter(|p| p.online).count();
            format!("{}/{} probes online", online, probes.len())
        }
    }

    fn edge_color(&self) -> Color {
        Color::srgb(0.8, 0.5, 0.3) // Orange
    }

    fn activity_types(&self) -> Vec<ActivityTypeInfo> {
        vec![ActivityTypeInfo {
            id: "probe_check",
            name: "Probe Check",
            color: Color::srgb(0.8, 0.5, 0.3),
            size: 5.0,
        }]
    }

    fn register_systems(&self, app: &mut App) {
        app.add_systems(
            Update,
            (
                scale_probe_node_svgs,
                update_probe_nodes,
                apply_probe_spring_forces,
                update_probe_node_visibility,
            ),
        );
    }

    fn visible_instance_types(&self) -> &'static [sandpolis_core::InstanceType] {
        // Probe layer shows servers and agents (not clients)
        &[sandpolis_core::InstanceType::Server, sandpolis_core::InstanceType::Agent]
    }

    fn show_probe_nodes(&self) -> bool {
        // Probe layer is the only layer that shows probe nodes
        true
    }
}

/// Static instance of the probe GUI extension.
static PROBE_GUI_EXT: ProbeGuiExtension = ProbeGuiExtension;

// Register the extension with inventory
inventory::submit! {
    &PROBE_GUI_EXT as &dyn LayerGuiExtension
}
