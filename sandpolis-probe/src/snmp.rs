
/// Request an SNMP walk operation be executed
pub struct SnmpWalkRequest {

    /// OID to retrieve
    pub oid: String,
}

pub enum SnmpAuth {
    Md5,
    Sha1,
}

pub enum SnmpPrivacy {
    Aes,
    Des,
}

pub enum SnmpSecurityLevel {
    AuthPriv,
    AuthNoPriv,
}

pub enum SnmpCredentials {
    V3 {
        pub username: String,
        pub security: SnmpSecurityLevel,
    },
    V2c,
    V1 {
        community: String,
    },
}

// Response containing the result of a walk operation
message RS_SnmpWalk {

    message Data {

        // The retrieved OID
        string oid = 1;

        // The OID's type
        string type = 2;

        // The OID's value
        string value = 3;
    }

    repeated Data data = 1;
}
