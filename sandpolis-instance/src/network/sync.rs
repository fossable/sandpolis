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
//! - Agent ↔ local stratum server: the LS may not write its own replica, so it
//!   opens a [`SyncProxyRequester`] that forwards the agent's records up an
//!   [`IngestRequester`] stream to the global stratum server. The GS applies
//!   them and they return to the LS through its own (instance-scoped)
//!   [`SyncRequester`].

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

/// Wants data on someone else's behalf: forwards it upstream instead of writing
/// it locally.
///
/// A local stratum server uses this in place of [`SyncRequester`] for the agents
/// attached to it. Its database is a read-only replica, so an agent's updates
/// can't be applied there — they are pushed to the global stratum server, which
/// is the only writer, and come back down through the LS's own subscription.
#[derive(Stream)]
pub struct SyncProxyRequester {
    /// The [`IngestRequester`] stream carrying records to the global stratum
    /// server.
    upstream: Sender<StreamMessage>,
    upstream_id: StreamId,
}

impl SyncProxyRequester {
    pub fn new(upstream: Sender<StreamMessage>, upstream_id: StreamId) -> Self {
        Self {
            upstream,
            upstream_id,
        }
    }
}

impl StreamRequester for SyncProxyRequester {
    type In = SyncUpdate;
    type Out = SyncRequest;

    async fn new(_: Self::Out, _: Sender<Self::Out>) -> Result<Self> {
        anyhow::bail!("SyncProxyRequester must be constructed directly")
    }

    async fn on_message(&self, update: Self::In, _: Sender<Self::Out>) -> Result<()> {
        let payload = serde_cbor::to_vec(&IngestRequest::Records(update))?;
        self.upstream
            .send(StreamMessage::local(self.upstream_id, payload))
            .await?;
        Ok(())
    }
}

/// Records travelling *up* the strata, from a local stratum server to the global
/// stratum server.
#[derive(Serialize, Deserialize, Debug)]
pub enum IngestRequest {
    Records(SyncUpdate),
}

/// Acknowledgement of an ingested batch.
#[derive(Serialize, Deserialize, Debug)]
pub struct IngestAck {
    pub applied: usize,
}

/// Pushes records up to the global stratum server. Constructed by a local
/// stratum server on its upstream connection.
#[derive(Stream)]
pub struct IngestRequester;

impl StreamRequester for IngestRequester {
    type In = IngestAck;
    type Out = IngestRequest;

    async fn new(_: Self::Out, _: Sender<Self::Out>) -> Result<Self> {
        anyhow::bail!("IngestRequester must be constructed directly")
    }

    async fn on_message(&self, ack: Self::In, _: Sender<Self::Out>) -> Result<()> {
        tracing::trace!(applied = ack.applied, "Upstream ingest acknowledged");
        Ok(())
    }
}

/// Applies records pushed up from a local stratum server.
///
/// **Register this only on connections whose peer is a server.** It is a write
/// path into the authoritative database, so an agent or client must never be
/// able to open it — they publish data by serving their own [`SyncResponder`],
/// which the server pulls from and can filter.
#[derive(Stream)]
pub struct IngestResponder {
    db: RealmDatabase,
}

impl StreamResponder for IngestResponder {
    type In = IngestRequest;
    type Out = IngestAck;

    async fn on_message(&self, request: Self::In, sender: Sender<Self::Out>) -> Result<()> {
        let IngestRequest::Records(update) = request;

        let mut applied = 0;
        for record in &update.records {
            match SYNC.apply(&self.db, record) {
                Ok(()) => applied += 1,
                Err(e) => {
                    tracing::debug!(error = %e, model = record.model_id, "Failed to ingest record")
                }
            }
        }

        sender.send(IngestAck { applied }).await?;
        Ok(())
    }
}

/// Registers an [`IngestResponder`] bound to a particular realm database.
///
/// Like [`SyncResponderRegistration`] this is stateful, so it is passed
/// explicitly into a connection's handler list — and only for server peers.
pub struct IngestResponderRegistration {
    db: RealmDatabase,
}

impl IngestResponderRegistration {
    pub fn new(db: RealmDatabase) -> Self {
        Self { db }
    }
}

impl RegisterResponders for IngestResponderRegistration {
    fn register_responders(&self, registry: &StreamRegistry) {
        let db = self.db.clone();
        registry.register_responder(move || IngestResponder { db: db.clone() });
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
        tx.send(StreamMessage::local(id, payload))
        .await?;
        Ok((id, tx))
    }

    /// Open an [`IngestRequester`] stream for pushing records to the global
    /// stratum server. Held open by a local stratum server for the life of its
    /// upstream connection.
    pub fn open_ingest(&self) -> (StreamId, Sender<StreamMessage>) {
        self.streams.register(IngestRequester)
    }

    /// Open a [`SyncProxyRequester`] stream: pull everything the peer has, but
    /// forward it to `upstream` rather than writing it locally.
    ///
    /// This is what a local stratum server opens toward each agent attached to
    /// it, since it may not write its own replica.
    pub async fn open_sync_proxy(
        &self,
        upstream: Sender<StreamMessage>,
        upstream_id: StreamId,
        filters: Vec<SyncFilter>,
    ) -> Result<(StreamId, Sender<StreamMessage>)> {
        let (id, tx) = self
            .streams
            .register(SyncProxyRequester::new(upstream, upstream_id));
        let payload = serde_cbor::to_vec(&SyncRequest::Subscribe { filters })?;
        tx.send(StreamMessage::local(id, payload)).await?;
        Ok((id, tx))
    }

    /// Close a previously opened sync stream.
    pub async fn close_sync(&self, id: StreamId, tx: &Sender<StreamMessage>) -> Result<()> {
        let payload = serde_cbor::to_vec(&SyncRequest::Close)?;
        let _ = tx
            .send(StreamMessage::local(id, payload))
            .await;
        self.close_stream(id);
        Ok(())
    }
}
