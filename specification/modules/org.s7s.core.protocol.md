# Sandpolis Protocol

Sandpolis uses a HTTP JSON protocol based on
[protocol buffers](https://developers.google.com/protocol-buffers) for all
inter-instance network communications. By default, the server listens on TCP
port **8768**.

The schemas for all core messages are defined in [sandpolis/org.s7s.core.protocol](https://github.com/sandpolis/org.s7s.core.protocol).
Plugins may define their own messages.

## Streams

Many operations require real-time data for a short-lived or long-lived session.

All streams have a _source_ and a _sink_ and can exist between any two instances
(a stream where the source and sink reside on the same instance is called a
_local stream_). The source's purpose is to produce _stream events_ at whatever
frequency is appropriate for the use-case and the sink's purpose is to consume
those stream events.

### Stream Transport

Stream transport is handled by secure websockets and uses binary messages rather
than JSON.

### Direct Streams

For high-volume or low-latency streams, a direct connection may be preferable to
a connection through a server. In these cases, the server will first attempt to
coordinate a websocket connection between the two instances using the typical
"hole-punching" strategy. If a direct connection cannot be established, the stream
falls back to indirect mode.

Direct websocket connections can be coordinated only between a client and agent
or between two agents.

### Stream Multicasting

Stream sources can push events to more than one sink simultaneously. This is
called multicasting and can save bandwidth in situations where multiple users
request the same resource at the same time. For exmaple, if more than one user
requests a desktop stream on an agent simultaneously, the agent only needs to
produce the data once and the server will duplicate it.

Direct streams cannot be multicast.

## Session

All sessions must be authenticated with client certificates. The maximum lifetime
of any client certificate is one year. To reduce the burden of manually rotating
certificates on a large number of agents, the process is automated.

Rotating certificates on servers and clients cannot be automated.

### Session Cookies

Client instances must additionally provide a valid username + password + TOTP token
in order to authenticate a session. The client will be provided a session cookie
to be included in subsequent requests.

The session cookie expires after 15 minutes and can be renewed if the client determines
the user is not idle.