use native_db::ToKey;
use native_model::Model;
use sandpolis_macros::data;

pub mod routes;

#[data]
pub struct ServerBannerData {
    inner: ServerBanner,
}
