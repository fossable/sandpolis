//! Domains group instances (and accounts) into regions of the world view.
//!
//! Membership is an explicit assignment rather than something an instance knows
//! about itself: an instance no domain names belongs to none, which is the
//! default. The name shares a namespace with account domains
//! (`AccountData.domain`), so an instance and the accounts on the same service
//! land in one region.

use crate::InstanceId;
use native_db::*;
use native_model::Model;
use sandpolis_macros::data;

/// A named grouping of instances in the world view.
///
/// Estate-wide data, so the global stratum server owns it and it replicates down
/// to local stratum servers and clients the way users and accounts do.
#[data]
#[derive(Default)]
pub struct DomainData {
    /// The domain's identifier, for example "github.com". Stored as it was
    /// typed and normalized wherever it's matched, matching how accounts store
    /// theirs.
    #[secondary_key(unique)]
    pub name: String,

    /// The instances in this domain. Only ids passing
    /// [`InstanceId::is_domain_member`] belong here: domains group the estate,
    /// not the servers running it.
    pub members: Vec<InstanceId>,
}

inventory::submit! {
    crate::database::sync::SyncRegistration(|r| r.register::<DomainData>())
}

/// The sync `model_id` for [`DomainData`], used by clients to subscribe to the
/// set of domains.
pub fn domain_model_id() -> u32 {
    <DomainData as Model>::native_model_id()
}
