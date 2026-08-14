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

#### Instances

An _instance_ is a Sandpolis process running as an **agent**, **server**, or
**client** (or all three in CoLo mode).

#### Strata

Servers exist in one of two _strata_. Every network has **exactly one global
stratum (GS) server** and **any number of local stratum (LS) servers**.

An LS is an edge cache, useful for on-premise installations where it keeps
serving the instances around it even when the link to the GS is down. It
connects to exactly one GS, and never to another LS.

The distinction decides five things:

- **Configuration.** Only the GS reads `.realm` files, one per realm it serves,
  which it finds by scanning its `--data` directory; a realm exists only because
  a file declares it, and can never be created at runtime. Every other instance
  — LS servers, agents, clients — is configured by CLI flags plus the `.server`
  file naming the server it trusts.
- **Trust.** The GS holds the realm CA and is the network's single trust root.
  An LS never generates a CA; on first start it enrolls with the GS, which
  issues it a server certificate. The CA's private key never leaves the GS, so
  an LS can verify peers but never issue certificates of its own. Enrollment
  blocks the listener (with backoff) until it succeeds, since the certificate is
  what the listener presents.
- **Ownership.** Every piece of instance data has exactly one owner at a time:
  the server that instance is directly connected to. A server always owns its
  own scope; estate-wide data (users, accounts, realms) is always owned by the
  GS. The GS arbitrates through a persistent grant table: servers _claim_ their
  attached instances (the GS claims locally, an LS over a claim stream), each
  transfer bumps a fencing epoch, and disconnection is **not** a release — an LS
  keeps its scopes, and keeps writing, through a GS outage and across its own
  restarts. Ownership only moves when an instance shows up attached somewhere
  else.
- **Writability.** `RealmDatabase::write(scope)` gates every write on the
  ownership above: the GS holds full authority, an LS holds
  `WriteAuthority::Scoped`. A freshly granted scope is not writable until it has
  been _hydrated_ — fully replicated down from the GS — so its revision counters
  continue where the previous owner left off. The only paths around the gate are
  replication itself (revision-guarded, so an older record never clobbers a
  newer one) and instance-local bookkeeping (see `RealmDatabase::local_write`).
- **Routing.** Streams cross strata. A client addresses an agent by `InstanceId`
  and never learns the topology: an LS advertises its attached instances to the
  GS, and points its own default route at the GS for everything else.

```sh
# The global stratum server. It serves every .realm file in its data directory,
# creating ./data/default.realm if it finds none. A blank realm file means
# "generate a CA for me", which is written back into the file on first start.
sandpolis --data ./data

# Mint a .server file for another instance. The certificate's common name is
# the address given here, so it names exactly one server and realm.
sandpolis new-client-cert --realm ./data/default.realm \
          --address gs.example.com:8768 --output ops.server
sandpolis new-agent-cert --realm ./data/default.realm \
          --address gs.example.com:8768 --output fleet.server

# A local stratum server. The .server file is how it authenticates to the GS in
# order to enroll, and having one is what puts this server in the local stratum,
# so it serves no realms of its own — realm files in its data directory are
# ignored.
sandpolis --server ./ops.server --data ./ls-data --listen 0.0.0.0:8769

# An agent, attached to either stratum. Without --data it keeps nothing across
# restarts; clients have no --data flag and are always ephemeral.
sandpolis --server ./fleet.server --data ./agent-data
```

A `.server` file carries the realm CA, this instance's own certificate, and —
for an agent — its polling schedule, so one file is the whole connection policy.
`$S7S_SERVER` is the environment alias for `--server`.

Replication always follows ownership, and it is always pull-based: the owner
serves, the replica subscribes. An agent attached to an LS has its records
applied to the LS's database (once the scope is granted and hydrated), and the
GS pulls them back up with one subscription per LS covering that server's owned
scopes plus its own. Estate-wide data flows the other way, down a standing
global-scope subscription. Because the GS only ever pulls an instance's records
from that instance's current owner, a stale owner's writes can never enter the
estate — the pull subscription is the fence.

#### Notifications

Any layer, on any instance, can tell the user something happened:

```rust
notification::notify(
    Notification::error("Health", format!("{name} failed")).about(instance_id),
);
```

That writes a `NotificationData` row owned by the raising instance, so delivery
is just the replication above — agent to its owning server, up to the GS, out to
any client with the standing subscription. There is no notification protocol.

The client decides how the user finds out: an in-app toast while the window has
focus, the operating system's own notification interface when it doesn't (or
when there's no GUI at all, as in a TUI subcommand). A persisted watermark keeps
a subscription's opening snapshot from announcing history on every start.

#### World View

Users interact with the Sandpolis network via a real-time graph called the
_world view_. The graph is made of:

- Nodes
  - A _node_ is a point on the map. It could be an instance, probe, or a generic
    entity.
- Links
  - A _link_ expresses some general relationship between two nodes
- Terrains
  - A _terrain_ is a grouping of nodes

#### Domains

A _domain_ groups nodes under a shared name, which is drawn as a terrain. The
name is a service domain like `github.com`, so an instance and the accounts on
that service land in one region.

An account always belongs to one — its domain is part of its identity. An
instance does not: membership is an assignment stored in `DomainData`, owned by
the GS and replicated from there, and an instance no domain names belongs to
none and draws no terrain. Servers are never members, because domains group the
estate a server manages rather than the servers managing it — unless the process
is also an agent or client, whose shared `InstanceId` carries those bits (CoLo).

## CoLo mode

When a server feature is compiled alongside the client and/or agent and the
binary is run with no subcommand, all instance types start in the same process
and connect to each other automatically over loopback — no `.server` file or
other configuration is needed. With no `--data` flag there is no directory to
scan, so the server serves an implicit `default` realm whose CA lives only in
the in-memory database. This is meant for convenient local testing: targeting
the local instance (e.g. starting a desktop stream) "just works".

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

- Remove the CoLo mode special cases. The program can still build all three
  instances, but we want them to be called separately like this:

```sh
sandpolis agent # Start agent daemon
sandpolis server # Start server daemon
sandpolis client # Start client foreground
```

This should also simplify `InstanceId` which no longer carries multiple instance
support.

- `DatabaseLayer`, `NetworkLayer`, `RealmLayer` should be "Managers"
  - crates are "subsystems" while layer specifically refers to the UI concept
- On desktop, probe, and shell layers: servers are present in the graph (so we
  have links), but they are not interactable. When the server layer is active,
  only servers are shown and they become interactable. Clients are only present
  in the graph when the client layer is active (servers are also present, but
  not interactable).
- In GUI, implement 'node effects'
  - "selected" - we currently have this
  - "multi-selected" - we currently have this
  - "disabled" / "offline"
- The node panel framework needs more shared controls. It has buttons, text and
  gauges today (`sandpolis-client/src/gui/ui/{widgets,gauge}.rs`); charts and
  tables are the obvious gaps, and every layer currently rolls its own list.
- Notifications currently only reach the user as a toast or an OS notification.
  Add a notification center in the GUI (history, per-layer muting). When the
  client is running in the foreground, show in-app toasts and no OS-native
  notification. If the client is not running in the foreground, only show
  OS-native notifcations.
  - Notification on errors
  - Notification when a new instance joins for the first time
- Ensure the following constraints:
  - GS servers must serve every .realm file in their --data directory
  - LS server must accept a single --server arg
  - Agents must accept a single --server arg
  - Clients must accept a single --server arg

## `sandpolis-tunnel`

- Application-level tunnel (traffic to client port gets tunneled to port on
  device in agent/server's network)
  - Implement as stream

## `sandpolis-agent`

- Provide prebuilt agent binaries for SSH deployment to install. The deploy
  framework resolves them through `deploy::binary::AgentBinarySource`, which
  currently has no source installed, so a fresh install stops with "no prebuilt
  agent binary available".
- Add `sandpolis agent deploy` subcommand which drives the same flow as the GUI
  - Also support `--dryrun` mode of operation

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

## `sandpolis-health`

- Run DNS/TCP/HTTP tests

## `sandpolis-audit`

- auditd ingestion on agent, detection rules
- Button on toolbar that sets "Away" mode where monitoring becomes more strict
  - For example, a sucessful SSH login when away is highly suspicious and must
    be notified immediately
- Configurable notifications
  - failed login attempts
  - all login attempts

## `sandpolis-probe`

- Docker probe (`docker.rs`)
  - Control the docker daemon by starting/stopping containers, etc
- HTTP probe (`http.rs`)
- libvirt probe (`libvirt.rs`)
  - Control virtual machines
- ONVIF probe (`onvif.rs`)
  - View the video stream
- RDP probe via desktop layer
  - Implement on top of the IronRDP crates
  - Button on expanded node panel to maximize the RDP session
- RTSP probe (`rtsp/`)
  - Button on expanded node panel to maximize the video stream
- SSH probe via shell layer
  - Button on expanded node panel to maximize the terminal
- VNC probe
  - Button on expanded node panel to maximize the VNC session
- IPMI probe (skeleton in `ipmi.rs`, needs real BMC queries)
- SNMP probe — partial, needs MIB-driven discovery
- ARP probe (`arp/`) — verify completeness

## `sandpolis-filesystem`

- GUI: delete, create folder, upload/download
- Client can mount remote filesystems via FUSE

## `sandpolis-desktop`

- Desktop streaming controls: start/stop stream, request screenshot
- VNC probes stream here like agents (`vnc.rs`, `probe` feature). RDP probes get
  a node and a placeholder controller; they need an IronRDP backend.
- `DesktopStreamInputEvent` only carries `Option<char>`, so
  Enter/Backspace/arrows reach neither agents nor VNC probes

## `sandpolis-instance`

- Improve the GUI for viewing instance databases
- Assign an instance's domain from the GUI

## `sandpolis-shell`

- GUI: fully featured shell depending on `alacritty_terminal`
- SSH probes open a terminal here like agents (`ssh.rs`, `probe` feature).
  Telnet probes aren't modelled yet.
- The CLI/TUI (`sandpolis shell --instance`) can't target a probe; it would need
  a device flag on `TargetArgs`

## `sandpolis-inventory`

- Manage firmware updates
- Inspect nixpkgs package versions

## `sandpolis` (main crate)

- Banner image display in login input dialog
  - Fetched once a valid server URL is entered
- TUI interface redesign
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

- Remove `--blocked-ips` and just store IP block list in realm database
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
