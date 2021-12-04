<p align="center">
	<img src="https://s3.us-east-2.amazonaws.com/github.sandpolis.com/header.png" />
</p>

**Sandpolis** is a real-time distributed administration platform for servers,
desktop computers, embedded devices, and anything in-between.

**This project is unfinished (unstable) and should only be used in a testing
environment!**

### Introduction

The vision for Sandpolis is to build the ultimate no-expense-spared
administration system that:

-   provides unlimited control over any kind of device :computer:,
-   is fast and responsive :zap:,
-   scales to thousands of devices :boom:,
-   and gives you money :moneybag:.

Maybe that last one is a stretch, but at least Sandpolis won't cost you anything
because it's free in terms of cost and, more importantly, **free as in
freedom**.

This project has existed in some form since 2013 and has made tremendous
progress since, but it's also the kind that can never truly be completed.

#### Instances

Sandpolis is not just one program, but a set of several working together. There
are three catagories in which every Sandpolis component (or "instance") belongs:

-   :computer: an **agent** installed on a remote system that carries out
    administration tasks on behalf of users
-   :iphone: a **client** application that users can use to interact with agents
-   :lock: a **server** that facilitates communication between instances in the
    network and makes everything "work"

For end-user convenience (or confusion), there are multiple official programs in
each category. For example, you can login to a Sandpolis server with either the
[Desktop Client](https://github.com/sandpolis/com.sandpolis.client.lifegem) or
from a mobile device with the
[iOS Client](https://github.com/sandpolis/com.sandpolis.client.lockstone).

| Instance                                                                       | Status                                                                                                                                                                                                                                                                                                                                                         | Description                                                        |
| ------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------ |
| [Server](https://github.com/sandpolis/com.sandpolis.server.vanilla)            | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.server.vanilla.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.server.vanilla/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.server.vanilla/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)             | The official server implementation written in Java                 |
| [iOS Client](https://github.com/sandpolis/com.sandpolis.client.lockstone)      | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.client.lockstone.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.client.lockstone/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.client.lockstone/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)       | The mobile iOS client written in Swift                             |
| [Desktop Client](https://github.com/sandpolis/com.sandpolis.client.lifegem)    | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.client.lifegem.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.client.lifegem/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.client.lifegem/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)             | A desktop GUI client written in Java/Kotlin                        |
| [Terminal Client](https://github.com/sandpolis/com.sandpolis.client.ascetic)   | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.client.ascetic.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.client.ascetic/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.client.ascetic/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)             | A simple client with a terminal UI                                 |
| [Web Client](https://github.com/sandpolis/com.sandpolis.client.brightstone)    | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.client.brightstone.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.client.brightstone/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.client.brightstone/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) | A browser-based client written in Python/JavaScript                |
| [Agent](https://github.com/sandpolis/com.sandpolis.agent.vanilla)              | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.agent.vanilla.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.agent.vanilla/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.agent.vanilla/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)                | A standard agent implementation written in Java                    |
| [Native Agent](https://github.com/sandpolis/com.sandpolis.agent.micro)         | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.agent.micro.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.agent.micro/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.agent.micro/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)                      | A lightweight agent written in Rust                                |
| [Minimal Agent](https://github.com/sandpolis/com.sandpolis.agent.nano)         | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.agent.nano.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.agent.nano/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.agent.nano/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)                         | An extremely lightweight agent written in C++                      |
| [Boot Agent](https://github.com/sandpolis/com.sandpolis.agent.boot)            | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.agent.boot.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.agent.boot/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.agent.boot/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)                         | A specialized agent designed to run in a minimal Linux environment |
| [Agent Distributor](https://github.com/sandpolis/com.sandpolis.distagent.rust) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.distagent.rust.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.distagent.rust/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.distagent.rust/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)             | A specialized instance responsible for installing agents           |

#### Plugins

Sandpolis supports plugins as a first-class feature. Essentially _all_ end-user
functionality (file transfers, remote desktop, etc) is implemented as a plugin.
This allows users to choose exactly what features they want and ignore the rest.
Third-party plugins can also be installed, but must be signed with a trusted
code-signing certificate.

The following plugins are officially supported:

| Plugin                                                                            | Status                                                                                                                                                                                                                                                                                                                                                      |
| --------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| [Desktop Plugin](https://github.com/sandpolis/com.sandpolis.plugin.desktop)       | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.plugin.desktop.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.plugin.desktop/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.plugin.desktop/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)          |
| [Shell Plugin](https://github.com/sandpolis/com.sandpolis.plugin.shell)           | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.plugin.shell.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.plugin.shell/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.plugin.shell/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)                |
| [Filesystem Plugin](https://github.com/sandpolis/com.sandpolis.plugin.filesystem) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.plugin.filesystem.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.plugin.filesystem/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.plugin.filesystem/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |
| [Snapshot Plugin](https://github.com/sandpolis/com.sandpolis.plugin.snapshot)     | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.plugin.snapshot.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.plugin.snapshot/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.plugin.snapshot/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)       |
| [Device Plugin](https://github.com/sandpolis/com.sandpolis.plugin.device)         | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.plugin.device.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.plugin.device/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.plugin.device/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)             |
| [OS Query Plugin](https://github.com/sandpolis/com.sandpolis.plugin.osquery)      | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.plugin.osquery.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.plugin.osquery/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.plugin.osquery/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)          |
| [Update Plugin](https://github.com/sandpolis/com.sandpolis.plugin.update)         | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.plugin.update.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.plugin.update/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.plugin.update/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)             |

#### Core Modules

The following library modules define the core of Sandpolis. Instances and
plugins depend on these basic modules which prevents code duplication. You
probably don't need to worry about these unless you're developing plugins.

| Module                                                                            | Status                                                                                                                                                                                                                                                                                                                                                      |
| --------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| [core.agent](https://github.com/sandpolis/com.sandpolis.core.agent)               | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.core.agent.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.core.agent/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.core.agent/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)                      |
| [core.client](https://github.com/sandpolis/com.sandpolis.core.client)             | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.core.client.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.core.client/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.core.client/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)                   |
| [core.clientagent](https://github.com/sandpolis/com.sandpolis.core.clientagent)   | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.core.clientagent.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.core.clientagent/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.core.clientagent/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)    |
| [core.clientserver](https://github.com/sandpolis/com.sandpolis.core.clientserver) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.core.clientserver.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.core.clientserver/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.core.clientserver/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |
| [core.foreign](https://github.com/sandpolis/com.sandpolis.core.foreign)           | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.core.foreign.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.core.foreign/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.core.foreign/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)                |
| [core.foundation](https://github.com/sandpolis/com.sandpolis.core.foundation)     | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.core.foundation.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.core.foundation/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.core.foundation/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)       |
| [core.instance](https://github.com/sandpolis/com.sandpolis.core.instance)         | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.core.instance.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.core.instance/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.core.instance/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)             |
| [core.server](https://github.com/sandpolis/com.sandpolis.core.server)             | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.core.server.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.core.server/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.core.server/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)                   |
| [core.serveragent](https://github.com/sandpolis/com.sandpolis.core.serveragent)   | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.core.serveragent.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.core.serveragent/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.core.serveragent/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)    |

### Installing

You could install Sandpolis now, but it's likely to be an underwhelming
experience. Once things are more stable, installation instructions will appear
here.

In the meantime, here's a screenshot of the iOS client back when it was actually
functional:

<p align="center">
	<img src="https://s3.us-east-2.amazonaws.com/github.sandpolis.com/client/lockstone/triptych.png" />
</p>

### Building

Since git submodules are used extensively here, you can conveniently build just
a subset of the project. To choose what components to build, initialize the
desired submodules using the makefile utilities:

```sh
# Checkout required modules for the server component
make enableServerVanilla
```

Gradle is used to orchestrate the build:

```sh
./gradlew build
```

This will build all modules that are currently checked out.

#### Using Vagrant

In order to conveniently build components for other operating systems, Vagrant
can be used:

```sh
vagrant up linux
vagrant ssh linux
```
