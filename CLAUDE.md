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

- ~Persistent database stores native rust structs~
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
