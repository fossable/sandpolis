<p align="center">
	<img src="https://s3.us-east-2.amazonaws.com/github.sandpolis.com/header.png" />
</p>

**Sandpolis** is a remote administration platform for servers, desktop computers, embedded devices, and anything in-between. Although designed primarily for sysadmins and enthusiasts, it should also be usable by most hominins.

:zap: **This project is unfinished and should only be used in a secure testing environment!** :zap:

| Module | Status |
|--------|--------|
| [Core](https://github.com/sandpolis/sandpolis) | ![Release](https://img.shields.io/github/v/tag/sandpolis/sandpolis.svg?label=release) [![Build](https://github.com/sandpolis/sandpolis/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/sandpolis/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |
| [Server](https://github.com/sandpolis/com.sandpolis.server.vanilla) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.server.vanilla.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.server.vanilla/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.server.vanilla/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |
| [iOS Client](https://github.com/sandpolis/com.sandpolis.client.lockstone) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.client.lockstone.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.client.lockstone/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.client.lockstone/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |
| [Desktop Client](https://github.com/sandpolis/com.sandpolis.client.lifegem) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.client.lifegem.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.client.lifegem/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.client.lifegem/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |
| [Terminal Client](https://github.com/sandpolis/com.sandpolis.client.ascetic) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.client.ascetic.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.client.ascetic/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.client.ascetic/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |
| [Agent](https://github.com/sandpolis/com.sandpolis.agent.vanilla) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.agent.vanilla.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.agent.vanilla/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.agent.vanilla/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |
| [Native Agent](https://github.com/sandpolis/com.sandpolis.agent.micro) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.agent.micro.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.agent.micro/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.agent.micro/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |

### Introduction
Sandpolis is a real-time distributed application composed of three types of components:

- an **agent** installed on remote systems that carries out tasks on behalf of users
- a **client** application that users interact with
- a **server** that facilitates communication between instances in the network and makes everything "work"

In a typical setup, the server is hosted by a cloud provider like AWS or GCP, the client is installed on the administrator's machine, and the agent is installed on a large number of machines that need to be monitored/controlled.

#### Plugins
Sandpolis supports plugins as a first-class feature. In fact, *all* end-user functionality in Sandpolis (file transfers, remote desktop, etc) is implemented through plugins. Third-party plugins can also be installed, but must be signed with a trusted code-signing certificate.

The following official plugins are available by default:

| Plugin | Status | Description |
|--------|--------|-------------|
| [Desktop Plugin](https://github.com/sandpolis/com.sandpolis.plugin.desktop) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.plugin.desktop.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.plugin.desktop/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.plugin.desktop/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) | Provides remote desktop sessions |
| [Shell Plugin](https://github.com/sandpolis/com.sandpolis.plugin.shell) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.plugin.shell.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.plugin.shell/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.plugin.shell/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) | Provides remote shell sessions |
| [Filesystem Plugin](https://github.com/sandpolis/com.sandpolis.plugin.filesystem) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.plugin.filesystem.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.plugin.filesystem/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.plugin.filesystem/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) | Provides file browsing and transfers |
| [Snapshot Plugin](https://github.com/sandpolis/com.sandpolis.plugin.snapshot) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.plugin.snapshot.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.plugin.snapshot/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.plugin.snapshot/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) | Provides cold disk snapshots |
| [Device Plugin](https://github.com/sandpolis/com.sandpolis.plugin.device) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.plugin.device.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.plugin.device/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.plugin.device/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) | Provides monitoring and control of agentless devices |

#### Widely compatible
Sandpolis runs on many different operating systems and CPU architectures thanks to the JVM. For systems with minimal resources, there's also a native agent written in C++ that offers a subset of the usual features.

#### Low latency and high concurrency
Sandpolis is a real-time application which leads to a more satisfying user experience. Data is available right away and operations happen immediately.

To scale effectively, Sandpolis can utilize multiple servers in the same network which enables a large number of total concurrent connections from agents.

#### Uncompromising on performance and security
Every reasonable measure has been taken to ensure Sandpolis is both secure and performant. When it's not possible to achieve every desirable design objective, these two are prioritized.
