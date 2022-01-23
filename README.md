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

- provides unlimited control over any kind of device :computer:,
- is fast and responsive :zap:,
- scales to thousands of devices :boom:,
- and gives you money :moneybag:.

Maybe that last one is a stretch, but at least Sandpolis won't cost you anything
because it's free in terms of cost and, more importantly, **free as in
freedom**.

This project has existed in some form since 2013 and has made tremendous
progress since, but it's also the kind that can never truly be completed.

#### Instances

Sandpolis is not just one program, but a set of several working together. There
are three catagories in which every Sandpolis component (or "instance") belongs:

- :computer: an **agent** installed on a remote system that carries out
  administration tasks on behalf of users
- :iphone: a **client** application that users can use to interact with agents
- :lock: a **server** that facilitates communication between instances in the
  network and makes everything "work"

For end-user convenience (or confusion), there are multiple official programs in
each category. For example, you can login to a Sandpolis server with either the
[Desktop Client](https://github.com/sandpolis/org.s7s.instance.client.desktop)
or from a mobile device with the
[iOS Client](https://github.com/sandpolis/org.s7s.instance.client.ios).

| Instance                                                                         | Status                                                                                                                                                                                                                                                                                                                                                         | Description                                                        |
| -------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------ |
| [Server](https://github.com/sandpolis/org.s7s.instance.server.java)              | ![Release](https://img.shields.io/github/v/tag/sandpolis/org.s7s.instance.server.java.svg?label=release) [![Build](https://github.com/sandpolis/org.s7s.instance.server.java/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/org.s7s.instance.server.java/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)             | The official server implementation                                 |
| [iOS Client](https://github.com/sandpolis/org.s7s.instance.client.ios)           | ![Release](https://img.shields.io/github/v/tag/sandpolis/org.s7s.instance.client.ios.svg?label=release) [![Build](https://github.com/sandpolis/org.s7s.instance.client.ios/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/org.s7s.instance.client.ios/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)                | The mobile iOS client written in Swift                             |
| [Desktop Client](https://github.com/sandpolis/org.s7s.instance.client.desktop)   | ![Release](https://img.shields.io/github/v/tag/sandpolis/org.s7s.instance.client.desktop.svg?label=release) [![Build](https://github.com/sandpolis/org.s7s.instance.client.desktop/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/org.s7s.instance.client.desktop/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)    | A desktop GUI client written in Java/Kotlin                        |
| [Terminal Client](https://github.com/sandpolis/org.s7s.instance.client.terminal) | ![Release](https://img.shields.io/github/v/tag/sandpolis/org.s7s.instance.client.terminal.svg?label=release) [![Build](https://github.com/sandpolis/org.s7s.instance.client.terminal/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/org.s7s.instance.client.terminal/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) | A simple client with a terminal UI                                 |
| [Web Client](https://github.com/sandpolis/org.s7s.instance.client.web)           | ![Release](https://img.shields.io/github/v/tag/sandpolis/org.s7s.instance.client.web.svg?label=release) [![Build](https://github.com/sandpolis/org.s7s.instance.client.web/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/org.s7s.instance.client.web/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)                | A browser-based client written in Python/JavaScript                |
| [Agent](https://github.com/sandpolis/org.s7s.instance.agent)                     | ![Release](https://img.shields.io/github/v/tag/sandpolis/org.s7s.instance.agent.svg?label=release) [![Build](https://github.com/sandpolis/org.s7s.instance.agent/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/org.s7s.instance.agent/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)                               | The official agent implementation                                  |
| [Bootagent](https://github.com/sandpolis/org.s7s.instance.bootagent)             | ![Release](https://img.shields.io/github/v/tag/sandpolis/org.s7s.instance.bootagent.svg?label=release) [![Build](https://github.com/sandpolis/org.s7s.instance.bootagent/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/org.s7s.instance.bootagent/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)                   | A specialized agent designed to run at boot time                   |
| [Agent Deployer](https://github.com/sandpolis/org.s7s.instance.deployer)         | ![Release](https://img.shields.io/github/v/tag/sandpolis/org.s7s.instance.deployer.svg?label=release) [![Build](https://github.com/sandpolis/org.s7s.instance.deployer/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/org.s7s.instance.deployer/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)                      | A specialized instance responsible for installing agents           |

#### Plugins

Sandpolis supports plugins as a first-class feature. Essentially _all_ end-user
functionality (file transfers, remote desktop, etc) is implemented as a plugin.
This allows users to choose exactly what features they want and ignore the rest.
Third-party plugins can also be installed, but must be signed with a trusted
code-signing certificate.

The following plugins are officially supported:

| Plugin                                                                      | Status                                                                                                                                                                                                                                                                                                                                    |
| --------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| [Alert Plugin](https://github.com/sandpolis/org.s7s.plugin.alert)           | ![Release](https://img.shields.io/github/v/tag/sandpolis/org.s7s.plugin.alert.svg?label=release) [![Build](https://github.com/sandpolis/org.s7s.plugin.alert/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/org.s7s.plugin.alert/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)                |
| [Desktop Plugin](https://github.com/sandpolis/org.s7s.plugin.desktop)       | ![Release](https://img.shields.io/github/v/tag/sandpolis/org.s7s.plugin.desktop.svg?label=release) [![Build](https://github.com/sandpolis/org.s7s.plugin.desktop/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/org.s7s.plugin.desktop/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)          |
| [Shell Plugin](https://github.com/sandpolis/org.s7s.plugin.shell)           | ![Release](https://img.shields.io/github/v/tag/sandpolis/org.s7s.plugin.shell.svg?label=release) [![Build](https://github.com/sandpolis/org.s7s.plugin.shell/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/org.s7s.plugin.shell/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)                |
| [Filesystem Plugin](https://github.com/sandpolis/org.s7s.plugin.filesystem) | ![Release](https://img.shields.io/github/v/tag/sandpolis/org.s7s.plugin.filesystem.svg?label=release) [![Build](https://github.com/sandpolis/org.s7s.plugin.filesystem/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/org.s7s.plugin.filesystem/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |
| [Snapshot Plugin](https://github.com/sandpolis/org.s7s.plugin.snapshot)     | ![Release](https://img.shields.io/github/v/tag/sandpolis/org.s7s.plugin.snapshot.svg?label=release) [![Build](https://github.com/sandpolis/org.s7s.plugin.snapshot/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/org.s7s.plugin.snapshot/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)       |
| [Device Plugin](https://github.com/sandpolis/org.s7s.plugin.device)         | ![Release](https://img.shields.io/github/v/tag/sandpolis/org.s7s.plugin.device.svg?label=release) [![Build](https://github.com/sandpolis/org.s7s.plugin.device/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/org.s7s.plugin.device/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)             |
| [Update Plugin](https://github.com/sandpolis/org.s7s.plugin.update)         | ![Release](https://img.shields.io/github/v/tag/sandpolis/org.s7s.plugin.update.svg?label=release) [![Build](https://github.com/sandpolis/org.s7s.plugin.update/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/org.s7s.plugin.update/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)             |

#### Core Modules

The following library modules define the core of Sandpolis. Instances and
plugins depend on these basic modules which prevents code duplication. You
probably don't need to worry about these unless you're developing plugins.

| Module                                                                      | Status                                                                                                                                                                                                                                                                                                                                    |
| --------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| [core.foundation](https://github.com/sandpolis/org.s7s.core.foundation)     | ![Release](https://img.shields.io/github/v/tag/sandpolis/org.s7s.core.foundation.svg?label=release) [![Build](https://github.com/sandpolis/org.s7s.core.foundation/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/org.s7s.core.foundation/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)       |
| [core.instance](https://github.com/sandpolis/org.s7s.core.instance)         | ![Release](https://img.shields.io/github/v/tag/sandpolis/org.s7s.core.instance.svg?label=release) [![Build](https://github.com/sandpolis/org.s7s.core.instance/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/org.s7s.core.instance/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)             |
| [core.protocol](https://github.com/sandpolis/org.s7s.core.protocol)         | ![Release](https://img.shields.io/github/v/tag/sandpolis/org.s7s.core.protocol.svg?label=release) [![Build](https://github.com/sandpolis/org.s7s.core.protocol/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/org.s7s.core.protocol/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml)             |

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
