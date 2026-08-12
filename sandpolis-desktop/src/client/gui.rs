//! GUI components for the Desktop layer.
//!
//! Provides the desktop-viewer node controller and the layer's client plugin.
//!
//! The controller wires the full client-side stream pipeline. On "Start Stream"
//! it opens a relayed stream to the agent with [`sandpolis_client::sync`]'s
//! websocket connection (`open_stream_to`): the server routes requests to the
//! target agent and responses back. [`DesktopStreamRequester`] decodes incoming
//! frames into RGBA8, a Bevy system drains them and uploads them to the display
//! node's texture, and pointer / keyboard input over that node is mapped into
//! [`DesktopStreamInputEvent`]s and forwarded over the stream's outbound channel.

use bevy::asset::RenderAssetUsages;
use bevy::image::Image;
use bevy::input::ButtonState;
use bevy::input::keyboard::{Key, KeyboardInput};
use bevy::prelude::*;
use bevy::render::render_resource::{Extent3d, TextureDimension, TextureFormat};
use bevy::ui::RelativeCursorPosition;
use sandpolis_client::gui::ui::Activate;
use sandpolis_client::gui::ui::bind::bind_text;
use sandpolis_client::gui::controller::ControllerTarget;
use sandpolis_client::gui::ui::controller::{LayerClientInfo, NodeController, RegisterLayerClient};
use sandpolis_client::gui::ui::theme::{Role, Theme};
use sandpolis_client::gui::ui::widgets::{button, heading, row, text};
use sandpolis_instance::network::stream::StreamMessage;
use sandpolis_instance::{InstanceId, InstanceType, LayerName};
use std::collections::HashMap;
use tokio::sync::mpsc::{Receiver, Sender, UnboundedReceiver, channel};

use crate::screenshot::{DesktopScreenshotRequest, DesktopScreenshotRequester, DesktopScreenshotResult};
use crate::session::{
    DesktopFrame, DesktopStreamColorMode, DesktopStreamCompressionMode, DesktopStreamEvent,
    DesktopStreamInputEvent, DesktopStreamPointerButton, DesktopStreamRequest,
    DesktopStreamRequester,
};

/// Active client-side desktop sessions, keyed by the node they belong to.
///
/// The key is a [`ControllerTarget`] rather than a bare `InstanceId` because a
/// probe node borrows its gateway server's id; only the sub-node device id tells
/// a VNC probe apart from the server it orbits.
#[derive(Resource, Default)]
struct DesktopStreams {
    streams: HashMap<ControllerTarget, StreamSession>,
    screenshots: HashMap<ControllerTarget, ScreenshotSession>,
}

/// A live stream the GUI is rendering.
struct StreamSession {
    /// Decoded frames/state pushed by the requester (registered on the
    /// connection; the requester holds the sending half).
    events: UnboundedReceiver<DesktopStreamEvent>,
    /// Outbound requests (input, Stop). A background task forwards these over
    /// the stream, translating to the VNC wire type for probe streams.
    outbound: Sender<DesktopStreamRequest>,
    /// The display node showing this stream.
    view: Entity,
    /// Remote display dimensions, once known.
    size: Option<(u32, u32)>,
    /// Last pointer position sent, to avoid flooding identical moves.
    last_pointer: Option<(i32, i32)>,
}

/// A pending one-shot screenshot.
struct ScreenshotSession {
    result: UnboundedReceiver<DesktopScreenshotResult>,
    view: Entity,
}

/// The display node showing a desktop stream/screenshot for `target`.
#[derive(Component)]
struct DesktopStreamView {
    target: ControllerTarget,
}

/// A status label reflecting the stream state for `target`.
#[derive(Component)]
struct DesktopStatusText {
    target: ControllerTarget,
}

/// The desktop layer's node controller (remote desktop viewer).
pub struct DesktopController;

impl NodeController for DesktopController {
    fn title(&self) -> &str {
        "Desktop Viewer"
    }

    fn title_for(&self, target: ControllerTarget) -> String {
        #[cfg(feature = "probe")]
        if let Some(device) = probe_device(target) {
            return device.display_name();
        }
        #[cfg(not(feature = "probe"))]
        let _ = target;
        self.title().to_string()
    }

    fn build(
        &self,
        commands: &mut Commands,
        body: Entity,
        target: ControllerTarget,
        theme: &Theme,
    ) {
        let instance = target.instance;

        // A probe that only speaks RDP still gets a node (the layer shows them
        // so the estate looks complete), but there's nothing to drive yet.
        #[cfg(feature = "probe")]
        if is_rdp_only(target) {
            commands.entity(body).with_children(|p| {
                p.spawn(heading(theme, "Desktop Stream"));
                p.spawn(text(
                    theme,
                    "RDP streaming is not implemented yet.",
                    theme.metrics.font_sm,
                    Role::TextMuted,
                ));
            });
            return;
        }

        commands.entity(body).with_children(|p| {
            p.spawn(heading(theme, "Desktop Stream"));

            // Live stream display: an `ImageNode` (transparent until frames
            // arrive, so the dark background shows through) over a dark
            // background. `RelativeCursorPosition` maps the pointer into the
            // remote display's coordinate space for input forwarding.
            p.spawn((
                DesktopStreamView { target },
                Node {
                    width: Val::Percent(100.0),
                    height: Val::Px(280.0),
                    ..default()
                },
                BackgroundColor(Color::srgb_u8(30, 30, 30)),
                ImageNode {
                    color: Color::NONE,
                    ..default()
                },
                Interaction::default(),
                RelativeCursorPosition::default(),
            ));

            // Controls.
            p.spawn(row(theme.metrics.space_sm)).with_children(|controls| {
                controls
                    .spawn(button(theme, "Start Stream"))
                    .observe(
                        move |_: On<Activate>,
                              mut streams: ResMut<DesktopStreams>,
                              views: Query<(Entity, &DesktopStreamView)>| {
                            if streams.streams.contains_key(&target) {
                                return;
                            }
                            let Some((view, _)) =
                                views.iter().find(|(_, v)| v.target == target)
                            else {
                                return;
                            };

                            let (outbound, outbound_rx) = channel(64);
                            let events = open_stream(target, outbound_rx);

                            streams.streams.insert(
                                target,
                                StreamSession {
                                    events,
                                    outbound,
                                    view,
                                    size: None,
                                    last_pointer: None,
                                },
                            );
                            info!("Desktop stream requested for {}", instance);
                        },
                    );

                controls.spawn(button(theme, "Stop Stream")).observe(
                    move |_: On<Activate>,
                          mut streams: ResMut<DesktopStreams>,
                          mut nodes: Query<&mut ImageNode>| {
                        if let Some(session) = streams.streams.remove(&target) {
                            let _ = session.outbound.try_send(DesktopStreamRequest::Stop);
                            if let Ok(mut node) = nodes.get_mut(session.view) {
                                node.color = Color::NONE;
                            }
                            info!("Desktop stream stopped for {}", instance);
                        }
                    },
                );

                // Screenshots are a one-shot agent responder; a probe has no
                // equivalent, so the button only exists for agents.
                if target.sub.is_none() {
                    controls.spawn(button(theme, "Screenshot")).observe(
                        move |_: On<Activate>,
                              mut streams: ResMut<DesktopStreams>,
                              views: Query<(Entity, &DesktopStreamView)>| {
                            let Some((view, _)) = views.iter().find(|(_, v)| v.target == target)
                            else {
                                return;
                            };
                            let (requester, result) = DesktopScreenshotRequester::channel();
                            // One-shot: open a relayed stream to the agent with
                            // the screenshot request; the response returns via
                            // `result`.
                            spawn_screenshot(
                                instance,
                                requester,
                                DesktopScreenshotRequest {
                                    desktop_uuid: String::new(),
                                },
                            );
                            streams.screenshots.insert(
                                target,
                                ScreenshotSession { result, view },
                            );
                            info!("Screenshot requested for {}", instance);
                        },
                    );
                }
            });

            // Stream status line.
            p.spawn((
                DesktopStatusText { target },
                text(theme, "Stream inactive", theme.metrics.font_sm, Role::TextMuted),
            ));

            // Node information. A probe isn't an instance, so it has no instance
            // metadata to report — its registration is all there is to say.
            p.spawn((
                text(theme, "", theme.metrics.font_md, Role::Text),
                bind_text(move || describe_target(target)),
            ));
        });
    }
}

/// One line describing what the controller is pointed at.
fn describe_target(target: ControllerTarget) -> String {
    #[cfg(feature = "probe")]
    if let Some(device) = probe_device(target) {
        return format!("{} — {}", device.display_name(), device.device.ip);
    }
    match sandpolis_client::gui::queries::query_instance_metadata(target.instance) {
        Ok(m) => {
            let host = m.hostname.unwrap_or_else(|| "unknown".into());
            format!("{} — OS: {}", host, m.os_type)
        }
        Err(_) => "OS: unknown".into(),
    }
}

/// The probe device behind `target`, if there is one.
///
/// Reads the probe layer's device registry directly rather than going through
/// `sandpolis_probe::client::gui`, whose helpers are behind that crate's `client`
/// feature — depending on them would drag its whole GUI stack in here.
#[cfg(feature = "probe")]
fn probe_device(target: ControllerTarget) -> Option<sandpolis_probe::RegisteredDevice> {
    let device_id = target.sub?;
    let device = sandpolis_probe::REGISTERED_DEVICES
        .read()
        .ok()?
        .iter()
        .find(|device| device.id == device_id)?
        .clone();
    Some(device)
}

/// Whether `target` is a probe this layer shows but can't stream yet.
#[cfg(feature = "probe")]
fn is_rdp_only(target: ControllerTarget) -> bool {
    probe_device(target)
        .is_some_and(|device| device.device.rdp.is_some() && device.device.vnc.is_none())
}

/// Open the stream backing a new session and hand back the channel its events
/// arrive on.
///
/// A probe target gets a VNC session run by the device's owning server; anything
/// else gets a capture stream relayed to the agent. Both produce
/// [`DesktopStreamEvent`], so the viewer above this doesn't care which it got.
fn open_stream(
    target: ControllerTarget,
    outbound_rx: Receiver<DesktopStreamRequest>,
) -> UnboundedReceiver<DesktopStreamEvent> {
    #[cfg(feature = "probe")]
    if let Some(device) = probe_device(target)
        && device.device.vnc.is_some()
    {
        let (requester, events) = crate::vnc::VncStreamRequester::channel();
        let initial = crate::vnc::VncStreamRequest::Start {
            device_id: device.id,
        };
        spawn_vnc_stream(&device, requester, initial, outbound_rx);
        return events;
    }

    let (requester, events) = DesktopStreamRequester::channel();
    let initial = DesktopStreamRequest::Start {
        desktop_uuid: String::new(),
        color_mode: DesktopStreamColorMode::Rgb888,
        compression_mode: DesktopStreamCompressionMode::Zstd,
    };
    spawn_stream(target.instance, requester, initial, outbound_rx);
    events
}

/// Open a VNC stream against a probe device and forward outbound requests over
/// it until the channel closes.
///
/// Probes are reachable only from servers, so this opens a stream *to the owning
/// server* (not a relayed one to an agent) and translates the viewer's
/// agent-shaped requests into the VNC wire type on the way out.
#[cfg(feature = "probe")]
fn spawn_vnc_stream(
    device: &sandpolis_probe::RegisteredDevice,
    requester: crate::vnc::VncStreamRequester,
    initial: crate::vnc::VncStreamRequest,
    mut outbound_rx: Receiver<DesktopStreamRequest>,
) {
    let conn = device
        .device
        .server
        .as_ref()
        .and_then(sandpolis_client::sync::connection_for)
        .or_else(sandpolis_client::sync::connection);
    let Some(conn) = conn else {
        warn!("No server connection; cannot start VNC stream");
        return;
    };
    sandpolis_client::sync::spawn(async move {
        let (id, msg_tx) = match conn.open_stream(requester, initial).await {
            Ok(v) => v,
            Err(e) => {
                warn!(error = %e, "Failed to open VNC stream");
                return;
            }
        };
        while let Some(req) = outbound_rx.recv().await {
            let req = match req {
                DesktopStreamRequest::Input(event) => crate::vnc::VncStreamRequest::Input(event),
                DesktopStreamRequest::Stop => crate::vnc::VncStreamRequest::Stop,
                // Sent as the stream's initial request, never through here.
                DesktopStreamRequest::Start { .. } => continue,
            };
            let payload = match serde_cbor::to_vec(&req) {
                Ok(p) => p,
                Err(_) => continue,
            };
            if msg_tx.send(StreamMessage::local(id, payload)).await.is_err() {
                break;
            }
        }
        conn.close_stream(id);
    });
}

/// Open a relayed desktop stream to `instance` and forward outbound requests
/// (input, Stop) over it until the channel closes.
fn spawn_stream(
    instance: InstanceId,
    requester: DesktopStreamRequester,
    initial: DesktopStreamRequest,
    mut outbound_rx: Receiver<DesktopStreamRequest>,
) {
    let Some(conn) = sandpolis_client::sync::connection() else {
        warn!("No server connection; cannot start desktop stream");
        return;
    };
    sandpolis_client::sync::spawn(async move {
        let (id, msg_tx) = match conn.open_stream_to(instance, requester, initial).await {
            Ok(v) => v,
            Err(e) => {
                warn!(error = %e, "Failed to open desktop stream");
                return;
            }
        };
        while let Some(req) = outbound_rx.recv().await {
            let payload = match serde_cbor::to_vec(&req) {
                Ok(p) => p,
                Err(_) => continue,
            };
            if msg_tx
                .send(StreamMessage::to(id, payload, instance))
                .await
                .is_err()
            {
                break;
            }
        }
        conn.close_stream(id);
    });
}

/// Open a one-shot relayed screenshot stream to `instance`. The response returns
/// over the requester's channel.
fn spawn_screenshot(
    instance: InstanceId,
    requester: DesktopScreenshotRequester,
    initial: DesktopScreenshotRequest,
) {
    let Some(conn) = sandpolis_client::sync::connection() else {
        warn!("No server connection; cannot request screenshot");
        return;
    };
    sandpolis_client::sync::spawn(async move {
        if let Err(e) = conn.open_stream_to(instance, requester, initial).await {
            warn!(error = %e, "Failed to request screenshot");
        }
    });
}

/// Build an RGBA8 [`Image`] for a decoded frame.
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

/// Drain decoded stream frames and upload the latest to the display texture.
fn drive_desktop_streams(
    mut streams: ResMut<DesktopStreams>,
    mut images: ResMut<Assets<Image>>,
    mut nodes: Query<&mut ImageNode>,
) {
    for session in streams.streams.values_mut() {
        // Coalesce to the most recent frame so we only upload one texture/tick.
        let mut latest: Option<DesktopFrame> = None;
        while let Ok(event) = session.events.try_recv() {
            match event {
                DesktopStreamEvent::Started { width, height } => {
                    session.size = Some((width, height));
                }
                DesktopStreamEvent::Frame(frame) => latest = Some(frame),
                DesktopStreamEvent::Stopped => {}
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

/// Drain pending screenshots and display the returned image.
fn drive_desktop_screenshots(
    mut streams: ResMut<DesktopStreams>,
    mut images: ResMut<Assets<Image>>,
    mut nodes: Query<&mut ImageNode>,
) {
    let mut finished = Vec::new();
    for (target, session) in streams.screenshots.iter_mut() {
        let instance = target.instance;
        while let Ok(result) = session.result.try_recv() {
            match result {
                DesktopScreenshotResult::Ok(png) => match crate::screenshot::decode_png(&png) {
                    Ok(frame) if frame.width > 0 && frame.height > 0 => {
                        let handle =
                            images.add(image_from_rgba(frame.width, frame.height, frame.rgba));
                        if let Ok(mut node) = nodes.get_mut(session.view) {
                            node.image = handle;
                            node.color = Color::WHITE;
                        }
                        info!("Screenshot received for {}", instance);
                    }
                    Ok(_) => {}
                    Err(e) => warn!(error = %e, "Failed to decode screenshot for {}", instance),
                },
                DesktopScreenshotResult::Failed => {
                    warn!("Screenshot failed for {}", instance);
                }
            }
            finished.push(*target);
        }
    }
    for target in finished {
        streams.screenshots.remove(&target);
    }
}

/// Reflect each instance's stream state in its status label.
fn update_desktop_status(
    streams: Res<DesktopStreams>,
    mut labels: Query<(&DesktopStatusText, &mut Text)>,
) {
    for (status, mut label) in &mut labels {
        let value = match streams.streams.get(&status.target) {
            Some(session) => match session.size {
                Some((w, h)) => format!("Streaming {}×{}", w, h),
                None => "Connecting…".to_string(),
            },
            None => "Stream inactive".to_string(),
        };
        if label.0 != value {
            label.0 = value;
        }
    }
}

/// Map a just-pressed/just-released mouse button to a stream pointer button.
fn pointer_button(
    mouse: &ButtonInput<MouseButton>,
    pressed: bool,
) -> Option<DesktopStreamPointerButton> {
    let hit = |b: MouseButton| {
        if pressed {
            mouse.just_pressed(b)
        } else {
            mouse.just_released(b)
        }
    };
    if hit(MouseButton::Left) {
        Some(DesktopStreamPointerButton::Primary)
    } else if hit(MouseButton::Right) {
        Some(DesktopStreamPointerButton::Secondary)
    } else if hit(MouseButton::Middle) {
        Some(DesktopStreamPointerButton::Middle)
    } else {
        None
    }
}

/// Extract a typed character from a logical key, if any.
fn logical_char(key: &Key) -> Option<char> {
    match key {
        Key::Character(s) => s.chars().next(),
        Key::Space => Some(' '),
        _ => None,
    }
}

/// An input event with no fields set.
fn empty_input() -> DesktopStreamInputEvent {
    DesktopStreamInputEvent {
        key_pressed: None,
        key_released: None,
        key_typed: None,
        pointer_pressed: None,
        pointer_released: None,
        pointer_x: None,
        pointer_y: None,
        clipboard: None,
    }
}

/// Forward pointer and keyboard input over the hovered active stream.
fn forward_desktop_input(
    mut streams: ResMut<DesktopStreams>,
    mouse: Res<ButtonInput<MouseButton>>,
    mut keys: MessageReader<KeyboardInput>,
    views: Query<(&DesktopStreamView, &RelativeCursorPosition)>,
) {
    // Keyboard events are global; route them to whichever view is hovered.
    let key_events: Vec<(Option<char>, ButtonState)> = keys
        .read()
        .map(|ev| (logical_char(&ev.logical_key), ev.state))
        .collect();

    for (view, rel) in &views {
        let Some(session) = streams.streams.get_mut(&view.target) else {
            continue;
        };
        let Some((w, h)) = session.size else {
            continue;
        };
        let Some(norm) = rel.normalized else {
            // Pointer left the view; reset so re-entry re-sends a position.
            session.last_pointer = None;
            continue;
        };

        let x = (norm.x.clamp(0.0, 1.0) * w as f32).round() as i32;
        let y = (norm.y.clamp(0.0, 1.0) * h as f32).round() as i32;
        let pressed = pointer_button(&mouse, true);
        let released = pointer_button(&mouse, false);
        let moved = session.last_pointer != Some((x, y));

        if moved || pressed.is_some() || released.is_some() {
            let mut event = empty_input();
            event.pointer_x = Some(x);
            event.pointer_y = Some(y);
            event.pointer_pressed = pressed;
            event.pointer_released = released;
            let _ = session.outbound.try_send(DesktopStreamRequest::Input(event));
            session.last_pointer = Some((x, y));
        }

        for (character, state) in &key_events {
            let Some(c) = character else {
                continue;
            };
            let mut event = empty_input();
            match state {
                ButtonState::Pressed => {
                    event.key_pressed = Some(*c);
                    event.key_typed = Some(*c);
                }
                ButtonState::Released => {
                    event.key_released = Some(*c);
                }
            }
            let _ = session.outbound.try_send(DesktopStreamRequest::Input(event));
        }
    }
}

/// The desktop layer's client plugin.
pub struct DesktopClientPlugin;

impl Plugin for DesktopClientPlugin {
    fn build(&self, app: &mut App) {
        app.init_resource::<DesktopStreams>()
            .add_systems(
                Update,
                (
                    drive_desktop_streams,
                    drive_desktop_screenshots,
                    update_desktop_status,
                    forward_desktop_input,
                ),
            )
            .register_layer_client(
                LayerClientInfo::new(
                    LayerName::from("Desktop"),
                    "Remote desktop viewing and control",
                )
                .with_controller(DesktopController)
                .with_visible_instance_types(&[InstanceType::Server, InstanceType::Agent])
                // VNC probes stream just like agents do; RDP ones are shown but
                // open a placeholder until there's a backend for them.
                .showing_probe_nodes_for(&["VNC", "RDP"]),
            );
    }
}
