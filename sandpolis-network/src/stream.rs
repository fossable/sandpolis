//! Streams provide ephemeral data transfer over time. Operations like file transfers,
//! remote desktop sessions, and shell prompt sessions all run over streams.
//!
//! A stream has two endpoints: a `StreamRequester` and a `StreamResponder`. The
//! `StreamRequester` is responsible for starting the stream and it sends "requests"
//! (one or more than one). The `StreamResponder` is created as a result of the
//! `StreamRequester`'s first request and sends "responses" (one or more than one).

use anyhow::Result;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::future::Future;
use std::sync::{Arc, RwLock};
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
}

// TODO "Command"?
/// Implemented by stream types to generate unique IDs.
pub trait StreamRequester: StreamHandler {
    /// Use `#[derive(Stream)]` from `sandpolis_macros` to implement this.
    fn tag() -> u32
    where
        Self: Sized;

    /// Generate a new unique stream ID for this stream type.
    fn generate_id() -> StreamId
    where
        Self: Sized,
    {
        StreamId(((Self::tag() as u64) << 32) | (rand::random::<u32>() as u64))
    }
}

/// Responders are created on the first request message from a requester.
pub trait StreamResponder: StreamHandler {
    /// Use `#[derive(StreamResponder)]` from `sandpolis_macros` to implement this.
    fn tag() -> u32
    where
        Self: Sized;
}

/// Handle messages from the peer endpoint.
pub trait StreamHandler: Send + Sync + 'static {
    /// Input message type. For responders, this is a "request". For requesters,
    /// this is a "response".
    type In: for<'de> Deserialize<'de> + Send;

    /// Output message type. For responders, this is a "response". For requesters,
    /// this is a "request".
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

/// Internal object-safe trait for stream responder storage.
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
        Arc::new(StreamHandlerWrapper::new((self.factory)()))
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
}

impl StreamRegistry {
    pub fn new(tx: Sender<StreamMessage>) -> Self {
        Self {
            streams: RwLock::new(HashMap::new()),
            responder_factories: RwLock::new(HashMap::new()),
            tx,
        }
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
        } else {
            // No existing handler - try to create a responder from factory
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

                // Spawn task to forward response bytes as StreamMessages
                let tx = self.tx.clone();
                let stream_id = message.stream_id;
                tokio::spawn(async move {
                    while let Some(payload) = response_rx.recv().await {
                        let msg = StreamMessage { stream_id, payload };
                        if tx.send(msg).await.is_err() {
                            break;
                        }
                    }
                });

                // Dispatch the message to the newly created handler
                handler.on_receive_raw(&message.payload, response_tx).await;
            }
        }
    }

    /// Register a stream handler and return a sender for outbound messages.
    pub fn register<S: StreamHandler + StreamRequester>(
        &self,
        handler: S,
    ) -> (StreamId, Sender<StreamMessage>)
    where
        S::Out: Send + 'static,
    {
        let id = S::generate_id();

        // Create channel for response messages from the handler
        let (response_tx, mut response_rx) = tokio::sync::mpsc::channel::<Vec<u8>>(32);

        // Wrap the typed handler in the object-safe wrapper
        let wrapped: Arc<dyn RawStreamHandler> = Arc::new(StreamHandlerWrapper::new(handler));
        self.streams
            .write()
            .unwrap()
            .insert(id, (wrapped, response_tx));

        // Spawn task to forward response bytes as StreamMessages
        let tx = self.tx.clone();
        let stream_id = id;
        tokio::spawn(async move {
            while let Some(payload) = response_rx.recv().await {
                let msg = StreamMessage { stream_id, payload };
                if tx.send(msg).await.is_err() {
                    break;
                }
            }
        });

        // Create channel for outgoing request messages from the caller
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

    /// Remove a stream from the registry.
    pub fn close(&self, stream_id: StreamId) {
        self.streams.write().unwrap().remove(&stream_id);
    }
}

/// Wrapper that adapts a typed `StreamHandler` to the object-safe `RawStreamHandler`.
pub(crate) struct StreamHandlerWrapper<T: StreamHandler> {
    handler: T,
}

impl<T: StreamHandler> StreamHandlerWrapper<T> {
    pub fn new(handler: T) -> Self {
        Self { handler }
    }
}

impl<T: StreamHandler> RawStreamHandler for StreamHandlerWrapper<T>
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

    struct TestStream {
        received_count: Arc<AtomicUsize>,
    }

    impl StreamRequester for TestStream {
        fn tag() -> u32 {
            0x12345678
        }
    }

    impl StreamHandler for TestStream {
        type In = TestMessage;
        type Out = TestResponse;

        async fn on_message(&self, message: Self::In, _sender: Sender<Self::Out>) -> Result<()> {
            self.received_count
                .fetch_add(message.value, Ordering::SeqCst);
            Ok(())
        }
    }

    #[test]
    fn test_stream_generates_unique_ids() {
        let id1 = TestStream::generate_id();
        let id2 = TestStream::generate_id();

        assert_eq!(id1.tag(), id2.tag());
        assert_ne!(id1, id2);
    }

    #[tokio::test]
    async fn test_stream_handler_receives_messages() {
        let received_count = Arc::new(AtomicUsize::new(0));
        let stream = TestStream {
            received_count: received_count.clone(),
        };
        let wrapper = StreamHandlerWrapper::new(stream);

        let (tx, _rx) = tokio::sync::mpsc::channel::<Vec<u8>>(32);
        let payload = serde_cbor::to_vec(&TestMessage { value: 42 }).unwrap();
        wrapper.on_receive_raw(&payload, tx.clone()).await;
        wrapper.on_receive_raw(&payload, tx).await;

        // Give time for async processing
        tokio::time::sleep(tokio::time::Duration::from_millis(10)).await;
        assert_eq!(received_count.load(Ordering::SeqCst), 84);
    }
}
