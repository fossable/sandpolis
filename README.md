<p align="center">
	<img src="https://raw.githubusercontent.com/fossable/sandpolis/master/.github/images/sandpolis-256.png" />
</p>

![License](https://img.shields.io/github/license/fossable/sandpolis)
![GitHub repo size](https://img.shields.io/github/repo-size/fossable/sandpolis)
![Stars](https://img.shields.io/github/stars/fossable/sandpolis?style=social)

<hr>

`sandpolis` is a **virtual estate manager** (VEM) for people with too many
computers.

<p align="center">
	<img src="https://raw.githubusercontent.com/fossable/sandpolis/master/.github/images/overview.png" />
</p>

## Virtual estate

Virtual/digital estate is an encompassing
[term](https://www.fossable.org/fossable/virtual_estate) that refers to all
digital assets under your control. Some assets may be entirely virtual and
mostly controlled by a corporation, like accounts on _github.com_. Others have a
physical component as well, like a server in your closet, Raspberry Pi, or
laptop.

All of these entities are part of your _virtual estate_ and are intricately
connected in both obvious and unapparent ways.

As an example, you might have an SSH key or API token on your machine that
grants access to repositories (a digital asset) on Github. And suppose your
machine also has an authorized key installed that allows access from another
machine:

```
┌──────────┐  SSH Key  ┌──────────┐  API Token  ┌───────────────────┐
│Machine A ┼───────────►Machine B ┼─────────────► Github            │
└──────────┘           └──────────┘             │                   │
                                                │  - Private repos  │
                                                └───────────────────┘
```

This picture represents a simple virtual estate with physical/digital assets
that you have a high degree of control over (local machines), and purely digital
assets that you have very little control over (an online account).

Sandpolis is about mapping out these relations to provide an overall view of
your entire virtual estate. It can do both micro-level management tasks (like:
"give me a shell on Machine A") and macro-level tasks (like: "map out the attack
surface of my Github repos").

### Security Notice

The Sandpolis server is an extremely high-value attack target as it can become a
_single point of compromise_ over your virtual estate. To compensate, strong
security measures are available:

- All connections to a server use mTLS and require a valid client certificate.
  The server automatically rotates these certificates periodically, but the
  initial certificate must be installed out-of-band.

- Users can be required to login with two-factor authentication codes.

- User permissions restrict what users are able to do and on what instances.

Even with several layers of strong security, there's always risk that the server
can be compromised.

You can control how much power Sandpolis has by using _read only_ agents which
still provides useful monitoring information, but prohibits all write operations
(including agent updates). Once an agent enters _read only_ mode, it cannot be
changed back. This can significantly mitigate potential damage in the event of
server compromise.

## How it works

Sandpolis runs an agent on your devices and allows you to interact with them
from a client application. A server mediates client/agent communication and
stores historical data about instances in the network.

## Layers

Features are organized into conceptual _layers_ . If you build Sandpolis from
source, it's easy to pick and choose what layers are included:

```sh
# Build the Sandpolis server with remote desktop capabilities ONLY
cargo build --no-default-features --features server --features layer-desktop
```

If you don't build from source, you can still enable/disable layers, but they
will still be compiled into the final application. If you want to be paranoid
and restrict what your Sandpolis network for improved security, make sure you
build from source.

### Account

Models online/offline accounts and their relationships to agent instances.
Enables higher-order analysis of virtual estate like attack surface mapping and
compromise tracing.

### Audit

Triggers user notifications when certain events are detected in the Sandpolis
network. For example, if a user's status is currently _AWAY_, an unexpected SSH
login from that user (anywhere in the network) will fire an urgent alert.

### Desktop

Provides access to remote desktop capabilities.

### Deploy

Support for deploying agents via SSH.

### Filesystem

Provides read/write access to agent filesystems. The Sandpolis client can also
mount an agent's filesystem with FUSE.

### Inventory

Gather system information. Also integrates with package managers to monitor
software versions. Monitors systemd services.

### Probe

Probes are managable from the Sandpolis network, but don't run agent software.
Instead, a "gateway" agent instance connects to probes over a standard protocol
like SSH, SNMP, Docker, etc.

You can interact with probes almost as if they were regular agents (as long as
the gateway instance remains online).

### Shell

Provides an interactive remote shell on agents (or SSH probes). Also stores
customizable shell "snippets" that can be executed on a schedule.

### Tunnel

Establishes a permanent or ephemeral TCP tunnel between arbitrary instances.

### Snapshot

Create and apply _cold snapshots_ via a boot agent.

## Installation

<details>
<summary>Crates.io</summary>

![Crates.io Total Downloads](https://img.shields.io/crates/d/sandpolis)

#### Install from crates.io

```sh
cargo install sandpolis
```

As an added benefit for this installation method, you can customize exactly what
features you need. For example, to build with support for remote desktop and
nothing else:

```sh
cargo install sandpolis --no-default-features --features layer-desktop
```

As a result, your installation artifacts will be smaller and will be unable to
perform any excluded functionality.

</details>

<details>
<summary>Docker</summary>

#### Install server from DockerHub

![Docker Pulls](https://img.shields.io/docker/pulls/sandpolis/server)
![Docker Image Size](https://img.shields.io/docker/image-size/sandpolis/server)
![Docker Stars](https://img.shields.io/docker/stars/sandpolis/server)

```yml
# Docker compose
services:
  sandpolis-server:
    image: sandpolis/server
    restart: unless-stopped
```

#### Install client from DockerHub

![Docker Pulls](https://img.shields.io/docker/pulls/sandpolis/client)
![Docker Image Size](https://img.shields.io/docker/image-size/sandpolis/client)
![Docker Stars](https://img.shields.io/docker/stars/sandpolis/client)

```sh
alias sandpolis-client="docker run --rm sandpolis/client"
```

</details>
