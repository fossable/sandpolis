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

Every crate in the workspace apart from `sandpolis` and `sandpolis-mobile` is a
"layer" that brings some functionality. Layers can depend on each other and some
are optional (controlled via cargo features).

## CoLo mode

When a server feature is compiled alongside the client and/or agent and the
binary is run with no subcommand, all instance types start in the same process
and connect to each other automatically over loopback — no `--realm-cert` or
server configuration is needed. This is meant for convenient local testing:
targeting the local instance (e.g. starting a desktop stream) "just works".

Running a specific instance via subcommand (e.g. `sandpolis client`) disables
the auto-connection.

## Mobile App

The `sandpolis-mobile` crate wraps the main `sandpolis` crate.

Build instructions for Android:

```sh
cargo ndk -t arm64-v8a -o android/app/src/main/jniLibs build --link-libcxx-shared
cd android && ./gradlew assembleDebug
```

# Roadmap to 1.0

> This project has been in development for a long time and we need to rapidly
> move toward a MVP and then a stable 1.0 release afterwards. This roadmap
> outlines our overall requirements in no particular order.

- Analyze attack surface
- Compromise tracing:
  - Suppose any entity in the network is compromised, what others could be
    affected?
  - Assign a weight on how bad a compromise of an entity would be
- "Away" mode where monitoring becomes more strict
  - For example, a SSH login when away is highly suspicious and must be notified
    immediately
- Zooming in on a node enters another level of depth where all other nodes
  disappear. Now shows more detailed operations.
- `DatabaseLayer`, `NetworkLayer`, `RealmLayer` should not be layers anymore?
- On desktop, probe, and shell layers: servers are present in the graph (so we
  have links), but they are not interactable. When the server layer is active,
  only servers are shown and they become interactable. Clients are only present
  in the graph when the client layer is active (servers are also present, but
  not interactable).
- Upgrade to bevy 0.19
  - Reimplement all scenes using the new bsn! macro
    (https://bevy.org/news/bevy-0-19/#next-generation-scenes)
  - Replace our bespoke text input with the new native TextInput
    (https://bevy.org/news/bevy-0-19/#text-input)
  - Replace bevy_ui_widgets with
    https://bevy.org/news/bevy-0-19/#more-feathers-widgets
  - Keybinding to enable diagnostic overlay
    (https://bevy.org/news/bevy-0-19/#diagnostics-overlay)

## Layer implementations

> Many layer crates currently contain only data structs / skeletons. These need
> real agent-side collection, server-side routing, and client-side surfacing.

- `sandpolis-tunnel` — only data structs; no listener/repeater/terminator impl
- `sandpolis-wake` — `WakeLayer::schedule` commented out; no Wake-on-LAN sender
- `sandpolis-account` — `todo!()` in `lib.rs`; no account CRUD
- `sandpolis-deploy` — `todo!()` in `ssh/server.rs`; SSH-based agent deployment
  is unimplemented

## `sandpolis-account`

## `sandpolis-snapshot`

- Use boot agent to create/apply "cold snapshots"
- Store snapshots on server

## `sandpolis-audit`

- auditd ingestion on agent, detection rules

## `sandpolis-probe`

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

## `sandpolis-filesystem`

- GUI: delete, create folder, upload/download
- Client can mount remote filesystems via FUSE

## `sandpolis-desktop`

- Desktop streaming controls: start/stop stream, request screenshot

## `sandpolis-instance`

- GUI: view the data of an instance for debugging

## `sandpolis-shell`

- GUI: fully featured shell depending on `alacritty_terminal`

## `sandpolis-inventory`

- Manage firmware updates
- Inspect nixpkgs package versions

## Client

- Wire up Bevy GUI queries — currently most return placeholder data:
  - Instance / hostname query from database
  - Network resident queries
  - Filesystem resident queries (size, listing)
  - Inventory resident queries (packages, hardware)
  - Shell layer queries
- Layer visuals: query instance type, desktop environment, hardware type from DB
- Edge rendering: filesystem connections, desktop streaming connections
- Banner image display in login input (`input.rs`)
- Replace TUI interface with CLI
  - Make sure all features in the TUI have been brought over to the GUI
  - CLI can do anything the GUI can do (except graphical tasks like remote
    desktop)

## Server

- IP blocking middleware (`server/mod.rs` — `from_fn_with_state` TODO)
- Encrypted storage enclave for secrets
