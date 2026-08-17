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
_subsystem_ that brings some functionality. Subsystems can depend on each other
and some are optional (controlled via cargo features).

Most subsystems implement functionality for all three instance types. For
example, the way to think about the `sandpolis-server` crate is it "implements
server-related functionality", not that it is itself the server. Therefore, it's
OK for the `sandpolis-client` crate to depend on `sandpolis-server` to get
shared types (just not with the `server` feature enabled).

A subsystem's runtime state lives in one or more _managers_ — `ShellManager`,
`DatabaseManager`, `NetworkManager`, `RealmManager` — which are constructed at
startup and held together in `InstanceState`.

A subsystem may also provide at most one _layer_, which is strictly a GUI
concept: the mode the client's layer picker chooses between, which decides node
visibility, the toolbar, and the node panel body. `LayerName` names it, and
because the mapping is one-to-one, a layer's name is its subsystem's name.
Notifications and services are attributed to a layer so the client can group
them.

#### Instances

An _instance_ is a Sandpolis process running as an **agent**, **server**, or
**client**. One process is exactly one of them, named by its subcommand:

```sh
sandpolis server   # server daemon
sandpolis agent    # agent daemon
sandpolis client   # client, in the foreground
```

#### Strata

Servers exist in one of two _strata_. Every network has **exactly one global
stratum (GS) server** and **any number of local stratum (LS) servers**.

An LS is an edge cache, useful for on-premise installations where it keeps
serving the instances around it even when the link to the GS is down. It
connects to exactly one GS, and never to another LS.

The distinction decides five things:

- **Configuration.** Only the GS reads _realm configs_ (`<realm>.realm.ron`),
  one per realm it serves, which it finds by scanning its `--data` directory; a
  realm exists only because a file declares it, and can never be created at
  runtime. Every other instance — LS servers, agents, clients — is configured by
  CLI flags plus the _realm cert_ (`<realm>.realm.pem`) naming the server it
  trusts.
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
# The global stratum server. It serves every realm config in its data
# directory, creating ./data/default.realm.ron if it finds none. A blank realm
# config means "generate a CA for me", which is written back into the file on
# first start.
sandpolis server --data ./data

# Every start also mints one realm cert per realm and writes it to
# <realm>.realm.pem in the same directory. That file is what attaches another
# instance: copy it where it's needed. Its common name is the realm's address,
# so it names exactly one server and realm.
cp ./data/default.realm.pem ./ops.realm.pem

# A local stratum server. The realm cert is how it authenticates to the GS in
# order to enroll, and having one is what puts this server in the local stratum,
# so it serves no realms of its own — realm configs in its data directory are
# ignored.
sandpolis server --realm ./ops.realm.pem --data ./ls-data --listen 0.0.0.0:8769

# An agent, attached to either stratum. Without --data it keeps nothing across
# restarts. --poll makes it check in on a schedule rather than staying
# connected.
sandpolis agent --realm ./ops.realm.pem --data ./agent-data \
          --poll '0 */5 * * * *' --poll-timeout 30

# A client, attached to either stratum. It picks up every realm cert in --data,
# so naming one is only necessary without a data directory. With neither it
# starts at the login dialog instead.
sandpolis client --realm ./ops.realm.pem
sandpolis client --data ./client-data
```

A realm cert is three PEM blocks — this instance's certificate, the realm CA
that verifies the server, and the private key — so it is readable by `openssl`
and assembled by hand if need be. `$S7S_REALM` is the environment alias for
`--realm`, `$S7S_DATA` for `--data`.

Replication always follows ownership, and it is always pull-based: the owner
serves, the replica subscribes. An agent attached to an LS has its records
applied to the LS's database (once the scope is granted and hydrated), and the
GS pulls them back up with one subscription per LS covering that server's owned
scopes plus its own. Estate-wide data flows the other way, down a standing
global-scope subscription. Because the GS only ever pulls an instance's records
from that instance's current owner, a stale owner's writes can never enter the
estate — the pull subscription is the fence.

#### Notifications

Any subsystem, on any instance, can tell the user something happened. The first
argument names the layer it's attributed to, which is how the client groups it:

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
estate a server manages rather than the servers managing it.

## Development loop

Every time a server starts, it mints an endpoint certificate for each realm it
serves and writes it to `<realm>.realm.pem` in the data directory. Clients and
agents hold the same kind of certificate, so that one file attaches either.

A server started with no `--data` has no directory to scan, so it serves an
implicit `default` realm whose CA lives only in its in-memory database and its
certificate goes to `/tmp` instead. That's the whole setup for running the three
instances against each other on one host:

```sh
sandpolis server                                  # terminal 1
sandpolis agent  --realm /tmp/default.realm.pem   # terminal 2
sandpolis client --realm /tmp/default.realm.pem   # terminal 3
```

#### Containers

Every image comes out of `sandpolis/Dockerfile`, built with the repository root
as the context because `build.rs` walks the whole workspace:

```sh
docker build --target server -f sandpolis/Dockerfile -t sandpolis/server .
docker build --target demo   -f sandpolis/Dockerfile -t sandpolis/demo   .
```

The `server`, `agent` and `client` targets each carry one instance. `demo`
carries all three around a single `--all-features` binary: its entrypoint starts
a server, waits for the realm cert, attaches an agent, and opens the GUI client
if a wayland socket or `$DISPLAY` was handed in. Without one it drops into a
shell with `$S7S_REALM` and `$S7S_DATA` already pointed at the running demo, so
the client subcommands work without flags.

Both stages are nix. The build runs inside `nix-shell shell.nix`, and the
runtime image is the closure of a `buildEnv` from the same package lists
(`nix/deps.nix`, shared with the dev shell) unioned with every store path the
binary itself names — so a library the dev loop needs can't go missing from an
image. `nix/nixpkgs.nix` pins nixpkgs for both, which is what makes the runtime
closure match the glibc the binary was linked against.

## Mobile App

The `sandpolis-mobile` crate wraps the main `sandpolis` crate.

Build instructions for Android:

```sh
cargo ndk -t arm64-v8a -p 31 -o android/app/src/main/jniLibs build --link-libcxx-shared
cd android && ./gradlew assembleDebug
```

# Roadmap to 1.0

> This project has been in development for a long time and we need to rapidly
> move toward a MVP and then a stable 1.0 release afterwards. This roadmap
> outlines our overall requirements in no particular order.

- Investigate whether our current organization of subsystems is optimal or are
  there new subsystems we should create or collapse old subsystems. Previously
  we had subsystems for database, network, realm that we collapsed into the
  instance subsystem, which is why `sandpolis-instance` now holds four managers.
- On desktop, probe, and shell layers: servers are present in the graph (so we
  have links), but they are not interactable. When the server layer is active,
  only servers are shown and they become interactable. Clients are only present
  in the graph when the client layer is active (servers are also present, but
  not interactable).
- The node panel framework needs more shared controls. It has buttons, text and
  gauges today (`sandpolis-client/src/gui/ui/{widgets,gauge}.rs`); charts and
  tables are the obvious gaps, and every layer currently rolls its own list.
  - CPU and memory usage line graphs with historical data
- Notifications currently only reach the user as a toast or an OS notification.
  Add a notification center in the GUI (history, per-layer muting). When the
  client is running in the foreground, show in-app toasts and no OS-native
  notification. If the client is not running in the foreground, only show
  OS-native notifcations.
  - Notification on errors

## `sandpolis-tunnel`

- Application-level tunnel (between two agents or between an agent and a client)
  - Implement as stream
  - Configured in realm config, using syntax similar to SSH local/reverse
    tunnels

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
- Search for existing accounts with adler
  - Also augment with publically available information like account creation
    dates
- Service that checks accounts with haveibeenpwned API
- Service that consumes CVE data and alerts if software versions are found
  - depend on inventory subsystem

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
  - Drive from health layer
- HTTP probe (`http.rs`)
  - Drive from health layer?
- libvirt probe (`libvirt.rs`)
  - Control virtual machines
  - Drive from health layer
- ONVIF probe (`onvif.rs`)
  - View the video stream
  - Driven from the desktop layer?
- RDP probe via desktop subsystem
  - Implement on top of the IronRDP crates
  - Driven from the desktop layer
- RTSP probe (`rtsp/`)
  - Button on expanded node panel to maximize the video stream
  - Driven from the desktop layer?
- SSH probe via shell subsystem
  - Driven from the shell layer
- VNC probe
  - Driven from the desktop layer
- IPMI probe (skeleton in `ipmi.rs`, needs real BMC queries)
  - Drive from inventory layer?
- SNMP probe — partial, needs MIB-driven discovery
  - Drive from inventory layer?
- ARP probe (`arp/`) — verify completeness
- NFS probe
  - Drive from filesystem layer
  - https://github.com/Vaiz/nfs3
- SMB probe
  - Drive from filesystem layer
  - https://github.com/afiffon/smb-rs
- Node panels on probes in probe layer just show what protocols are supported -
  to interact with probes, you use a more specific layer like desktop,
  filesystem, etc.

## `sandpolis-filesystem`

- GUI: delete, create folder, upload/download
- Client can mount remote filesystems via FUSE

## `sandpolis-desktop`

- Desktop streaming controls: start/stop stream, request screenshot
- VNC probes stream here like agents (`vnc.rs`, `probe` feature). RDP probes get
  a node and a placeholder controller; they need an IronRDP backend.
- `DesktopStreamInputEvent` only carries `Option<char>`, so
  Enter/Backspace/arrows reach neither agents nor VNC probes
- Button on expanded node panel to maximize the desktop session

## `sandpolis-instance`

- Improve the GUI for viewing instance databases
- Assign an instance's domain from the GUI

## `sandpolis-shell`

- SSH probes open a terminal here like agents (`ssh.rs`, `probe` feature).
  Telnet probes aren't modelled yet.
- The CLI/TUI (`sandpolis shell --instance`) can't target a probe; it would need
  a device flag on `TargetArgs`

## `sandpolis-inventory`

- Manage firmware updates
- Inspect nixpkgs package versions

## `sandpolis-client`

- Build a reusable 2-column table that can render lists of data
  - It should support vertical scrolling only

## `sandpolis` (main crate)

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
  - Only the following subsystems are supported by bootagents: shell, snapshot
- The node panel icons for instances should always follow the instance type (not
  the layer icon like it currently does)
  - For non-instances, use the layer icon
  - We have icons for these under sandpolis-client/assets/network/.
  - See if we can generate better, more cohesive icons
- Implement db sync permissions
