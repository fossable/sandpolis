//! The `SyncStream`: database replication expressed as a stream.
//!
//! A [`SyncRequester`] is created by the side that *wants* data; it specifies
//! [`SyncFilter`]s for what it cares about and applies whatever records arrive to
//! its local database. A [`SyncResponder`] is created automatically on the side
//! that *has* the data; it answers with a snapshot of the matching records and
//! then streams live changes until the requester sends [`SyncRequest::Close`].
//!
//! - Agent ↔ server: the server opens one long-lived requester filtered to
//!   everything, so the agent streams its whole database.
//! - Client ↔ server: the client opens short-lived requesters for exactly what
//!   the UI is showing.

use super::stream::{Stream, StreamId, StreamMessage, StreamRegistry, StreamRequester, StreamResponder};
use super::{InstanceConnection, RegisterResponders};
use crate::database::RealmDatabase;
use crate::database::sync::{SYNC, SyncFilter, SyncRecord};
use anyhow::Result;
use sandpolis_macros::Stream;
use serde::{Deserialize, Serialize};
use tokio::sync::mpsc::{Sender, channel};
use tokio_util::sync::CancellationToken;

/// Requests sent by a [`SyncRequester`] to a [`SyncResponder`].
#[derive(Serialize, Deserialize, Debug)]
pub enum SyncRequest {
    /// Begin syncing the data matching these filters (snapshot + live updates).
    Subscribe { filters: Vec<SyncFilter> },
    /// Stop syncing and tear down the responder's watches.
    Close,
}

/// A batch of records sent by a [`SyncResponder`] to a [`SyncRequester`].
#[derive(Serialize, Deserialize, Debug)]
pub struct SyncUpdate {
    pub records: Vec<SyncRecord>,
}

/// Wants data: applies received records into its local database.
#[derive(Stream)]
pub struct SyncRequester {
    db: RealmDatabase,
}

impl SyncRequester {
    pub fn new(db: RealmDatabase) -> Self {
        Self { db }
    }
}

impl StreamRequester for SyncRequester {
    type In = SyncUpdate;
    type Out = SyncRequest;

    async fn new(_: Self::Out, _: Sender<Self::Out>) -> Result<Self> {
        // SyncRequester is always constructed directly via `InstanceConnection::open_sync`
        // (the registry's `register` path does not call this).
        anyhow::bail!("SyncRequester must be constructed directly")
    }

    async fn on_message(&self, update: Self::In, _: Sender<Self::Out>) -> Result<()> {
        for record in &update.records {
            if let Err(e) = SYNC.apply(&self.db, record) {
                tracing::debug!(error = %e, model = record.model_id, "Failed to apply sync record");
            }
        }
        Ok(())
    }
}

/// Has data: serves a snapshot then streams live changes matching the filters.
#[derive(Stream)]
pub struct SyncResponder {
    db: RealmDatabase,
    cancel: CancellationToken,
}

impl StreamResponder for SyncResponder {
    type In = SyncRequest;
    type Out = SyncUpdate;

    async fn on_message(&self, request: Self::In, sender: Sender<Self::Out>) -> Result<()> {
        match request {
            SyncRequest::Subscribe { filters } => {
                for filter in filters {
                    // Snapshot of currently matching records.
                    let records = SYNC.snapshot(&self.db, &filter)?;
                    if !records.is_empty() {
                        sender.send(SyncUpdate { records }).await?;
                    }

                    // Live updates: watch tasks feed records which we forward as
                    // single-record updates until the stream is closed.
                    let (record_tx, mut record_rx) = channel::<SyncRecord>(64);
                    SYNC.spawn_watch(&self.db, &filter, record_tx, self.cancel.clone())?;

                    let sender = sender.clone();
                    let cancel = self.cancel.clone();
                    tokio::spawn(async move {
                        loop {
                            tokio::select! {
                                _ = cancel.cancelled() => break,
                                record = record_rx.recv() => match record {
                                    Some(record) => {
                                        if sender
                                            .send(SyncUpdate { records: vec![record] })
                                            .await
                                            .is_err()
                                        {
                                            break;
                                        }
                                    }
                                    None => break,
                                }
                            }
                        }
                    });
                }
            }
            SyncRequest::Close => {
                self.cancel.cancel();
            }
        }
        Ok(())
    }
}

impl Drop for SyncResponder {
    fn drop(&mut self) {
        self.cancel.cancel();
    }
}

/// Registers a [`SyncResponder`] factory bound to a particular realm database.
///
/// Unlike the inventory-collected responders, this one is stateful (it carries
/// the local database) so it is passed explicitly into the connection's handler
/// list at setup time.
pub struct SyncResponderRegistration {
    db: RealmDatabase,
}

impl SyncResponderRegistration {
    pub fn new(db: RealmDatabase) -> Self {
        Self { db }
    }
}

impl RegisterResponders for SyncResponderRegistration {
    fn register_responders(&self, registry: &StreamRegistry) {
        let db = self.db.clone();
        registry.register_responder(move || SyncResponder {
            db: db.clone(),
            cancel: CancellationToken::new(),
        });
    }
}

impl InstanceConnection {
    /// Open a [`SyncRequester`] stream that applies matching records into `db`.
    ///
    /// Returns the stream id and the outbound message sender (used to send
    /// [`SyncRequest::Close`] later).
    pub async fn open_sync(
        &self,
        db: RealmDatabase,
        filters: Vec<SyncFilter>,
    ) -> Result<(StreamId, Sender<StreamMessage>)> {
        let (id, tx) = self.streams.register(SyncRequester::new(db));
        let payload = serde_cbor::to_vec(&SyncRequest::Subscribe { filters })?;
        tx.send(StreamMessage {
            stream_id: id,
            payload,
            dst: None,
        })
        .await?;
        Ok((id, tx))
    }

    /// Close a previously opened sync stream.
    pub async fn close_sync(&self, id: StreamId, tx: &Sender<StreamMessage>) -> Result<()> {
        let payload = serde_cbor::to_vec(&SyncRequest::Close)?;
        let _ = tx
            .send(StreamMessage {
                stream_id: id,
                payload,
                dst: None,
            })
            .await;
        self.close_stream(id);
        Ok(())
    }
}
