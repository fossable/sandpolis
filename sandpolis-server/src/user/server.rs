use super::{
    ClientAuthToken, CreateUserRequest, CreateUserResponse, GetUsersRequest, GetUsersResponse,
    UserData, UserManager, UserName,
};
use crate::login::LoginPassword;
use anyhow::{Result, anyhow, bail};
use aws_lc_rs::pbkdf2;
use axum::extract::{self, FromRequestParts, State, WebSocketUpgrade};
use axum::http::{StatusCode, request::Parts};
use axum::{Json, RequestPartsExt};
use axum_extra::TypedHeader;
use axum_extra::headers::{Authorization, authorization::Bearer};
use jsonwebtoken::{Validation, decode};
use native_db::ToKey;
use native_model::Model;
use passwords::PasswordGenerator;
use rand::RngExt;
use sandpolis_instance::database::DataScope;
use sandpolis_instance::network::ConnectionData;
use sandpolis_instance::network::InstanceConnection;
use sandpolis_instance::network::RequestResult;
use sandpolis_instance::realm::RealmName;
use sandpolis_macros::data;
use serde::{Deserialize, Serialize};
use std::fmt::{Debug, Display};
use std::num::NonZeroU32;
use totp_rs::{Builder, Secret};
use tracing::info;
use validator::Validate;

const SHA256_OUTPUT_LEN: usize = 32;

/// Create a new user
#[axum_macros::debug_handler]
pub async fn create_user(
    state: State<UserManager>,
    claims: Claims,
    extract::Json(request): extract::Json<CreateUserRequest>,
) -> RequestResult<CreateUserResponse> {
    request
        .validate()
        .map_err(|_| Json(CreateUserResponse::InvalidUser))?;

    // Only admins can create other admins
    if request.data.admin && !claims.admin {
        return Err(Json(CreateUserResponse::Failed));
    }

    // Create new password
    let password = if request.totp {
        state
            .new_password_with_totp(request.data.username.clone(), request.password)
            .await
            .map_err(|_| Json(CreateUserResponse::Failed))?
    } else {
        state
            .new_password(request.data.username.clone(), request.password)
            .await
            .map_err(|_| Json(CreateUserResponse::Failed))?
    };

    // Add new user
    state
        .users
        .push(request.data)
        .map_err(|_| Json(CreateUserResponse::Failed))?;

    Ok(Json(CreateUserResponse::Ok {
        totp_secret: password.totp_secret,
    }))
}

#[axum_macros::debug_handler]
pub async fn get_users(
    state: State<UserManager>,
    claims: Claims,
    extract::Json(request): extract::Json<GetUsersRequest>,
) -> RequestResult<GetUsersResponse> {
    let users: Vec<UserData> = state
        .users
        .iter()
        .map(|user| user.read().clone())
        // A regular user only ever sees their own account; listing the estate's
        // users is an admin capability.
        .filter(|user| claims.admin || user.username == claims.sub)
        .filter(|user| {
            request
                .username
                .as_ref()
                .is_none_or(|prefix| user.username.starts_with(prefix.as_str()))
        })
        .filter(|user| {
            request.email.as_ref().is_none_or(|prefix| {
                user.email
                    .as_ref()
                    .is_some_and(|email| email.starts_with(prefix))
            })
        })
        .collect();

    Ok(Json(GetUsersResponse::Ok(users)))
}

static USER_PASSWORD_HASH_ITERATIONS: NonZeroU32 = NonZeroU32::new(15000).unwrap();

#[data]
#[derive(Default)]
pub struct ServerJwtSecret {
    #[serde(with = "serde_bytes")]
    pub value: [u8; 32],
}

impl ServerJwtSecret {
    pub fn new() -> Self {
        Self {
            value: rand::rng().random::<[u8; 32]>(),
            ..Default::default()
        }
    }
}

#[data(temporal)]
#[derive(Validate, Default)]
pub struct PasswordData {
    /// User that this password belongs to
    #[secondary_key]
    pub user: UserName,

    /// Number of rounds to use when hashing password
    #[validate(range(min = 4284, max = 200000))]
    pub iterations: u32,

    /// Random data used to salt the password hash
    pub salt: Vec<u8>,

    /// Password hash
    pub hash: Vec<u8>,

    /// TOTP secret token
    pub totp_secret: Option<String>,
}

impl UserManager {
    // TODO better users.find
    pub async fn user(&self, username: &UserName) -> Result<UserData> {
        for user in self.users.iter() {
            if user.read().username == *username {
                return Ok(user.read().clone());
            }
        }

        bail!("User not found");
    }

    /// Create an admin user if one doesn't exist already. The password will be
    /// emitted in the server log if created.
    pub async fn try_create_admin(&self) -> Result<()> {
        for user in self.users.iter() {
            if user.read().admin {
                return Ok(());
            }
        }

        self.users.push(UserData {
            username: "admin".parse()?,
            admin: true,
            email: None,
            phone: None,
            expiration: None,
            ..Default::default()
        })?;

        // Generate a default password
        let password = PasswordGenerator::new()
            .length(8)
            .numbers(true)
            .lowercase_letters(true)
            .uppercase_letters(true)
            .symbols(false)
            .spaces(false)
            .exclude_similar_characters(true)
            .strict(false)
            .generate_one()
            .unwrap();

        self.new_password(
            "admin".parse()?,
            LoginPassword::new(self.instance.cluster_id, &password),
        )
        .await?;
        info!(username = "admin", password = %password, "Created default admin user");
        Ok(())
    }

    /// Create a new password without a TOTP.
    pub async fn new_password(
        &self,
        user: UserName,
        password: LoginPassword,
    ) -> Result<PasswordData> {
        // Precondition: user exists
        // TODO

        // Precondition: no password exists for this user yet
        // TODO

        let salt = rand::rng().random::<[u8; 32]>().to_vec();
        let mut hash = [0u8; SHA256_OUTPUT_LEN];

        pbkdf2::derive(
            pbkdf2::PBKDF2_HMAC_SHA256,
            USER_PASSWORD_HASH_ITERATIONS,
            &salt,
            password.0.as_bytes(),
            &mut hash,
        );

        let db = self.database.realm(RealmName::default())?;
        let rw = db.write(DataScope::Global)?;

        let password = PasswordData {
            user,
            iterations: USER_PASSWORD_HASH_ITERATIONS.get(),
            salt,
            hash: hash.to_vec(),
            totp_secret: None,
            ..Default::default()
        };
        rw.insert(password.clone())?;
        rw.commit()?;

        Ok(password)
    }

    /// Create a new password with a TOTP.
    pub async fn new_password_with_totp(
        &self,
        user: UserName,
        password: LoginPassword,
    ) -> Result<PasswordData> {
        // Precondition: user exists
        // TODO

        // Precondition: no password exists for this user yet
        // TODO

        let salt = rand::rng().random::<[u8; 32]>().to_vec();
        let mut hash = [0u8; SHA256_OUTPUT_LEN];

        pbkdf2::derive(
            pbkdf2::PBKDF2_HMAC_SHA256,
            USER_PASSWORD_HASH_ITERATIONS,
            &salt,
            password.0.as_bytes(),
            &mut hash,
        );

        let db = self.database.realm(RealmName::default())?;
        let rw = db.write(DataScope::Global)?;

        let password = PasswordData {
            iterations: USER_PASSWORD_HASH_ITERATIONS.get(),
            salt,
            hash: hash.to_vec(),
            totp_secret: Some(
                Builder::new()
                    .with_algorithm(totp_rs::Algorithm::SHA1)
                    .with_digits(6)
                    .with_skew(1)
                    .with_step_duration(30)
                    .with_secret(Secret::default())
                    .with_issuer(Some("Sandpolis"))
                    .with_account_name(user.to_string())
                    .build()?
                    .to_url()?,
            ),
            user,
            ..Default::default()
        };
        rw.insert(password.clone())?;
        rw.commit()?;

        Ok(password)
    }

    pub async fn password(&self, user: UserName) -> Result<PasswordData> {
        let db = self.database.realm(RealmName::default())?;
        let r = db.r_transaction()?;

        let passwords: Vec<PasswordData> = r
            .scan()
            .secondary(PasswordDataKey::user)?
            .equal(user)?
            // .and(
            //     r.scan()
            //         .secondary(PasswordDataKey::_revision)?
            //         .equal(DataRevision::Latest(0))?,
            // )
            .collect::<Result<Vec<_>, _>>()?;

        if passwords.is_empty() {
            bail!("Password not found");
        } else if passwords.len() > 1 {
            bail!("Too many passwords found");
        }

        Ok(passwords[0].to_owned())
    }

    pub fn new_token(&self, claims: Claims) -> Result<ClientAuthToken> {
        Ok(ClientAuthToken(jsonwebtoken::encode(
            &jsonwebtoken::Header::default(),
            &claims,
            &self
                .jwt_keys
                .get(&claims.realm)
                .ok_or(anyhow!("Realm not found"))?
                .0,
        )?))
    }
}

impl Display for PasswordData {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("PasswordData")
            .field("iterations", &self.iterations)
            .field("salt", &self.salt)
            .finish()
    }
}

#[derive(Debug, Serialize, Deserialize)]
pub struct Claims {
    /// Username
    pub sub: UserName,

    /// Claim expiration
    pub exp: usize,

    /// Whether the user is an admin
    pub admin: bool,

    /// Realm in which these claims exist
    pub realm: RealmName,
}

impl FromRequestParts<UserManager> for Claims {
    type Rejection = StatusCode;

    async fn from_request_parts(
        parts: &mut Parts,
        state: &UserManager,
    ) -> Result<Self, Self::Rejection> {
        // Extract the token from the authorization header
        let TypedHeader(Authorization(bearer)) = parts
            .extract::<TypedHeader<Authorization<Bearer>>>()
            .await
            .map_err(|_| StatusCode::BAD_REQUEST)?;

        // TODO if we can get this from parts.extentions provided by `auth_middleware`,
        // then we won't need to send the header at all.
        let TypedHeader(realm) = parts
            .extract::<TypedHeader<RealmName>>()
            .await
            .map_err(|_| StatusCode::BAD_REQUEST)?;

        let token_data = decode::<Claims>(
            bearer.token(),
            &state.jwt_keys.get(&realm).ok_or(StatusCode::BAD_REQUEST)?.1,
            &Validation::default(),
        )
        .map_err(|_| StatusCode::FORBIDDEN)?;

        Ok(token_data.claims)
    }
}

/// Accept a websocket from a client, agent, or local stratum server.
///
/// Authentication is by realm cert (the `x-realm` header is validated upstream by
/// `auth_middleware`). The peer reports its own `InstanceId` via `x-instance-id`
/// so the server can tell agents (whose own data it pulls, directly or through
/// the ownership machinery) from clients (which subscribe to subsets). The
/// resulting connection is retained in `network.inbound`; dropping it would
/// cancel the socket.
///
/// The peer also reports its stratum via `x-stratum`, which enforces the shape of
/// the network: there is exactly one global stratum server, and local stratum
/// servers only ever connect upward.
// TODO: verify the reported instance id against the connection's certificate.
#[axum_macros::debug_handler]
pub async fn connect(
    State(state): State<UserManager>,
    TypedHeader(realm): TypedHeader<RealmName>,
    headers: axum::http::HeaderMap,
    ws: WebSocketUpgrade,
) -> axum::response::Response {
    use axum::response::IntoResponse;

    let network = state.network.clone();
    let cluster_id = state.instance.cluster_id;
    let local_instance = state.instance.instance_id;
    let stratum = state.stratum.clone();
    let ownership = state.ownership.clone();
    let remote_instance = headers
        .get("x-instance-id")
        .and_then(|v| v.to_str().ok())
        .and_then(|s| s.parse::<sandpolis_instance::InstanceId>().ok());
    let peer_stratum = headers
        .get("x-stratum")
        .and_then(|v| v.to_str().ok())
        .unwrap_or("")
        .to_string();

    // A polling peer says how often it means to come back, which is the only
    // way this server can tell "away until the next window" from "gone".
    let poll = headers
        .get("x-poll-schedule")
        .and_then(|v| v.to_str().ok())
        .filter(|schedule| {
            !schedule.is_empty()
                && schedule.len()
                    <= sandpolis_instance::network::liveness::MAX_SCHEDULE_LEN
        })
        .map(|schedule| sandpolis_instance::network::liveness::PollAnnouncement {
            schedule: schedule.to_string(),
            timeout: std::time::Duration::from_secs(
                headers
                    .get("x-poll-timeout")
                    .and_then(|v| v.to_str().ok())
                    .and_then(|v| v.parse().ok())
                    .unwrap_or(0),
            ),
        });

    // A peer identifying as a server, by its instance id or its stratum header.
    // The header is what a *server* build sends; the id bit is the fallback.
    let peer_is_server =
        !peer_stratum.is_empty() || remote_instance.is_some_and(|id| id.is_server());

    // A network has exactly one global stratum server, so another one announcing
    // itself is a misconfiguration rather than a topology to accommodate.
    if peer_stratum == "global" {
        tracing::warn!(
            "Rejecting connection from another global stratum server; a network has exactly one"
        );
        return (
            StatusCode::CONFLICT,
            "this network already has a global stratum server",
        )
            .into_response();
    }

    // Local stratum servers connect upward only, never to each other, so an
    // inbound server connection here means someone is pointed at the wrong host.
    if stratum.is_local() && peer_is_server {
        tracing::warn!(
            "Rejecting inbound server connection: local stratum servers connect upward only"
        );
        return (
            StatusCode::CONFLICT,
            "connect servers to the global stratum server, not a local stratum server",
        )
            .into_response();
    }

    let mut response = ws.on_upgrade(move |socket| async move {
        let mut cd = ConnectionData::default();
        if let Some(id) = remote_instance {
            cd.remote_instance = id;
        }
        cd.established = chrono::Utc::now();
        // Live connection bookkeeping is local state, allowed on a replica.
        let data = network.connections.push_local(cd).unwrap();

        // Serve this peer's subscriptions from our local realm database.
        let realm_db = network.database.realm(realm.clone()).unwrap();
        let sync_reg =
            sandpolis_instance::network::sync::SyncResponderRegistration::new(realm_db.clone());

        let mut handlers: Vec<&dyn sandpolis_instance::network::RegisterResponders> =
            sandpolis_instance::network::collected_responders().collect();
        handlers.push(&sync_reg);

        let connection = InstanceConnection::websocket(socket, data, realm, cluster_id, &handlers);

        if let Some(poll) = poll {
            let _ = connection.poll.set(poll);
        }

        // Let this connection relay streams to other connections (client -> agent).
        connection
            .streams
            .set_relay(std::sync::Arc::downgrade(&network.relay));

        // Likewise server-only: advertising lets a peer claim to carry traffic
        // for other instances, and an ownership claim carries the right to
        // write an instance's data into the estate — an agent or client must
        // never be able to do either.
        if peer_is_server {
            sandpolis_instance::network::reachability::accept_advertisements(
                &connection,
                network.relay.clone(),
            );
            crate::ownership::accept_claims(&connection, ownership.clone(), realm_db.clone());
        }

        // Pull everything an attached agent owns (the long-lived sync). The
        // filter is scoped to the agent's own instance, so a peer can never
        // smuggle in records belonging to someone else. A server peer's data
        // arrives through the ownership machinery instead.
        if let Some(id) = remote_instance.filter(|id| id.is_agent()) {
            if stratum.is_local() {
                // Ownership decides: the reconciler (`ownership::maintain_agent_sync`)
                // opens this pull once the scope is granted and hydrated.
            } else if let Err(e) = connection
                .open_sync(
                    realm_db,
                    vec![sandpolis_instance::database::sync::SyncFilter::instance(id)],
                )
                .await
            {
                tracing::warn!(error = %e, "Failed to open agent sync stream");
            }
        }

        network.track_inbound(connection);
    });

    // Report our own instance id back to the dialer (mirrors the `x-instance-id`
    // request header) so it can record `remote_instance` instead of leaving a
    // freshly-generated default, which would surface as a phantom graph node.
    if let Ok(value) = axum::http::HeaderValue::from_str(&local_instance.to_string()) {
        response.headers_mut().insert("x-instance-id", value);
    }

    response
}
