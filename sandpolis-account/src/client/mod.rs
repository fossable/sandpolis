//! Client-side access to account data.
//!
//! Accounts and links are replicated from the server by the sync engine, so the
//! GUI reads them synchronously out of the client's local database.

use crate::favicon::FaviconData;
use crate::{AccountData, AccountLinkData};
use native_model::Model;
use sandpolis_instance::realm::RealmName;

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
    sandpolis_client::sync::subscribe(account_model_id(), None);
    sandpolis_client::sync::subscribe(link_model_id(), None);
    sandpolis_client::sync::subscribe(favicon_model_id(), None);
}

/// Drop the subscription created by [`subscribe`].
pub fn unsubscribe() {
    sandpolis_client::sync::unsubscribe(account_model_id(), None);
    sandpolis_client::sync::unsubscribe(link_model_id(), None);
    sandpolis_client::sync::unsubscribe(favicon_model_id(), None);
}

/// Every account in the client's local database.
pub fn query_accounts() -> anyhow::Result<Vec<AccountData>> {
    scan()
}

/// Every account link in the client's local database.
pub fn query_links() -> anyhow::Result<Vec<AccountLinkData>> {
    scan()
}

/// Every domain favicon in the client's local database.
pub fn query_favicons() -> anyhow::Result<Vec<FaviconData>> {
    scan()
}

fn scan<T>() -> anyhow::Result<Vec<T>>
where
    T: sandpolis_instance::database::Data + Model + 'static,
{
    let Some(database) = sandpolis_client::sync::client_database() else {
        return Ok(vec![]);
    };
    let realm = database.realm(RealmName::default())?;
    let r = realm.r_transaction()?;
    Ok(r.scan()
        .primary::<T>()?
        .all()?
        .collect::<std::result::Result<Vec<_>, _>>()?)
}
