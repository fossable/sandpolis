
// Request an SNMP walk operation be executed
pub struct SnmpWalkRequest {

    /// OID to retrieve
    pub oid: String,
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
