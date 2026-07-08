//! Streams provide ephemeral data transfer over time. Operations like file transfers,
//! remote desktop sessions, and shell prompt sessions all run over streams.
//!
//! A stream has two endpoints: a `StreamRequester` and a `StreamResponder`. The
//! `StreamRequester` is responsible for starting the stream and it sends "requests"
//! (one or more than one). The `StreamResponder` is created as a result of the
//! `StreamRequester`'s first request and sends "responses" (one or more than one).

use crate::InstanceId;
use anyhow::Result;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::future::Future;
use std::sync::{Arc, Mutex, RwLock};
use tokio::sync::mpsc::Sender;

/// Connections may have multiple streams running concurrently, so this identifier
/// allows each stream to remain separated.
///
/// The first half identifies the stream type and the second half identifies a particular
/// stream within that type.
#[derive(Debug, Clone, Copy, Serialize, Deserialize, Hash, PartialEq, Eq)]
pub struct StreamId(u64);

impl StreamId {
    /// Extract the type tag from a stream ID.
    pub fn tag(&self) -> u32 {
        (self.0 >> 32) as u32
    }
}

/// Encapsulates the stream's local messages.
#[derive(Serialize, Deserialize)]
pub struct StreamMessage {
    pub stream_id: StreamId,
    pub payload: Vec<u8>,

    /// Destination instance for a relayed stream. `None` means the message is
    /// handled by the receiving peer directly (e.g. sync, or responses on their
    /// way back to the origin). `Some(id)` means a server should forward this to
    /// the connection for that instance.
    #[serde(default)]
    pub dst: Option<InstanceId>,
}

/// Implemented by stream types to generate unique IDs.
pub trait Stream {
    /// Use `#[derive(Stream)]` from `sandpolis_macros` to implement this.
    fn tag() -> u32
    where
        Self: Sized;
}

/// Initiates a stream and handles responses from the responder.
///
/// The requester is responsible for starting the stream by sending the initial
/// request message. It then receives responses from the `StreamResponder`.
pub trait StreamRequester: Stream + Send + Sync + Sized + 'static {
    /// Input message type (responses from the responder).
    type In: for<'de> Deserialize<'de> + Send;

    /// Output message type (requests to the responder).
    type Out: Serialize;

    /// Create a new requester and send the initial request.
    fn new(_: Self::Out, tx: Sender<Self::Out>) -> impl Future<Output = Result<Self>> + Send;

    /// Called when the stream receives a response from the responder.
    fn on_message(
        &self,
        _: Self::In,
        tx: Sender<Self::Out>,
    ) -> impl Future<Output = Result<()>> + Send {
        async { Ok(()) }
    }

    /// Generate a new unique stream ID for this stream type.
    fn generate_id() -> StreamId
    where
        Self: Sized,
    {
        StreamId(((Self::tag() as u64) << 32) | (rand::random::<u32>() as u64))
    }
}

/// Handles incoming requests from a `StreamRequester` and sends responses.
///
/// A responder is created automatically when the first request arrives for a
/// stream type that has a registered factory.
pub trait StreamResponder: Stream + Send + Sync + 'static {
    /// Input message type (requests from the requester).
    type In: for<'de> Deserialize<'de> + Send;

    /// Output message type (responses to the requester).
    type Out: Serialize;

    /// Called when the stream receives a request from the requester.
    fn on_message(
        &self,
        _: Self::In,
        _: Sender<Self::Out>,
    ) -> impl Future<Output = Result<()>> + Send {
        async { Ok(()) }
    }
}

/// Internal object-safe trait for stream handler storage.
/// Both `StreamRequesterWrapper` and `StreamResponderWrapper` implement this.
pub(crate) trait RawStreamHandler: Send + Sync + 'static {
    fn on_receive_raw(
        &self,
        payload: &[u8],
        raw_sender: Sender<Vec<u8>>,
    ) -> std::pin::Pin<Box<dyn std::future::Future<Output = ()> + Send + '_>>;
}

/// Object-safe factory trait for creating responder instances.
pub(crate) trait RawResponderFactory: Send + Sync + 'static {
    fn create(&self) -> Arc<dyn RawStreamHandler>;
}

/// Typed wrapper for responder factories.
struct ResponderFactory<R, F>
where
    R: StreamResponder + 'static,
    R::Out: Send + 'static,
    F: Fn() -> R + Send + Sync + 'static,
{
    factory: F,
    _marker: std::marker::PhantomData<R>,
}

impl<R, F> RawResponderFactory for ResponderFactory<R, F>
where
    R: StreamResponder + 'static,
    R::Out: Send + 'static,
    F: Fn() -> R + Send + Sync + 'static,
{
    fn create(&self) -> Arc<dyn RawStreamHandler> {
        Arc::new(StreamResponderWrapper::new((self.factory)()))
    }
}

/// Registry for managing active streams on a connection.
///
/// This is transport-agnostic and works with `StreamMessage` payloads.
pub struct StreamRegistry {
    streams: RwLock<HashMap<StreamId, (Arc<dyn RawStreamHandler>, Sender<Vec<u8>>)>>,
    /// Factories for creating responders, keyed by type tag.
    responder_factories: RwLock<HashMap<u32, Box<dyn RawResponderFactory>>>,
    /// Sender for outgoing stream messages.
    tx: Sender<StreamMessage>,
    /// Server-side relay for forwarding streams to other connections. Held as a
    /// `Weak` to avoid a reference cycle (relay -> connections -> registry).
    relay: RwLock<Option<std::sync::Weak<Relay>>>,
}

impl StreamRegistry {
    pub fn new(tx: Sender<StreamMessage>) -> Self {
        Self {
            streams: RwLock::new(HashMap::new()),
            responder_factories: RwLock::new(HashMap::new()),
            tx,
            relay: RwLock::new(None),
        }
    }

    /// Attach a relay so unknown streams can be forwarded to other connections.
    /// Only used on the server.
    pub fn set_relay(&self, relay: std::sync::Weak<Relay>) {
        *self.relay.write().unwrap() = Some(relay);
    }

    /// Send a raw message directly to this connection's peer.
    pub async fn send_raw(&self, message: StreamMessage) {
        let _ = self.tx.send(message).await;
    }

    /// Whether `tx` is this registry's own outbound channel, i.e. messages on it
    /// originate from this connection. Used by the relay to avoid routing a
    /// message back to its sender.
    pub fn is_origin(&self, tx: &Sender<StreamMessage>) -> bool {
        self.tx.same_channel(tx)
    }

    /// Register a responder factory for a given stream type.
    /// When an incoming message arrives for an unknown stream ID,
    /// the factory will be used to create a new responder instance.
    pub fn register_responder<R, F>(&self, factory: F)
    where
        R: StreamResponder + 'static,
        R::Out: Send + 'static,
        F: Fn() -> R + Send + Sync + 'static,
    {
        let tag = R::tag();
        let boxed_factory: Box<dyn RawResponderFactory> = Box::new(ResponderFactory {
            factory,
            _marker: std::marker::PhantomData,
        });
        self.responder_factories
            .write()
            .unwrap()
            .insert(tag, boxed_factory);
    }

    /// Handle an incoming `StreamMessage` by dispatching to the appropriate handler.
    /// If no handler exists for the stream ID, attempt to create one using a
    /// registered responder factory.
    pub async fn dispatch(&self, message: StreamMessage) {
        let handler_opt = {
            let streams = self.streams.read().unwrap();
            streams
                .get(&message.stream_id)
                .map(|(handler, response_tx)| (handler.clone(), response_tx.clone()))
        };

        if let Some((handler, response_tx)) = handler_opt {
            handler.on_receive_raw(&message.payload, response_tx).await;
            return;
        }

        // No local handler. On a server, try to relay to another connection.
        let relay = self.relay.read().unwrap().clone();
        if let Some(relay) = relay.and_then(|r| r.upgrade()) {
            if relay.route(&message, &self.tx).await {
                return;
            }
        }

        // Otherwise create a responder from a registered factory.
        let type_tag = message.stream_id.tag();
        let factory_opt = {
            let factories = self.responder_factories.read().unwrap();
            factories.get(&type_tag).map(|f| f.create())
        };

        if let Some(handler) = factory_opt {
            // Create channel for response messages from the handler
            let (response_tx, mut response_rx) = tokio::sync::mpsc::channel::<Vec<u8>>(32);

            // Register the new responder
            self.streams
                .write()
                .unwrap()
                .insert(message.stream_id, (handler.clone(), response_tx.clone()));

            // Spawn task to forward response bytes as StreamMessages. Responses
            // carry no `dst`: they travel back to whoever opened the stream
            // (either the direct peer, or via the relay's routing table).
            let tx = self.tx.clone();
            let stream_id = message.stream_id;
            tokio::spawn(async move {
                while let Some(payload) = response_rx.recv().await {
                    let msg = StreamMessage {
                        stream_id,
                        payload,
                        dst: None,
                    };
                    if tx.send(msg).await.is_err() {
                        break;
                    }
                }
            });

            // Dispatch the message to the newly created handler
            handler.on_receive_raw(&message.payload, response_tx).await;
        }
    }

    /// Register a stream handler and return a sender for outbound messages.
    pub fn register<S: StreamRequester>(&self, handler: S) -> (StreamId, Sender<StreamMessage>)
    where
        S::Out: Send + 'static,
    {
        self.register_to(handler, None)
    }

    /// Like [`register`](Self::register) but stamps `dst` on every outbound
    /// message for the stream, so a server relays it to the target instance.
    pub fn register_to<S: StreamRequester>(
        &self,
        handler: S,
        dst: Option<InstanceId>,
    ) -> (StreamId, Sender<StreamMessage>)
    where
        S::Out: Send + 'static,
    {
        let id = S::generate_id();

        // Create channel for response messages from the handler
        let (response_tx, mut response_rx) = tokio::sync::mpsc::channel::<Vec<u8>>(32);

        // Wrap the typed handler in the object-safe wrapper
        let wrapped: Arc<dyn RawStreamHandler> = Arc::new(StreamRequesterWrapper::new(handler));
        self.streams
            .write()
            .unwrap()
            .insert(id, (wrapped, response_tx));

        // Spawn task to forward the handler's outgoing bytes as StreamMessages.
        let tx = self.tx.clone();
        let stream_id = id;
        tokio::spawn(async move {
            while let Some(payload) = response_rx.recv().await {
                let msg = StreamMessage {
                    stream_id,
                    payload,
                    dst,
                };
                if tx.send(msg).await.is_err() {
                    break;
                }
            }
        });

        // Create channel for outgoing request messages from the caller. These
        // are pre-built `StreamMessage`s (the caller sets `dst`).
        let tx2 = self.tx.clone();
        let (msg_tx, mut msg_rx) = tokio::sync::mpsc::channel::<StreamMessage>(32);
        tokio::spawn(async move {
            while let Some(msg) = msg_rx.recv().await {
                if tx2.send(msg).await.is_err() {
                    break;
                }
            }
        });

        (id, msg_tx)
    }

    /// Remove a stream from the registry and drop any relay route for it.
    pub fn close(&self, stream_id: StreamId) {
        self.streams.write().unwrap().remove(&stream_id);
        if let Some(relay) = self.relay.read().unwrap().clone().and_then(|r| r.upgrade()) {
            relay.routes.lock().unwrap().remove(&stream_id);
        }
    }
}

/// Server-side stream router. Forwards messages between a client connection and a
/// target agent connection, keyed by stream id.
pub struct Relay {
    /// All connections the server holds (shared with `NetworkLayer::inbound`).
    connections: Arc<RwLock<Vec<Arc<super::InstanceConnection>>>>,
    /// stream id -> the origin connection's outbound sender (for responses).
    routes: Mutex<HashMap<StreamId, Sender<StreamMessage>>>,
}

impl Relay {
    pub fn new(connections: Arc<RwLock<Vec<Arc<super::InstanceConnection>>>>) -> Self {
        Self {
            connections,
            routes: Mutex::new(HashMap::new()),
        }
    }

    /// Find a connection to `target`, skipping the origin connection. In an
    /// all-in-one build the local client and agent share one `InstanceId`, so
    /// both inbound connections match `target`; excluding the origin (the
    /// sender) routes to the *other* one, never back to the sender.
    fn find(
        &self,
        target: InstanceId,
        origin_tx: &Sender<StreamMessage>,
    ) -> Option<Arc<super::InstanceConnection>> {
        self.connections
            .read()
            .unwrap()
            .iter()
            .find(|c| c.data.read().remote_instance == target && !c.streams.is_origin(origin_tx))
            .cloned()
    }

    /// Attempt to route an unhandled message. Returns `true` if it was forwarded
    /// (and should not be handled locally).
    async fn route(&self, message: &StreamMessage, origin_tx: &Sender<StreamMessage>) -> bool {
        // Client -> agent: an addressed message. Remember the origin so responses
        // can return, then forward to the target connection.
        if let Some(target) = message.dst {
            let Some(conn) = self.find(target, origin_tx) else {
                // Unknown target: swallow it rather than mis-handling locally.
                tracing::warn!(
                    target = %target,
                    stream_id = ?message.stream_id,
                    "No connection for relay target; dropping stream message"
                );
                return true;
            };
            self.routes
                .lock()
                .unwrap()
                .insert(message.stream_id, origin_tx.clone());
            conn.streams
                .send_raw(StreamMessage {
                    stream_id: message.stream_id,
                    payload: message.payload.clone(),
                    dst: Some(target),
                })
                .await;
            return true;
        }

        // Agent -> client: a response on a relayed stream goes back to its origin.
        let origin = self.routes.lock().unwrap().get(&message.stream_id).cloned();
        if let Some(origin) = origin {
            let _ = origin
                .send(StreamMessage {
                    stream_id: message.stream_id,
                    payload: message.payload.clone(),
                    dst: None,
                })
                .await;
            return true;
        }

        false
    }
}

/// Wrapper that adapts a typed `StreamRequester` to the object-safe `RawStreamHandler`.
pub(crate) struct StreamRequesterWrapper<T: StreamRequester> {
    handler: T,
}

impl<T: StreamRequester> StreamRequesterWrapper<T> {
    pub fn new(handler: T) -> Self {
        Self { handler }
    }
}

impl<T: StreamRequester> RawStreamHandler for StreamRequesterWrapper<T>
where
    T::In: Send,
    T::Out: Send + 'static,
{
    fn on_receive_raw(
        &self,
        payload: &[u8],
        raw_sender: Sender<Vec<u8>>,
    ) -> std::pin::Pin<Box<dyn std::future::Future<Output = ()> + Send + '_>> {
        let msg = match serde_cbor::from_slice::<T::In>(payload) {
            Ok(msg) => msg,
            Err(_) => return Box::pin(async {}),
        };

        // Create a typed sender that serializes to raw bytes
        let (typed_tx, mut typed_rx) = tokio::sync::mpsc::channel::<T::Out>(32);

        // Spawn task to forward typed messages to raw sender
        tokio::spawn(async move {
            while let Some(typed_msg) = typed_rx.recv().await {
                if let Ok(bytes) = serde_cbor::to_vec(&typed_msg) {
                    if raw_sender.send(bytes).await.is_err() {
                        break;
                    }
                }
            }
        });

        Box::pin(async move {
            let _ = self.handler.on_message(msg, typed_tx).await;
        })
    }
}

/// Wrapper that adapts a typed `StreamResponder` to the object-safe `RawStreamHandler`.
pub(crate) struct StreamResponderWrapper<T: StreamResponder> {
    handler: T,
}

impl<T: StreamResponder> StreamResponderWrapper<T> {
    pub fn new(handler: T) -> Self {
        Self { handler }
    }
}

impl<T: StreamResponder> RawStreamHandler for StreamResponderWrapper<T>
where
    T::In: Send,
    T::Out: Send + 'static,
{
    fn on_receive_raw(
        &self,
        payload: &[u8],
        raw_sender: Sender<Vec<u8>>,
    ) -> std::pin::Pin<Box<dyn std::future::Future<Output = ()> + Send + '_>> {
        let msg = match serde_cbor::from_slice::<T::In>(payload) {
            Ok(msg) => msg,
            Err(_) => return Box::pin(async {}),
        };

        // Create a typed sender that serializes to raw bytes
        let (typed_tx, mut typed_rx) = tokio::sync::mpsc::channel::<T::Out>(32);

        // Spawn task to forward typed messages to raw sender
        tokio::spawn(async move {
            while let Some(typed_msg) = typed_rx.recv().await {
                if let Ok(bytes) = serde_cbor::to_vec(&typed_msg) {
                    if raw_sender.send(bytes).await.is_err() {
                        break;
                    }
                }
            }
        });

        Box::pin(async move {
            let _ = self.handler.on_message(msg, typed_tx).await;
        })
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::sync::Arc;
    use std::sync::atomic::{AtomicUsize, Ordering};

    #[derive(Serialize, Deserialize)]
    struct TestMessage {
        value: usize,
    }

    #[derive(Serialize, Deserialize)]
    struct TestResponse;

    #[derive(Serialize, Deserialize)]
    struct TestRequest;

    struct TestStreamResponder {
        received_count: Arc<AtomicUsize>,
    }

    impl Stream for TestStreamResponder {
        fn tag() -> u32 {
            0x12345678
        }
    }

    impl StreamResponder for TestStreamResponder {
        type In = TestMessage;
        type Out = TestResponse;

        async fn on_message(&self, message: Self::In, _sender: Sender<Self::Out>) -> Result<()> {
            self.received_count
                .fetch_add(message.value, Ordering::SeqCst);
            Ok(())
        }
    }

    #[tokio::test]
    async fn test_stream_responder_receives_messages() {
        let received_count = Arc::new(AtomicUsize::new(0));
        let stream = TestStreamResponder {
            received_count: received_count.clone(),
        };
        let wrapper = StreamResponderWrapper::new(stream);

        let (tx, _rx) = tokio::sync::mpsc::channel::<Vec<u8>>(32);
        let payload = serde_cbor::to_vec(&TestMessage { value: 42 }).unwrap();
        wrapper.on_receive_raw(&payload, tx.clone()).await;
        wrapper.on_receive_raw(&payload, tx).await;

        // Give time for async processing
        tokio::time::sleep(tokio::time::Duration::from_millis(10)).await;
        assert_eq!(received_count.load(Ordering::SeqCst), 84);
    }
}

#[cfg(test)]
mod relay_tests {
    use super::*;
    use crate::network::{ConnectionData, InstanceConnection};
    use crate::realm::RealmName;
    use crate::{ClusterId, InstanceId, InstanceType, test_db};
    use sandpolis_macros::Stream;
    use tokio::sync::mpsc;
    use tokio::time::{Duration, timeout};
    use tokio_util::sync::CancellationToken;

    #[derive(Serialize, Deserialize)]
    struct RelayPing(u64);
    #[derive(Serialize, Deserialize)]
    struct RelayPong(u64);

    #[derive(Stream, Default)]
    struct RelayEchoResponder;
    impl StreamResponder for RelayEchoResponder {
        type In = RelayPing;
        type Out = RelayPong;
        async fn on_message(&self, ping: Self::In, sender: Sender<Self::Out>) -> Result<()> {
            sender.send(RelayPong(ping.0 * 2)).await?;
            Ok(())
        }
    }

    #[derive(Stream)]
    struct RelayEchoRequester {
        result: mpsc::Sender<u64>,
    }
    impl StreamRequester for RelayEchoRequester {
        type In = RelayPong;
        type Out = RelayPing;
        async fn new(_: Self::Out, _: Sender<Self::Out>) -> Result<Self> {
            anyhow::bail!("constructed directly")
        }
        async fn on_message(&self, pong: Self::In, _: Sender<Self::Out>) -> Result<()> {
            let _ = self.result.send(pong.0).await;
            Ok(())
        }
    }

    fn pump(mut rx: mpsc::Receiver<StreamMessage>, dst: Arc<StreamRegistry>) {
        tokio::spawn(async move {
            while let Some(msg) = rx.recv().await {
                dst.dispatch(msg).await;
            }
        });
    }

    /// Wire a client and an agent to a relaying server and run one echo
    /// round-trip. The ids may be equal (as in an all-in-one build, where the
    /// co-located client and agent share one `InstanceId`).
    async fn relay_echo_roundtrip(agent_id: InstanceId, client_id: InstanceId) -> anyhow::Result<()> {
        let db = test_db!(ConnectionData);
        let realm = db.realm(RealmName::default())?;
        let conns = realm.resident_vec::<ConnectionData>(())?;

        // Channels named by the dispatch they feed.
        let (c2s_tx, c2s_rx) = mpsc::channel(32); // client -> server (client-facing)
        let (s2c_tx, s2c_rx) = mpsc::channel(32); // server -> client
        let (s2a_tx, s2a_rx) = mpsc::channel(32); // server -> agent
        let (a2s_tx, a2s_rx) = mpsc::channel(32); // agent -> server (agent-facing)

        let client_reg = Arc::new(StreamRegistry::new(c2s_tx));
        let server_client_reg = Arc::new(StreamRegistry::new(s2c_tx));
        let server_agent_reg = Arc::new(StreamRegistry::new(s2a_tx));
        let agent_reg = Arc::new(StreamRegistry::new(a2s_tx));

        agent_reg.register_responder(RelayEchoResponder::default);

        // Server connections (so the relay can find the agent by instance id).
        let mut agent_data = ConnectionData::default();
        agent_data.remote_instance = agent_id;
        let mut client_data = ConnectionData::default();
        client_data.remote_instance = client_id;
        let agent_conn = Arc::new(InstanceConnection {
            data: conns.push(agent_data)?,
            realm: RealmName::default(),
            cluster_id: ClusterId::default(),
            cancel: CancellationToken::new(),
            streams: server_agent_reg.clone(),
        });
        let client_conn = Arc::new(InstanceConnection {
            data: conns.push(client_data)?,
            realm: RealmName::default(),
            cluster_id: ClusterId::default(),
            cancel: CancellationToken::new(),
            streams: server_client_reg.clone(),
        });

        let connections = Arc::new(RwLock::new(vec![agent_conn, client_conn]));
        let relay = Arc::new(Relay::new(connections));
        server_client_reg.set_relay(Arc::downgrade(&relay));
        server_agent_reg.set_relay(Arc::downgrade(&relay));

        pump(c2s_rx, server_client_reg.clone());
        pump(s2c_rx, client_reg.clone());
        pump(s2a_rx, agent_reg.clone());
        pump(a2s_rx, server_agent_reg.clone());

        // Client opens a stream addressed to the agent.
        let (result_tx, mut result_rx) = mpsc::channel(8);
        let (id, tx) = client_reg.register_to(
            RelayEchoRequester { result: result_tx },
            Some(agent_id),
        );
        tx.send(StreamMessage {
            stream_id: id,
            payload: serde_cbor::to_vec(&RelayPing(21))?,
            dst: Some(agent_id),
        })
        .await?;

        let got = timeout(Duration::from_secs(2), result_rx.recv())
            .await?
            .expect("relayed response");
        assert_eq!(got, 42);

        Ok(())
    }

    /// A client opens a stream addressed to an agent; the server relays the
    /// request to the agent and the agent's response back to the client.
    #[tokio::test]
    async fn relays_client_to_agent_and_back() -> anyhow::Result<()> {
        relay_echo_roundtrip(
            InstanceId::new(&[InstanceType::Agent]),
            InstanceId::new(&[InstanceType::Client]),
        )
        .await
    }

    /// Same round-trip when the client and agent share one `InstanceId`, as in
    /// an all-in-one build. The relay must exclude the origin connection and
    /// route to the other one.
    #[tokio::test]
    async fn relays_with_shared_instance_id() -> anyhow::Result<()> {
        let shared = InstanceId::new(&[InstanceType::Client, InstanceType::Agent]);
        relay_echo_roundtrip(shared, shared).await
    }
}
