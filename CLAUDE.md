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
