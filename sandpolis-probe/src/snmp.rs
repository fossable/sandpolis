//! SNMP probe types.

use serde::{Deserialize, Serialize};

/// Request an SNMP walk operation be executed.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct SnmpWalkRequest {
    /// OID to retrieve.
    pub oid: String,
}

/// SNMP authentication protocol.
#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub enum SnmpAuth {
    Md5,
    Sha1,
}

/// SNMP privacy protocol.
#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub enum SnmpPrivacy {
    Aes,
    Des,
}

/// SNMP security level for SNMPv3.
#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub enum SnmpSecurityLevel {
    AuthPriv,
    AuthNoPriv,
    NoAuthNoPriv,
}

/// SNMP credentials.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub enum SnmpCredentials {
    V3 {
        username: String,
        security: SnmpSecurityLevel,
        auth_protocol: Option<SnmpAuth>,
        auth_password: Option<String>,
        priv_protocol: Option<SnmpPrivacy>,
        priv_password: Option<String>,
    },
    V2c {
        community: String,
    },
    V1 {
        community: String,
    },
}

/// SNMP walk result data.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct SnmpWalkData {
    /// The retrieved OID.
    pub oid: String,
    /// The OID's type.
    pub oid_type: String,
    /// The OID's value.
    pub value: String,
}

/// Response from an SNMP walk operation.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub enum SnmpWalkResponse {
    Ok(Vec<SnmpWalkData>),
    Failed(String),
}
