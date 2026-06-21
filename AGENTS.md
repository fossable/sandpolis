Sandpolis is a Rust-based virtual estate manager that covers:

- Physical devices like servers, desktops, phones, etc
- Purely virtual entities like accounts, EC2 instances, etc

It's comprised of multiple applications:

- Server
- Agent
  - "Regular" mode
  - "UKI" mode
- Client
  - GUI based on Bevy
  - CLI based on clap for scripting or optional TUI based on Ratatui

All of these applications are built from the main `sandpolis` crate (except for
the mobile app) with feature flags.

Every crate in the workspace apart from `sandpolis` and `sandpolis-mobile` is a
"layer" that brings some functionality. Layers can depend on each other and some
are optional (controlled via cargo features).

Most layers implement some functionality for all three instance types. For
example, the way to think about the `sandpolis-agent` crate is it "does
something with agents", not that it "implements what an agent does".

## CoLo mode

When a server feature is compiled alongside the client and/or agent and the
binary is run with no subcommand, all instance types start in the same process
and connect to each other automatically over loopback — no `--realm-cert` or
server configuration is needed. This is meant for convenient local testing:
targeting the local instance (e.g. starting a desktop stream) "just works".

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

- "Away" mode where monitoring becomes more strict
  - For example, a SSH login when away is highly suspicious and must be notified
    immediately
- Zooming in on a node enters another level of depth where all other nodes
  disappear. Now shows more detailed operations.
- `DatabaseLayer`, `NetworkLayer`, `RealmLayer` should not be layers anymore?
  Layers vs subsystems? Layers are just UI?
- On desktop, probe, and shell layers: servers are present in the graph (so we
  have links), but they are not interactable. When the server layer is active,
  only servers are shown and they become interactable. Clients are only present
  in the graph when the client layer is active (servers are also present, but
  not interactable).
- Upgrade to bevy 0.19
  - Reimplement all scenes using the new bsn! macro
    (https://bevy.org/news/bevy-0-19/#next-generation-scenes)
    - The pattern is established in `sandpolis-client/src/gui/ui/scene.rs`
      (theme colors via captured `{...}` exprs + `Themed*` markers,
      `template_value` for runtime components, `on(..)` observers, dynamic
      `Vec<impl Scene>` child lists). The help modal is migrated as the first
      example.
    - Still imperative (migrate to `bsn!` incrementally): login modal, add-agent
      modal, layer picker, node picker, minimap, layer indicator, about panel,
      theme picker, node/edge graph, per-layer controller bodies.
    - `spawn_floating_panel` (`ui/panel.rs`) intentionally stays imperative: it
      returns child entity ids synchronously, which doesn't fit `spawn_scene`.

## `sandpolis-tunnel`

- Application-level tunnel (traffic to client port gets tunneled to port on
  device in agent/server's network)
  - Implement as stream

## `sandpolis-agent`

- Merge `sandpolis-deploy` crate into `sandpolis-agent`
  - The idea is you can install the agent via SSH or via a local executable
  - Drop outdated code that's no longer useful like the Java/protobuf stuff
  - Drop the embedded config - we're moving towards all configuration happening
    via CLI flags
  - Gate appropriately - systemd features are only needed by the agent, UI
    features are only needed by the client, and SSH features are only needed by
    the server.
  - Scope: just build out the framework, we'll provide the actual prebuilt agent
    binaries for install later

## `sandpolis-account`

- Allow CRUD operations on account objects
- Analyze attack surface
- Compromise tracing:
  - Suppose any entity in the network is compromised, what others could be
    affected?
  - Assign a weight on how bad a compromise of an entity would be

## `sandpolis-snapshot`

- Use boot agent to create/apply "cold snapshots"
- Store snapshots on server
- Not compatible with regular agents

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

## `sandpolis` (main crate)

- Banner image display in login input dialog
  - Fetched once a valid server URL is entered
- TUI interface redesign
  - Collapse the `client-tui` and `client-gui` features into just `client`
  - Don't compile the TUI/CLI code on android (via conditional compilation)
  - Instead of a unified TUI, we need a CLI that optionally opens a TUI for
    specific features. The CLI is also usable noninteractively in scripts. For
    example:

```sh
# Run client (with `client` feature only)
# Run server (with `server` feature only)
# Run server + agent CoLo (with `server` + `agent` features)
# Run client + agent CoLo (with `client` + `agent` features)
# Run client + server + agent CoLo (with `server` + `agent` + `client` features)
sandpolis

# Open interactive TUI with agent list. Choose one to restart.
sandpolis agent restart

# Noninteractive version of the above that responds with json
sandpolis agent restart --json --instance UUID

# Open interactive TUI with server list
sandpolis server

# Open interactive TUI
sandpolis probe

# Open interactive TUI
sandpolis desktop

# Noninteractive screenshot
sandpolis desktop screenshot --instance UUID

# Interactive shell (TUI)
sandpolis shell

# Interactive shell (non-TUI)
sandpolis shell --instance UUID
```

- Configure IP blocking middleware in `sandpolis.ron`
  - Add/remove from the GUI in the server layer
- Encrypted storage enclave for secrets
- Support direct connections between clients/agents if hole punching works
  - Streams can optionally run over this direct connection
- Whenever a stream is active, we need to render that in the GUI as a dotted
  line running parallel to the link between the nodes
  - This also works for streams running over direct connections
- Bootagent mode is a UKI that boots before the actual OS
  - The server can place a "boot hold" that prevents the UKI from chainloading
    the actual bootloader.
  - Also configured as fallback in case the primary OS fails to boot which the
    UKI detects
  - Only the following layers are supported by bootagents: shell, snapshot
