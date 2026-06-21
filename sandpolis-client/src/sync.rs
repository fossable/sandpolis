//! Client-side database sync: short-lived subscriptions driven by the UI.
//!
//! The UI calls [`subscribe`] for the data a view shows and [`unsubscribe`] when
//! it goes away. Each subscription opens a `SyncStream` to the server; incoming
//! records are applied to the client's local database by the stream's requester,
//! so the UI can read them synchronously via [`client_database`].

use sandpolis_instance::InstanceId;
use sandpolis_instance::database::DatabaseLayer;
use sandpolis_instance::database::sync::SyncFilter;
use sandpolis_instance::network::InstanceConnection;
use sandpolis_instance::network::stream::{StreamId, StreamMessage};
use sandpolis_instance::realm::RealmName;
use std::collections::HashMap;
use std::sync::{Arc, Mutex, OnceLock};
use tokio::runtime::Handle;
use tokio::sync::mpsc::Sender;

static HANDLE: OnceLock<SyncHandle> = OnceLock::new();
static DATABASE: OnceLock<DatabaseLayer> = OnceLock::new();

/// Install the client's database and websocket connection for sync. Call once
/// after the websocket to the server is established.
pub fn init(connection: Arc<InstanceConnection>, database: DatabaseLayer) {
    let _ = DATABASE.set(database.clone());
    let _ = HANDLE.set(SyncHandle {
        inner: Arc::new(SyncHandleInner {
            connection,
            database,
            subs: Mutex::new(HashMap::new()),
            // Captured here because `init` runs inside the Tokio runtime. UI
            // callers (e.g. Bevy systems) invoke `subscribe`/`unsubscribe` from
            // threads with no runtime context, so we spawn through this handle.
            runtime: Handle::current(),
        }),
    });
}

/// Spawn a future onto the client's Tokio runtime from any thread.
///
/// UI code (Bevy systems, etc.) runs off the runtime's worker threads, so
/// `tokio::spawn` would panic there. This routes through the runtime handle
/// captured in [`init`]. Returns `false` if sync hasn't been initialized yet.
pub fn spawn<F>(future: F) -> bool
where
    F: std::future::Future<Output = ()> + Send + 'static,
{
    if let Some(handle) = HANDLE.get() {
        handle.inner.runtime.spawn(future);
        true
    } else {
        false
    }
}

/// The client's local database, if initialized. UI query functions read from it.
pub fn client_database() -> Option<DatabaseLayer> {
    DATABASE.get().cloned()
}

/// The client's websocket connection to the server, if established. Used to open
/// relayed streams to agents (desktop, shell, filesystem).
pub fn connection() -> Option<Arc<InstanceConnection>> {
    HANDLE.get().map(|h| h.inner.connection.clone())
}

/// Wait until the server connection is established (or `timeout` elapses),
/// returning it. One-shot noninteractive commands use this since the connection
/// is brought up asynchronously after startup.
pub async fn wait_for_connection(
    timeout: std::time::Duration,
) -> Option<Arc<InstanceConnection>> {
    let deadline = tokio::time::Instant::now() + timeout;
    loop {
        if let Some(c) = connection() {
            return Some(c);
        }
        if tokio::time::Instant::now() >= deadline {
            return None;
        }
        tokio::time::sleep(std::time::Duration::from_millis(100)).await;
    }
}

/// Subscribe to a model's records (optionally scoped to one instance). Idempotent
/// per `(model_id, instance)`.
pub fn subscribe(model_id: u32, instance: Option<InstanceId>) {
    if let Some(handle) = HANDLE.get() {
        handle.subscribe(model_id, instance);
    }
}

/// Drop a subscription previously created with [`subscribe`].
pub fn unsubscribe(model_id: u32, instance: Option<InstanceId>) {
    if let Some(handle) = HANDLE.get() {
        handle.unsubscribe(model_id, instance);
    }
}

type SubKey = (u32, Option<InstanceId>);

enum SubState {
    Pending,
    Active {
        id: StreamId,
        tx: Sender<StreamMessage>,
    },
}

#[derive(Clone)]
struct SyncHandle {
    inner: Arc<SyncHandleInner>,
}

struct SyncHandleInner {
    connection: Arc<InstanceConnection>,
    database: DatabaseLayer,
    subs: Mutex<HashMap<SubKey, SubState>>,
    runtime: Handle,
}

impl SyncHandle {
    fn subscribe(&self, model_id: u32, instance: Option<InstanceId>) {
        let key = (model_id, instance);
        {
            let mut subs = self.inner.subs.lock().unwrap();
            if subs.contains_key(&key) {
                return;
            }
            subs.insert(key, SubState::Pending);
        }

        let realm = match self.inner.database.realm(RealmName::default()) {
            Ok(realm) => realm,
            Err(_) => {
                self.inner.subs.lock().unwrap().remove(&key);
                return;
            }
        };
        let this = self.clone();
        self.inner.runtime.spawn(async move {
            let filters = vec![SyncFilter {
                model_id: Some(model_id),
                instance,
            }];
            match this.inner.connection.open_sync(realm, filters).await {
                Ok((id, tx)) => {
                    let mut subs = this.inner.subs.lock().unwrap();
                    // Only keep it if it wasn't unsubscribed while pending.
                    if subs.remove(&key).is_some() {
                        subs.insert(key, SubState::Active { id, tx });
                    } else {
                        this.inner.connection.close_stream(id);
                    }
                }
                Err(e) => {
                    tracing::debug!(error = %e, "Failed to open sync subscription");
                    this.inner.subs.lock().unwrap().remove(&key);
                }
            }
        });
    }

    fn unsubscribe(&self, model_id: u32, instance: Option<InstanceId>) {
        let key = (model_id, instance);
        let state = self.inner.subs.lock().unwrap().remove(&key);
        if let Some(SubState::Active { id, tx }) = state {
            let connection = self.inner.connection.clone();
            self.inner.runtime.spawn(async move {
                let _ = connection.close_sync(id, &tx).await;
            });
        }
    }
}
