
message PostListenerRequest {

    // The listening address
    string address = 1;

    // The listening port
    int32 port = 2;
}

enum PostListenerResponse {
    CREATE_LISTENER_OK = 0;
    CREATE_LISTENER_ACCESS_DENIED = 1;

    CREATE_LISTENER_INVALID_PORT = 2;
}

enum DeleteListenerResponse {
    DELETE_LISTENER_OK = 0;
    DELETE_LISTENER_ACCESS_DENIED = 1;
}

message PutListenerRequest {

}

enum PutListenerResponse {
    UPDATE_LISTENER_OK = 0;
    UPDATE_LISTENER_ACCESS_DENIED = 1;
}
