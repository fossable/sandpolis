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

Virtual/digital estate is an all-encompassing term that generally refers to all
of the (non-physical) assets in your possession. Some of them may be entirely
virtual and controlled by a corporation, like accounts on _github.com_. Others
have a physical component as well, like a server in your closet, Raspberry Pi,
or laptop.

All of these entities are part of your _virtual estate_ and are intricately
connected in various ways. As an example, you might have an SSH key or API token
on your machine that grants access to repositories (a kind of digital asset) on
Github. And suppose your machine also has an authorized key installed that
allows access from another machine:

```
┌──────────┐  SSH Key  ┌──────────┐  API Token  ┌───────────────────┐
│Machine A ┼───────────►Machine B ┼─────────────► Github            │
└──────────┘           └──────────┘             │                   │
                                                │  - Private repos  │
                                                └───────────────────┘
```

If those private repos are worth protecting, then Sandpolis can map out an
attack surface that includes both `Machine A` and `Machine B`. If `Machine A`
happens to have a weak password or one that's shared with another website, then
the attack surface is consequently expanded with appropriate probabilities.

### Why sovereignty over your virtual estate is important

With virtual estates, it's not always clear who is in control of what and
exactly how much control they have.

Control is usually divided among multiple parties. Some examples:

- Control of your Github repos (a digital asset) is shared between you, Github
  Inc, and probably Microsoft too.

- Control of your iPhone (a physical asset) is shared between you and Apple.
  Arguably, Apple has more control of it than you do.

- Control of your password manager (a digital asset) is hopefully not shared
  with anyone. SaaS password managers maintain a minimal level of control even
  with proper end-to-end encryption.

While it's impossible to know exactly what percentage of control we have over
our virtual estates, it definitely seems to be trending down. People are willing
to give up control for features and convenience, and they didn't seem to notice
the tradeoff.

If we don't prioritize control of our virtual estates, we might eventually lose
it altogether. Imagine an alternate reality where all we have are thin clients!

Sandpolis is a tool designed to help realize as much sovereignty over your
virtual estate as possible. It provides total control over the devices you own
(via a local agent process) and visualizes how those devices interact with parts
of your virtual estate that you don't directly own (such as cloud services).

There's no going back to the level of computing sovereignty we had before the
Internet, but we don't want the opposite extreme either. We want a healthy
medium of _partial sovereignty_ where people are aware and accepting of the
control tradeoffs they are making.

## Security Warning

The Sandpolis server is an extremely high-value attack target as it provides
management capabilities over your virtual estate. To compensate, strong security
measures are available:

- All connections to a server use mTLS and require a valid client certificate.
  The server automatically rotates these certificates periodically, but the
  initial certificate must be installed out-of-band.

- Users can be required to login with two-factor authentication codes.

- User permissions restrict what users are able to do and on what instances.

Even with several layers of strong authentication, there's always risk that the
server can be compromised. If the risks of introducing a _single point of
compromise_ outweigh the convenience of having a unified management interface,
then **you don't have to use Sandpolis**.

You can choose how much trust you allocate to the Sandpolis network. For
example, agents can optionally run in _read only_ mode which still provides
useful monitoring information, but prohibits all write operations (including
agent updates). This can significantly mitigate potential damage in the event of
server compromise.

## How it works

Sandpolis runs an agent on your devices and allows you to interact with them
from a client application. A server mediates client/agent communication and
stores historical data about instances in the network.

## Layers

Features are organized into _layers_ that can be toggled on/off in the UI. If
you build Sandpolis from source, it's also easy to pick and choose what layers
are included:

```sh
# Build the Sandpolis server with remote desktop capabilities ONLY
cargo build --no-default-features --features server --features layer-desktop
```

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

### Filesystem

Provides read/write access to agent filesystems. The Sandpolis client can also
mount an agent's filesystem with FUSE.

### Package

Integrates with package managers to monitor software versions.

### Probe

Probes are managable from the Sandpolis network, but don't run agent software.
Instead, a remote Sandpolis agent instance connects to probes over a standard
protocol like SSH, SNMP, Docker, etc.

You can interact with probes almost as if they were regular agents (as long as
the gateway instance remains online).

### Shell

Provides an interactive remote shell. Also stores customizable shell "snippets"
that can be executed on a schedule.

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
