use actix_web::{
    http::{header::ContentType, StatusCode},
    web, HttpResponse, Result,
};

#[actix_web::post("/v1/agent/{iid}/reboot")]
async fn reboot(iid: web::Path<String>) -> Result<HttpResponse> {
    Ok(HttpResponse::build(StatusCode::OK)
        .content_type(ContentType::octet_stream())
        .body(vec![]))
}
