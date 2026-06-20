# Claude Code Guide for Sandpolis

Sandpolis is a Rust-based virtual estate manager that covers:

- Physical devices like servers, desktops, phones, etc
- Purely virtual entities like accounts, EC2 instances, etc

It's comprised of multiple applications:

- Server
- Agent
  - "Regular" agent
  - "Boot" agent UKI
- Client
  - GUI based on Bevy and egui
  - TUI based on Ratatui

All of these applications are built from the main `sandpolis` crate (except for
the mobile app) with different feature flags.

Functionality is divided into "layers" which can be enabled/disabled at build
time with Cargo features.

## CoLo mode

When a server feature is compiled alongside the client and/or agent and the
binary is run with no subcommand, all instance types start in the same process
and connect to each other automatically over loopback — no `--realm-cert` or
server configuration is needed. This is meant for convenient local testing:
targeting the local instance (e.g. starting a desktop stream) "just works".

It is wired in three places:

- `RealmLayer::new` (`sandpolis-instance/src/realm/mod.rs`) derives the
  client/agent realm certs from the local cluster CA and keeps them in memory,
  so the co-located client/agent can connect without an out-of-band cert.
- `sandpolis/src/main.rs` (gated on the run including both `server` and the
  role) points the agent at `https://127.0.0.1:<listen port>/default` and opens
  the same loopback connection for the client via
  `client::spawn_local_server_connection`.
- `Relay::find` (`sandpolis-instance/src/network/stream.rs`) excludes the origin
  connection when routing, since the co-located client and agent share one
  `InstanceId` (from `InstanceId::default()`) and would otherwise be ambiguous.

Running a specific instance via subcommand (e.g. `sandpolis client`) disables
the auto-connection.

## Database Layer (`sandpolis-database`)

- Database is based on the native_db crate which saves/loads Rust structs
- Data model are defined by structs with `#[data]` macro which automatically get
  `_id`, `_revision`, `_creation` fields

## Mobile App (`sandpolis-mobile`)

- Wraps the main `sandpolis` crate and uses the `client-gui` feature with Bevy

Build instructions for Android:

```sh
cargo ndk -t arm64-v8a -o android/app/src/main/jniLibs build --link-libcxx-shared
cd android && ./gradlew assembleDebug
```

# Roadmap to 1.0

> This project has been in development for a long time and we need to rapidly
> move toward a MVP and then a stable 1.0 release afterwards. This roadmap
> outlines our overall requirements in no particular order.

## Core features

- Consume auditd
- Manage firmware updates
- Inspect nixpkgs package versions
- Analyze attack surface
- Compromise tracing:
  - Suppose any entity in the network is compromised, what others could be
    affected?
  - Assign a weight on how bad a compromise of an entity would be
- "Away" mode where monitoring becomes more strict
  - For example, a SSH login when away is highly suspicious and must be notified
    immediately
- Mount FUSE filesystem of an agent from a client
- Zooming in on a node enters another level of depth where all other nodes
  disappear. Now shows more detailed operations.
- `DatabaseLayer`, `NetworkLayer`, `RealmLayer` should not be layers anymore?
- On desktop, probe, and shell layers: servers are present in the graph (so we
  have links), but they are not interactable. When the server layer is active,
  only servers are shown and they become interactable. Clients are only present
  in the graph when the client layer is active (servers are also present, but
  not interactable).

## Layer implementations

> Many layer crates currently contain only data structs / skeletons. These need
> real agent-side collection, server-side routing, and client-side surfacing.

- `sandpolis-audit` — only enums/data; no auditd ingestion, no detection rules,
  no event stream wiring
- `sandpolis-tunnel` — only data structs; no listener/repeater/terminator impl
- `sandpolis-wake` — `WakeLayer::schedule` commented out; no Wake-on-LAN sender
- `sandpolis-snapshot` — agent/server modules exist but disk snapshot create /
  restore / diff logic is not implemented
- `sandpolis-account` — `todo!()` in `lib.rs`; no account CRUD
- `sandpolis-deploy` — `todo!()` in `ssh/server.rs`; SSH-based agent deployment
  is unimplemented
- `sandpolis-shell` — `execute.rs` and `session.rs` need remote shell session
  routing across server
- `sandpolis-filesystem` — FUSE session handlers (`FuseLookup`, etc.) defined
  but unimplemented; ties into the FUSE roadmap item above
- `sandpolis-inventory` — `applications/firefox.rs` is the only app collector;
  most `hardware/*.rs` files are data-only with no agent-side collection

## Probe layer

In the UI, the probes should have a node controller window below them with tabs
for each of the following probe "integrations":

- Docker probe (`docker.rs`)
  - Control the docker daemon by starting/stopping containers, etc
- HTTP probe (`http.rs`)
- libvirt probe (`libvirt.rs`)
  - Control virtual machines
- ONVIF probe (`onvif.rs`)
  - View the video stream
- RDP probe (`rdp.rs`)
- RTSP probe (`rtsp/`)
  - View the video stream
- SSH probe (`ssh.rs`)
- VNC probe (`vnc.rs`)
  - Use the `vnc` crate. We have an example of usage at @../goldboot
- IPMI probe (skeleton in `ipmi.rs`, needs real BMC queries)
- SNMP probe — partial, needs MIB-driven discovery
- ARP probe (`arp/`) — verify completeness

## Agent

- Agent self-update (`sandpolis-agent/src/update.rs` is `todo!()`)
- Agent self-uninstall (`sandpolis-agent/src/uninstall.rs` is `todo!()`)
- Embedded config support (`sandpolis-agent/src/config.rs` TODO)
- Connection retry logic (`sandpolis/src/agent/mod.rs` TODO)

## Client — TUI

- Agent list: navigate, connect, selection mode (multiple TODOs)
- Server list: ping, retry, saved-token auth, login-failed dialog,
  connection-failed dialog (~8 TODOs)

## Client — GUI

- Wire up Bevy GUI queries — currently most return placeholder data:
  - Instance / hostname query from database
  - Network resident queries
  - Filesystem resident queries (size, listing)
  - Inventory resident queries (packages, hardware)
  - Shell layer queries
- Layer visuals: query instance type, desktop environment, hardware type from DB
- Desktop streaming controls: start/stop stream, request screenshot
- Filesystem GUI: delete, create folder, upload/download
- Edge rendering: filesystem connections, desktop streaming connections
- Banner image display in login input (`input.rs`)

## Server

- IP blocking middleware (`server/mod.rs` — `from_fn_with_state` TODO)
- Encrypted storage enclave for secrets
