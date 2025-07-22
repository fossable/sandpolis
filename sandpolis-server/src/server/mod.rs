use crate::ServerBanner;
use native_db::ToKey;
use native_model::Model;
use sandpolis_macros::data;

pub mod routes;

#[data]
#[derive(Default)]
pub struct ServerBannerData {
    inner: ServerBanner,
}
