use actix_session::Session;
use crate::banner::Banner;
use crate::connection::ServerConnection;
use crate::database::DbConnection;
use crate::group::Group;
use crate::server::Server;
use crate::user::User;
use actix_session::CookieSession;
use actix_web::web::Data;
use actix_web::{middleware, App, HttpServer};
use rustls::server::AllowAnyAuthenticatedClient;
use rustls::{RootCertStore, ServerConfig};
use std::env;
use std::io;
use std::sync::RwLock;
use tokio::time::{sleep, Duration};
use anyhow::bail;

pub mod api {
    pub mod v1 {
        pub mod agent {
            pub mod reboot;
        }
        pub mod server {
            pub mod banner;
            pub mod session;
            pub mod users;
        }
        pub mod util;
    }
}
pub mod banner;
pub mod connection;
pub mod database;
pub mod group;
pub mod server;
pub mod user;

struct ServerContext {
    db: DbConnection,
    iid: String,
    users: RwLock<Vec<User>>,
    connections: RwLock<Vec<ServerConnection>>,
    servers: RwLock<Vec<Server>>,
    groups: RwLock<Vec<Group>>,
    banner: RwLock<Banner>,
}

impl ServerContext {

    pub fn verify_session(&self, session: Session) -> anyhow::Result<User> {
        if let Some(cookie) = session.get::<String>("session")? {

            // Search for a user with the session cookie
            self
            .users
            .read()
            .unwrap()
            .iter()
            .find(|user| user.sessions.contains(&cookie))
        } else {
            bail!("")
        }
    }
}

#[actix_web::main]
async fn main() -> io::Result<()> {
    let db_url = match env::var("S7S_DB_URL") {
        Ok(value) => {
            // Clear variable
            env::set_var("S7S_DB_URL", "");
            value
        }
        Err(_) => String::from("http://127.0.0.1:5984"),
    };

    let db_username = match env::var("S7S_DB_USERNAME") {
        Ok(value) => {
            // Clear variable
            env::set_var("S7S_DB_USERNAME", "");
            value
        }
        Err(_) => String::from("admin"),
    };

    let db_password = match env::var("S7S_DB_PASSWORD") {
        Ok(value) => {
            // Clear variable
            env::set_var("S7S_DB_PASSWORD", "");
            value
        }
        Err(_) => String::from("admin"),
    };

    // Configure logging
    env_logger::init_from_env(env_logger::Env::new().default_filter_or("info"));

    // Attempt database connection until we succeed
    let db = loop {
        match DbConnection::new(&db_url, &db_username, &db_password).await {
            Ok(connection) => {
                break connection;
            }
            Err(_) => {
                sleep(Duration::from_millis(1000)).await;
            }
        }
    };
    db.setup_db().await;

    // Attempt to load context until we succeed. Just in case the connection goes down
    // immediately after it's established.
    let context = loop {
        match db.load_context().await {
            Ok(c) => {
                break Data::new(c);
            }
            Err(_) => {
                sleep(Duration::from_millis(1000)).await;
            }
        }
    };
    let context = Data::new(db.load_context());

    let mut cert_store = RootCertStore::empty();

    let config = ServerConfig::builder()
        .with_safe_defaults()
        .with_client_cert_verifier(AllowAnyAuthenticatedClient::new(cert_store))
        .with_single_cert()
        .unwrap();

    HttpServer::new(move || {
        App::new()
            // enable automatic response compression - usually register this first
            .wrap(middleware::Compress::default())
            // cookie session middleware
            .wrap(CookieSession::signed(&[0; 32]).secure(true))
            // enable logger - always register actix-web Logger middleware last
            .wrap(middleware::Logger::default())
            // register simple route, handle all methods
            .app_data(context.clone())
            // Add API endpoints
            .service(api::v1::agent::reboot::reboot)
            .service(api::v1::server::banner::get_banner)
            .service(api::v1::server::session::delete_session)
            .service(api::v1::server::users::add_user)
            .service(api::v1::server::users::list_users)
    })
    .bind_rustls(("localhost", 8443), config)?
    .workers(2)
    .run()
    .await
}
