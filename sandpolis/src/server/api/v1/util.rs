/*async fn check_session(session: Session, db: &DbConnection) -> Result<()> {
    Ok(())
}*/

macro_rules! check_session {
    ($session:expr, $users:expr) => {};
}

macro_rules! rs_body {
    ($value:expr) => {
        Ok(
            actix_web::HttpResponse::build(actix_web::http::StatusCode::OK)
                .content_type(actix_web::http::header::ContentType::octet_stream())
                .body($value.encode_to_vec()),
        )
    };
}

pub(crate) use rs_body;

/// Forward a request to another instance.
pub async fn forward(iid: &str) -> Result<HttpResponse> {
    if *iid != context.iid {
        let connections = context.connections.read().unwrap();

        if let Some(connection) = connections
            .iter()
            .find(|&connection| connection.iid == *iid)
        {
            return connection.redirect(&rq).await;
        }
    }
}
