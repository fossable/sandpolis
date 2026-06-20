//! The device-management stream: register/delete/list managed devices.
//!
//! A client opens one [`DeviceMgmtRequester`] to the server and `Subscribe`s; the
//! server's [`DeviceMgmtResponder`] answers with the current device list and then
//! streams the full list again whenever it changes. `Register`/`Delete` requests
//! (sent on short-lived streams) mutate the server's [`REGISTERED_DEVICES`],
//! persist them to `sandpolis.ron`, and broadcast the new list to all subscribers.

use crate::RegisteredDevice;
use crate::config::DeviceConfig;
use sandpolis_instance::InstanceId;
use serde::{Deserialize, Serialize};

/// Requests from a client to the server's device manager.
#[derive(Serialize, Deserialize, Debug)]
pub enum DeviceMgmtRequest {
    /// Begin receiving the device list (snapshot + updates).
    Subscribe,
    /// Register a new device on `gateway`.
    Register {
        gateway: InstanceId,
        device: DeviceConfig,
    },
    /// Delete the device with this id.
    Delete { id: u64 },
}

/// Responses from the server's device manager.
#[derive(Serialize, Deserialize, Debug)]
pub enum DeviceMgmtResponse {
    /// The current full device list.
    List(Vec<RegisteredDevice>),
    /// An operation failed.
    Error(String),
}

#[cfg(feature = "server")]
mod server {
    use super::*;
    use crate::REGISTERED_DEVICES;
    use anyhow::Result;
    use sandpolis_instance::network::{
        RegisterResponders, ResponderRegistration, StreamRegistry, StreamResponder,
    };
    use sandpolis_macros::Stream;
    use std::sync::LazyLock;
    use tokio::sync::broadcast;
    use tokio::sync::mpsc::Sender;

    /// Broadcasts the full device list to every active subscriber whenever it
    /// changes.
    static DEVICE_BROADCAST: LazyLock<broadcast::Sender<Vec<RegisteredDevice>>> =
        LazyLock::new(|| broadcast::channel(16).0);

    /// Snapshot the current device list.
    fn snapshot() -> Vec<RegisteredDevice> {
        REGISTERED_DEVICES.read().unwrap().clone()
    }

    /// Persist the current list and notify subscribers.
    fn commit() {
        let devices = snapshot();
        crate::persist_devices(&devices);
        let _ = DEVICE_BROADCAST.send(devices);
    }

    /// Server side of the management stream.
    #[derive(Stream, Default)]
    pub struct DeviceMgmtResponder;

    impl StreamResponder for DeviceMgmtResponder {
        type In = DeviceMgmtRequest;
        type Out = DeviceMgmtResponse;

        async fn on_message(&self, request: Self::In, sender: Sender<Self::Out>) -> Result<()> {
            match request {
                DeviceMgmtRequest::Subscribe => {
                    // Snapshot first, then live updates.
                    sender.send(DeviceMgmtResponse::List(snapshot())).await?;

                    let mut rx = DEVICE_BROADCAST.subscribe();
                    let sender = sender.clone();
                    tokio::spawn(async move {
                        loop {
                            match rx.recv().await {
                                Ok(list) => {
                                    if sender.send(DeviceMgmtResponse::List(list)).await.is_err() {
                                        break;
                                    }
                                }
                                // Dropped messages: resend the current snapshot.
                                Err(broadcast::error::RecvError::Lagged(_)) => {
                                    if sender
                                        .send(DeviceMgmtResponse::List(snapshot()))
                                        .await
                                        .is_err()
                                    {
                                        break;
                                    }
                                }
                                Err(broadcast::error::RecvError::Closed) => break,
                            }
                        }
                    });
                }
                DeviceMgmtRequest::Register { gateway, device } => {
                    {
                        let mut devices = REGISTERED_DEVICES.write().unwrap();
                        let id = devices.iter().map(|d| d.id).max().unwrap_or(0) + 1;
                        devices.push(RegisteredDevice {
                            id,
                            gateway,
                            device,
                            online: false,
                            status_message: None,
                        });
                    }
                    commit();
                }
                DeviceMgmtRequest::Delete { id } => {
                    {
                        let mut devices = REGISTERED_DEVICES.write().unwrap();
                        devices.retain(|d| d.id != id);
                    }
                    commit();
                }
            }
            Ok(())
        }
    }

    /// Registers [`DeviceMgmtResponder`] on each connection.
    pub struct DeviceMgmtResponderRegistration;

    impl RegisterResponders for DeviceMgmtResponderRegistration {
        fn register_responders(&self, registry: &StreamRegistry) {
            registry.register_responder(DeviceMgmtResponder::default);
        }
    }

    inventory::submit!(ResponderRegistration(&DeviceMgmtResponderRegistration));
}

#[cfg(feature = "client-gui")]
mod client {
    use super::*;
    use crate::REGISTERED_DEVICES;
    use anyhow::Result;
    use sandpolis_instance::network::InstanceConnection;
    use sandpolis_instance::network::StreamRequester;
    use sandpolis_instance::network::stream::StreamMessage;
    use sandpolis_macros::Stream;
    use std::sync::Arc;
    use tokio::sync::mpsc::Sender;

    /// Client side of the management stream: applies received device lists into
    /// the local [`REGISTERED_DEVICES`].
    #[derive(Stream, Default)]
    pub struct DeviceMgmtRequester;

    impl StreamRequester for DeviceMgmtRequester {
        type In = DeviceMgmtResponse;
        type Out = DeviceMgmtRequest;

        async fn new(_: Self::Out, _: Sender<Self::Out>) -> Result<Self> {
            // Constructed directly and registered via `subscribe`/`send`.
            anyhow::bail!("DeviceMgmtRequester must be constructed directly")
        }

        async fn on_message(&self, response: Self::In, _: Sender<Self::Out>) -> Result<()> {
            match response {
                DeviceMgmtResponse::List(list) => {
                    *REGISTERED_DEVICES.write().unwrap() = list;
                }
                DeviceMgmtResponse::Error(e) => {
                    tracing::warn!(error = %e, "Device management error");
                }
            }
            Ok(())
        }
    }

    /// Open a long-lived subscription so [`REGISTERED_DEVICES`] tracks the server.
    /// The returned stream stays registered (and receiving updates) even after the
    /// outbound sender is dropped, so we only need to send the initial request.
    pub fn subscribe(conn: Arc<InstanceConnection>) {
        tokio::spawn(async move {
            let (id, tx) = conn.register_stream(DeviceMgmtRequester);
            let payload = match serde_cbor::to_vec(&DeviceMgmtRequest::Subscribe) {
                Ok(p) => p,
                Err(_) => return,
            };
            let _ = tx
                .send(StreamMessage {
                    stream_id: id,
                    payload,
                    dst: None,
                })
                .await;
        });
    }

    /// Send a one-shot management request (Register/Delete) to the server.
    fn send_request(conn: Arc<InstanceConnection>, request: DeviceMgmtRequest) {
        tokio::spawn(async move {
            let (id, tx) = conn.register_stream(DeviceMgmtRequester);
            let payload = match serde_cbor::to_vec(&request) {
                Ok(p) => p,
                Err(_) => return,
            };
            let _ = tx
                .send(StreamMessage {
                    stream_id: id,
                    payload,
                    dst: None,
                })
                .await;
            conn.close_stream(id);
        });
    }

    /// Register a new device on `gateway`.
    pub fn register_device(conn: Arc<InstanceConnection>, gateway: InstanceId, device: DeviceConfig) {
        send_request(conn, DeviceMgmtRequest::Register { gateway, device });
    }

    /// Delete the device with `id`.
    pub fn delete_device(conn: Arc<InstanceConnection>, id: u64) {
        send_request(conn, DeviceMgmtRequest::Delete { id });
    }
}

#[cfg(feature = "client-gui")]
pub use client::{DeviceMgmtRequester, delete_device, register_device, subscribe};
