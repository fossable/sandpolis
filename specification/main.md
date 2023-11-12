# Sandpolis Specification v0.1

This specification defines the internals of the reference implementation located at https://github.com/sandpolis/sandpolis.

## Module

Sandpolis is composed of three types of modules:

- Instance Module
- Core Module
- Plugin Module

| Module ID                        | Module Name                    | Type     | Implementation Technologies |
| ---------------------------------|--------------------------------|----------|-----------------------------|
| org.s7s.core.ext.apt             | sandpolis-core-ext-apt         | Library  | Rust, Kotlin                |
| org.s7s.core.ext.freedesktop     | sandpolis-core-ext-freedesktop | Library  | Rust, Kotlin                |
| org.s7s.core.ext.fuse            | sandpolis-core-ext-fuse        | Library  | Rust, Kotlin                |
| org.s7s.core.ext.homebrew        | sandpolis-core-ext-homebrew    | Library  | Rust, Kotlin                |
| org.s7s.core.ext.launchd         | sandpolis-core-ext-launchd     | Library  | Rust, Kotlin                |
| org.s7s.core.ext.linux           | sandpolis-core-ext-linux       | Library  | Rust, Kotlin                |
| org.s7s.core.ext.osquery         | sandpolis-core-ext-osquery     | Library  | Rust                        |
| org.s7s.core.ext.pacman          | sandpolis-core-ext-pacman      | Library  | Rust, Kotlin                |
| org.s7s.core.ext.qcow2           | sandpolis-core-ext-qcow2       | Library  | Rust                        |
| org.s7s.core.ext.systemd         | sandpolis-core-ext-systemd     | Library  | Rust, Kotlin                |
| org.s7s.core.ext.uefi            | sandpolis-core-ext-uefi        | Library  | Rust                        |
| org.s7s.core.foundation          | sandpolis-core-foundation      | Library  | Rust, Kotlin                |
| org.s7s.core.instance            | sandpolis-core-instance        | Library  | Rust, Kotlin                |
| org.s7s.core.protocol            | sandpolis-core-protocol        | Library  | Rust, Kotlin                |
| org.s7s.instance.agent           | sandpolis-agent                | Instance | Rust                        |
| org.s7s.instance.bootagent       | sandpolis-bootagent            | Instance | Rust                        |
| org.s7s.instance.client.android  | sandpolis-client-android       | Instance | Kotlin                      |
| org.s7s.instance.client.desktop  | sandpolis-client-desktop       | Instance | Kotlin                      |
| org.s7s.instance.client.ios      | sandpolis-client-ios           | Instance | Kotlin                      |
| org.s7s.instance.client.terminal | sandpolis-client-terminal 
| org.s7s.instance.client.web      | sandpolis-client-web           | Instance | Kotlin                      |
| org.s7s.instance.deployer        | sandpolis-deployer             | Instance | Rust                        |
| org.s7s.instance.installer       | sandpolis-installer            | Instance | Kotlin                      |
| org.s7s.instance.server          | sandpolis-server               | Instance | Rust                        |
| org.s7s.pkg.aur.agent            | sandpolis-core-ext-apt 
| org.s7s.pkg.aur.client.desktop   | sandpolis-core-ext-apt 
| org.s7s.pkg.aur.client.terminal  | sandpolis-core-ext-apt 
| org.s7s.pkg.aur.server           | sandpolis-core-ext-apt 
| org.s7s.plugin.alert             | sandpolis-plugin-alert         | Plugin   |
| org.s7s.plugin.desktop           | sandpolis-plugin-desktop       | Plugin   |
| org.s7s.plugin.device            | sandpolis-plugin-device        | Plugin   |
| org.s7s.plugin.filesystem        | sandpolis-plugin-filesystem    | Plugin   |
| org.s7s.plugin.shell             | sandpolis-plugin-shell         | Plugin   |
| org.s7s.plugin.snapshot          | sandpolis-plugin-snapshot      | Plugin   |
| org.s7s.plugin.update            | sandpolis-plugin-update        | Plugin   |

### Implementation Languages

Rust is the language of choice for the server and agent for its high performance
and safety by default.

Kotlin is used on the client side because it has popular UI toolkits and code sharing
is easy across Android, iOS, web, and desktop applications.

### Naming

```
org.s7s.[module group].[component].[variant]
```

### Versioning

Modules are versioned with a three-field [semantic version](https://semver.org) of the typical form: (`v[MAJOR].[MINOR].[PATCH]`).
A `PATCH` value of 0 should be omitted leaving a version that looks like: `v1.3`.
