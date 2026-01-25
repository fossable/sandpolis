use serde::{Deserialize, Serialize};

/// Default NUT server port.
pub const DEFAULT_PORT: u16 = 3493;

/// Authentication credentials for a NUT server.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct NutCredentials {
    pub username: String,
    pub password: String,
}

/// Configuration for connecting to a NUT server.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct NutConfig {
    /// Hostname or IP address of the NUT server.
    pub host: String,

    /// Port number (defaults to 3493).
    pub port: Option<u16>,

    /// Optional authentication credentials.
    pub auth: Option<NutCredentials>,
}

/// Request to list all UPS devices on a NUT server.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct ListUpsRequest {
    pub config: NutConfig,
}

/// Information about a UPS device.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct UpsDevice {
    /// The UPS name as registered with the NUT server.
    pub name: String,

    /// Human-readable description of the UPS.
    pub description: String,
}

/// Response from listing UPS devices.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub enum ListUpsResponse {
    Ok(Vec<UpsDevice>),
    ConnectionFailed(String),
    AuthenticationFailed,
}

/// Request to get all variables from a UPS.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct GetUpsVarsRequest {
    pub config: NutConfig,

    /// The name of the UPS to query.
    pub ups_name: String,
}

/// A variable from a UPS device.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct UpsVariable {
    pub name: String,
    pub value: String,
}

/// Response from getting UPS variables.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub enum GetUpsVarsResponse {
    Ok(Vec<UpsVariable>),
    ConnectionFailed(String),
    AuthenticationFailed,
    UpsNotFound,
}

/// Request to get the status of a UPS.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct GetUpsStatusRequest {
    pub config: NutConfig,

    /// The name of the UPS to query.
    pub ups_name: String,
}

/// Parsed UPS status information.
#[derive(Clone, Debug, Default, Serialize, Deserialize)]
pub struct UpsStatus {
    /// UPS status flags (e.g., "OL" for online, "OB" for on battery).
    pub status: Option<String>,

    /// Battery charge percentage (0-100).
    pub battery_charge: Option<f32>,

    /// Battery runtime remaining in seconds.
    pub battery_runtime: Option<f32>,

    /// Battery voltage.
    pub battery_voltage: Option<f32>,

    /// Input voltage.
    pub input_voltage: Option<f32>,

    /// Input frequency in Hz.
    pub input_frequency: Option<f32>,

    /// Output voltage.
    pub output_voltage: Option<f32>,

    /// UPS load percentage.
    pub load: Option<f32>,

    /// UPS temperature in Celsius.
    pub temperature: Option<f32>,

    /// UPS manufacturer.
    pub manufacturer: Option<String>,

    /// UPS model.
    pub model: Option<String>,

    /// UPS serial number.
    pub serial: Option<String>,
}

/// Response from getting UPS status.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub enum GetUpsStatusResponse {
    Ok(UpsStatus),
    ConnectionFailed(String),
    AuthenticationFailed,
    UpsNotFound,
}

/// Request to list available commands for a UPS.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct ListUpsCommandsRequest {
    pub config: NutConfig,

    /// The name of the UPS.
    pub ups_name: String,
}

/// Response from listing UPS commands.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub enum ListUpsCommandsResponse {
    Ok(Vec<String>),
    ConnectionFailed(String),
    AuthenticationFailed,
    UpsNotFound,
}

#[cfg(feature = "agent")]
mod client {
    use super::*;
    use rups::blocking::Connection;
    use rups::{Auth, ConfigBuilder, Host};
    use std::collections::HashMap;

    fn connect(config: &NutConfig) -> Result<Connection, String> {
        let port = config.port.unwrap_or(DEFAULT_PORT);
        let host: Host = (config.host.clone(), port)
            .try_into()
            .map_err(|e: rups::ClientError| format!("invalid host: {}", e))?;

        let auth = config
            .auth
            .as_ref()
            .map(|creds| Auth::new(creds.username.clone(), Some(creds.password.clone())));

        let rups_config = ConfigBuilder::new()
            .with_host(host)
            .with_auth(auth)
            .build();

        Connection::new(&rups_config).map_err(|e| format!("{}", e))
    }

    /// List all UPS devices on a NUT server.
    pub fn list_ups(request: &ListUpsRequest) -> ListUpsResponse {
        let mut conn = match connect(&request.config) {
            Ok(c) => c,
            Err(e) => return ListUpsResponse::ConnectionFailed(e),
        };

        match conn.list_ups() {
            Ok(devices) => {
                let devices = devices
                    .into_iter()
                    .map(|(name, description)| UpsDevice { name, description })
                    .collect();
                ListUpsResponse::Ok(devices)
            }
            Err(rups::ClientError::Nut(rups::NutError::AccessDenied)) => {
                ListUpsResponse::AuthenticationFailed
            }
            Err(e) => ListUpsResponse::ConnectionFailed(format!("{}", e)),
        }
    }

    /// Get all variables from a UPS.
    pub fn get_ups_vars(request: &GetUpsVarsRequest) -> GetUpsVarsResponse {
        let mut conn = match connect(&request.config) {
            Ok(c) => c,
            Err(e) => return GetUpsVarsResponse::ConnectionFailed(e),
        };

        match conn.list_vars(&request.ups_name) {
            Ok(vars) => {
                let vars = vars
                    .into_iter()
                    .map(|var| UpsVariable {
                        name: var.name().to_string(),
                        value: var.value(),
                    })
                    .collect();
                GetUpsVarsResponse::Ok(vars)
            }
            Err(rups::ClientError::Nut(rups::NutError::AccessDenied)) => {
                GetUpsVarsResponse::AuthenticationFailed
            }
            Err(rups::ClientError::Nut(rups::NutError::UnknownUps)) => {
                GetUpsVarsResponse::UpsNotFound
            }
            Err(e) => GetUpsVarsResponse::ConnectionFailed(format!("{}", e)),
        }
    }

    /// Get parsed status information from a UPS.
    pub fn get_ups_status(request: &GetUpsStatusRequest) -> GetUpsStatusResponse {
        let mut conn = match connect(&request.config) {
            Ok(c) => c,
            Err(e) => return GetUpsStatusResponse::ConnectionFailed(e),
        };

        let vars: HashMap<String, String> = match conn.list_vars(&request.ups_name) {
            Ok(vars) => vars
                .into_iter()
                .map(|var| (var.name().to_string(), var.value()))
                .collect(),
            Err(rups::ClientError::Nut(rups::NutError::AccessDenied)) => {
                return GetUpsStatusResponse::AuthenticationFailed
            }
            Err(rups::ClientError::Nut(rups::NutError::UnknownUps)) => {
                return GetUpsStatusResponse::UpsNotFound
            }
            Err(e) => return GetUpsStatusResponse::ConnectionFailed(format!("{}", e)),
        };

        let status = UpsStatus {
            status: vars.get("ups.status").cloned(),
            battery_charge: vars.get("battery.charge").and_then(|v| v.parse().ok()),
            battery_runtime: vars.get("battery.runtime").and_then(|v| v.parse().ok()),
            battery_voltage: vars.get("battery.voltage").and_then(|v| v.parse().ok()),
            input_voltage: vars.get("input.voltage").and_then(|v| v.parse().ok()),
            input_frequency: vars.get("input.frequency").and_then(|v| v.parse().ok()),
            output_voltage: vars.get("output.voltage").and_then(|v| v.parse().ok()),
            load: vars.get("ups.load").and_then(|v| v.parse().ok()),
            temperature: vars.get("ups.temperature").and_then(|v| v.parse().ok()),
            manufacturer: vars.get("ups.mfr").cloned(),
            model: vars.get("ups.model").cloned(),
            serial: vars.get("ups.serial").cloned(),
        };

        GetUpsStatusResponse::Ok(status)
    }

    /// List available commands for a UPS.
    pub fn list_ups_commands(request: &ListUpsCommandsRequest) -> ListUpsCommandsResponse {
        let mut conn = match connect(&request.config) {
            Ok(c) => c,
            Err(e) => return ListUpsCommandsResponse::ConnectionFailed(e),
        };

        match conn.list_commands(&request.ups_name) {
            Ok(commands) => ListUpsCommandsResponse::Ok(commands),
            Err(rups::ClientError::Nut(rups::NutError::AccessDenied)) => {
                ListUpsCommandsResponse::AuthenticationFailed
            }
            Err(rups::ClientError::Nut(rups::NutError::UnknownUps)) => {
                ListUpsCommandsResponse::UpsNotFound
            }
            Err(e) => ListUpsCommandsResponse::ConnectionFailed(format!("{}", e)),
        }
    }
}

#[cfg(feature = "agent")]
pub use client::*;
