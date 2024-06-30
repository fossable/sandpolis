use crate::api::v1::util::rs_body;
use crate::user::User;
use crate::ServerContext;
use actix_session::Session;
use actix_web::web;
use actix_web::{http::StatusCode, HttpRequest, HttpResponse, Result};
use core_protocol::core::protocol::{GetUserResponse, GetUsersResponse, PostUserRequest};
use prost::Message;

#[actix_web::post("/v1/server/{iid}/users")]
async fn add_user(
    session: Session,
    body: web::Bytes,
    context: web::Data<ServerContext>,
) -> Result<HttpResponse> {
    match PostUserRequest::decode(body) {
        Err(_) => Ok(HttpResponse::new(StatusCode::BAD_REQUEST)),
        Ok(rq) => {
            let users = context.users.read().unwrap();

            if let Some(_) = users.iter().find(|&user| user.username == rq.username) {
                Ok(HttpResponse::new(StatusCode::BAD_REQUEST))
            } else {
                let mut user = User::default();
                user.username = rq.username;
                Ok(HttpResponse::new(StatusCode::OK))
            }
        }
    }
}

#[actix_web::get("/v1/server/{iid}/users")]
async fn list_users(
    rq: HttpRequest,
    body: web::Bytes,
    session: Session,
    context: web::Data<ServerContext>,
    iid: web::Path<String>,
) -> Result<HttpResponse> {
    match context.get_ref().verify_session(session) {
        Err(_) => Ok(HttpResponse::new(StatusCode::UNAUTHORIZED)),
        Ok(_) => {
            if *iid != context.iid {
                Ok(HttpResponse::new(StatusCode::UNAUTHORIZED))
            } else {
                rs_body!(GetUsersResponse {
                    user: context
                        .users
                        .read()
                        .unwrap()
                        .iter()
                        .map(|user| GetUserResponse::from(user.clone()))
                        .collect(),
                })
            }
        }
    }
}
