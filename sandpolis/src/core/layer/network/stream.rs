use anyhow::Result;
use serde::Serialize;

pub trait StreamSource {
    type O: Serialize;

    async fn emit(&self) -> Result<Self::O>;
}

pub trait StreamSink {
    type I: Deserialize;
    type O: Serialize;

    async fn accept(&self, input: Self::I) -> Result<Self::O>;
}

pub enum DataStreamDirection {
    Upstream,
    Downstream,
    Bidirectional,
}

pub struct DataStreamRequest {

    pub permanent: bool,

    string oid = 3;
    repeated string whitelist = 4;

    pub direction: DataStreamDirection,
    pub update_period: Optional<u64>,
}

enum DataStreamResponse {
    Ok(u64),
    Invalid,
    Failed,
}

message EV_STStreamData {

    enum ValueType {
        BYTES = 0;
        BYTES_ARRAY = 1;
        STRING = 2;
        STRING_ARRAY = 3;
        INTEGER = 4;
        INTEGER_ARRAY = 5;
        LONG = 6;
        LONG_ARRAY = 7;
        BOOLEAN = 8;
        BOOLEAN_ARRAY = 9;
        DOUBLE = 10;
        DOUBLE_ARRAY = 11;
        OS_TYPE = 12;
        OS_TYPE_ARRAY = 13;
        INSTANCE_TYPE = 14;
        INSTANCE_TYPE_ARRAY = 15;
        INSTANCE_FLAVOR = 16;
        INSTANCE_FLAVOR_ARRAY = 17;
    }

    // The object's relative OID
    string oid = 1;

    // Whether the object corresponding to the OID was removed
    bool removed = 2;

    // The attribute value type
    ValueType value_type = 3;

    // The timestamp associated with the attribute value
    int64 timestamp = 4;

    bytes           bytes          = 5;
    repeated bytes  bytes_array    = 6;
    string          string         = 7;
    repeated string string_array   = 8;
    int32           integer        = 9;
    repeated int32  integer_array  = 10;
    int64           long           = 11;
    repeated int64  long_array     = 12;
    bool            boolean        = 13;
    repeated bool   boolean_array  = 14;
    double          double         = 15;
    repeated double double_array   = 16;
}

message RQ_StopStream {

    // The stream ID of the stream to stop
    int32 id = 1;
}

enum RS_StopStream {
    STOP_STREAM_OK = 0;
    STOP_STREAM_INVALID = 1;
}
