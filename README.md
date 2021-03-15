<p align="center">
	<img src="https://s3.us-east-2.amazonaws.com/github.sandpolis.com/header.png" />
</p>

**Sandpolis** is a real-time distributed administration platform for servers, desktop computers, embedded devices, and anything in-between.

**This project is unfinished and should only be used in a secure testing environment!**

### Introduction
The vision for Sandpolis is to build the ultimate no-expense-spared administration system that:

- provides full control over any device :computer:,
- is crazy fast :zap:,
- scales to thousands of devices :boom:,
- and gives you money :moneybag:.

Maybe not that last one, but it won't cost you anything because it's free in terms of cost and, more importantly, **free as in freedom**.

This project has existed in some form since 2013 and has always been [a solo endeavour](https://github.com/cilki). It's already unbelievable that it's come this far, but maybe it will be fully functional by 2023?

#### Instances
Sandpolis is not just one program, but a set of several working together. There are three catagories in which every Sandpolis component (or "instance") belongs:

- :computer: an **agent** installed on a remote system that carries out administration tasks on behalf of users
- :iphone: a **client** application that users can use to interact with agents
- :lock: a **server** that facilitates communication between instances in the network and makes everything "work"

For end-user convenience (or confusion), there are multiple official programs in each category. For example, you can login to a Sandpolis server with either the [Desktop Client](https://github.com/sandpolis/com.sandpolis.client.lifegem) or from a mobile device with the [iOS Client](https://github.com/sandpolis/com.sandpolis.client.lockstone).

| Instance | Status |
|----------|--------|
| [Server](https://github.com/sandpolis/com.sandpolis.server.vanilla) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.server.vanilla.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.server.vanilla/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.server.vanilla/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |
| [iOS Client](https://github.com/sandpolis/com.sandpolis.client.lockstone) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.client.lockstone.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.client.lockstone/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.client.lockstone/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |
| [Desktop Client](https://github.com/sandpolis/com.sandpolis.client.lifegem) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.client.lifegem.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.client.lifegem/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.client.lifegem/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |
| [Terminal Client](https://github.com/sandpolis/com.sandpolis.client.ascetic) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.client.ascetic.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.client.ascetic/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.client.ascetic/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |
| [Agent](https://github.com/sandpolis/com.sandpolis.agent.vanilla) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.agent.vanilla.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.agent.vanilla/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.agent.vanilla/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |
| [Native Agent](https://github.com/sandpolis/com.sandpolis.agent.micro) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.agent.micro.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.agent.micro/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.agent.micro/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |
| [Minimal Native Agent](https://github.com/sandpolis/com.sandpolis.agent.nano) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.agent.nano.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.agent.nano/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.agent.nano/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |

#### Plugins
Sandpolis supports plugins as a first-class feature. Essentially *all* end-user functionality in Sandpolis (file transfers, remote desktop, etc) is implemented as a plugin. This allows users to choose exactly what features they want and ignore the rest. Third-party plugins can also be installed, but must be signed with a trusted code-signing certificate.

The following plugins are officially supported:

| Plugin | Status |
|--------|--------|
| [Desktop Plugin](https://github.com/sandpolis/com.sandpolis.plugin.desktop) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.plugin.desktop.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.plugin.desktop/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.plugin.desktop/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |
| [Shell Plugin](https://github.com/sandpolis/com.sandpolis.plugin.shell) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.plugin.shell.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.plugin.shell/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.plugin.shell/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |
| [Filesystem Plugin](https://github.com/sandpolis/com.sandpolis.plugin.filesystem) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.plugin.filesystem.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.plugin.filesystem/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.plugin.filesystem/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |
| [Snapshot Plugin](https://github.com/sandpolis/com.sandpolis.plugin.snapshot) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.plugin.snapshot.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.plugin.snapshot/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.plugin.snapshot/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |
| [Device Plugin](https://github.com/sandpolis/com.sandpolis.plugin.device) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.plugin.device.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.plugin.device/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.plugin.device/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |

#### Core Modules
The following library modules define the core of Sandpolis. Instances and plugins depend on these basic modules which prevents code duplication. You probably don't need to worry about these unless you're developing plugins.

| Module | Status |
|--------|--------|
| [core.agent](https://github.com/sandpolis/com.sandpolis.core.agent) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.core.agent.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.core.agent/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.core.agent/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |
| [core.client](https://github.com/sandpolis/com.sandpolis.core.client) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.core.client.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.core.client/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.core.client/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |
| [core.clientagent](https://github.com/sandpolis/com.sandpolis.core.clientagent) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.core.clientagent.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.core.clientagent/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.core.clientagent/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |
| [core.clientserver](https://github.com/sandpolis/com.sandpolis.core.clientserver) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.core.clientserver.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.core.clientserver/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.core.clientserver/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |
| [core.foundation](https://github.com/sandpolis/com.sandpolis.core.foundation) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.core.foundation.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.core.foundation/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.core.foundation/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |
| [core.instance](https://github.com/sandpolis/com.sandpolis.core.instance) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.core.instance.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.core.instance/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.core.instance/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |
| [core.net](https://github.com/sandpolis/com.sandpolis.core.net) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.core.net.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.core.net/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.core.net/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |
| [core.server](https://github.com/sandpolis/com.sandpolis.core.server) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.core.server.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.core.server/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.core.server/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |
| [core.serveragent](https://github.com/sandpolis/com.sandpolis.core.serveragent) | ![Release](https://img.shields.io/github/v/tag/sandpolis/com.sandpolis.core.serveragent.svg?label=release) [![Build](https://github.com/sandpolis/com.sandpolis.core.serveragent/workflows/.github/workflows/build.yml/badge.svg)](https://github.com/sandpolis/com.sandpolis.core.serveragent/actions?query=workflow%3A.github%2Fworkflows%2Fbuild.yml) |

### Installing
You could install Sandpolis now, but it's likely to be an underwhelming experience. Once things are more stable, installation instructions will appear out of thin air.

In the meantime, here's a screenshot to admire of the iOS client back when it was actually functional:
<p align="center">
	<img src="https://s3.us-east-2.amazonaws.com/github.sandpolis.com/client/lockstone/triptych.png" />
</p>

### Building
Since there are many complex components in Sandpolis, you'll usually only want to build a small subset them. To choose what components to build, initialize the desired submodules:
```sh
# Sandpolis uses git submodules extensively
git submodule update --init com.sandpolis.server.vanilla
git submodule update --init module
```

Since Sandpolis is built with Gradle, building the code is the easy part:
```sh
# Just make sure you have JDK 16 first
./gradlew build
```

#### Using Vagrant
In order to build components for other operating systems, Vagrant can be used:

```sh
vagrant up linux
vagrant ssh linux
```
