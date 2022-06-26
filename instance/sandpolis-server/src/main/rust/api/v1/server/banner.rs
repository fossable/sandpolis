use crate::api::v1::util::rs_body;
use crate::ServerContext;
use actix_web::web;
use actix_web::{HttpResponse, Result};
use core_protocol::core::protocol::GetBannerResponse;

#[actix_web::get("/v1/server/{iid}/banner")]
async fn get_banner(
    context: web::Data<ServerContext>,
    iid: web::Path<String>,
) -> Result<HttpResponse> {
    let banner = *context.banner.read().unwrap();
    let response: GetBannerResponse = banner.into();

    rs_body!(response)
}

#[cfg(test)]
mod tests {
    use super::*;
}
