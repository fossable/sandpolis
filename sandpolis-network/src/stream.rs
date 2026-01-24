//! Streams provide ephemeral data transfer over time. Operations like file transfers,
//! remote desktop sessions, and shell prompt sessions all run over streams.
//!
//! A stream has two endpoints: a `StreamRequester` and a `StreamResponder`. The
//! `StreamRequester` is responsible for starting the stream and it sends "requests"
//! (one or more than one). The `StreamResponder` is created as a result of the
//! `StreamRequester`'s first request and sends "responses" (one or more than one).

use anyhow::Result;
use serde::{Deserialize, Serialize};
use std::future::Future;
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
pub trait StreamResponder: StreamHandler {}

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
