//! Client-side access to account data.
//!
//! Accounts and links are replicated from the server by the sync engine, so the
//! GUI reads them synchronously out of the client's local database.

use crate::favicon::FaviconData;
use crate::{AccountData, AccountLinkData};
use native_model::Model;

pub mod gui;

/// The sync model id for accounts.
pub fn account_model_id() -> u32 {
    <AccountData as Model>::native_model_id()
}

/// The sync model id for account links.
pub fn link_model_id() -> u32 {
    <AccountLinkData as Model>::native_model_id()
}

/// The sync model id for domain favicons.
pub fn favicon_model_id() -> u32 {
    <FaviconData as Model>::native_model_id()
}

/// Subscribe to live account updates. Accounts aren't scoped to an instance, so
/// the subscription covers every account the server knows about.
pub fn subscribe() {
    sandpolis_client::sync::subscribe_all(account_model_ids(), None);
}

/// Drop the subscription created by [`subscribe`].
pub fn unsubscribe() {
    sandpolis_client::sync::unsubscribe_all(account_model_ids(), None);
}

fn account_model_ids() -> [u32; 3] {
    [account_model_id(), link_model_id(), favicon_model_id()]
}

/// Every account in the client's local database.
pub fn query_accounts() -> anyhow::Result<Vec<AccountData>> {
    sandpolis_client::sync::scan_all()
}

/// Every account link in the client's local database.
pub fn query_links() -> anyhow::Result<Vec<AccountLinkData>> {
    sandpolis_client::sync::scan_all()
}

/// Every domain favicon in the client's local database.
pub fn query_favicons() -> anyhow::Result<Vec<FaviconData>> {
    sandpolis_client::sync::scan_all()
}
