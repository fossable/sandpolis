use crate::ServerContext;
use actix_session::Session;
use actix_web::web;
use actix_web::{http::StatusCode, HttpResponse, Result};
use core_protocol::core::protocol::PostLoginRequest;
use prost::Message;

#[actix_web::post("/v1/server/{iid}/session")]
async fn new_session(
    session: Session,
    body: web::Bytes,
    context: web::Data<ServerContext>,
) -> Result<HttpResponse> {
    match PostLoginRequest::decode(body) {
        Ok(rq) => {
            // Find user in server context
            if let Some(user) = context
                .users
                .read()
                .unwrap()
                .iter()
                .find(|&user| user.username == rq.username)
            {
                if user.verify_login(&rq.password, rq.token) {
                    Ok(HttpResponse::new(StatusCode::OK))
                } else {
                    Ok(HttpResponse::new(StatusCode::BAD_REQUEST))
                }
            } else {
                Ok(HttpResponse::new(StatusCode::BAD_REQUEST))
            }
        }
        Err(_) => Ok(HttpResponse::new(StatusCode::BAD_REQUEST)),
    }
}

#[actix_web::delete("/v1/server/{iid}/session")]
async fn delete_session(session: Session) -> Result<HttpResponse> {
    Ok(HttpResponse::new(StatusCode::OK))
}
