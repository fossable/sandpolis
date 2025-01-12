<p align="center">
	<img src="https://raw.githubusercontent.com/fossable/sandpolis/master/.github/images/sandpolis-256.png" />
</p>

![License](https://img.shields.io/github/license/fossable/sandpolis)
![GitHub repo size](https://img.shields.io/github/repo-size/fossable/sandpolis)
![Stars](https://img.shields.io/github/stars/fossable/sandpolis?style=social)

<hr>

`sandpolis` is a **virtual estate monitoring/management tool** under active
development.

<p align="center">
	<img src="https://raw.githubusercontent.com/fossable/sandpolis/master/.github/images/overview.png" />
</p>

#### Partially virtual

Sandpolis originally focused on monitoring/management of devices that you
physically own.

#### Purely virtual

Some parts of your virtual estate have no associated physical entity, for
example:

- VPS cloud hosting
- User accounts with online services

Sandpolis also manages these components.

## Security Warning

Sandpolis is an extremely high-value attack target as it provides management
access to your virtual estate. To compensate, strong security measures are
available:

- All connections to a server use mTLS and require a valid client certificate.
  The server automatically rotates these certificates periodically, but the
  initial certificate must be installed out-of-band.

- Users can be required to login with two-factor authentication codes.

- User permissions can restrict what users are able to do and on what instances.

- Agents can optionally run in _read only_ mode which still provides useful
  information, but prohibits all write operations. This can significantly
  mitigate potential damage in the event of server compromise.

Even with several layers of strong authentication, there's always risk that the
Sandpolis server can be compromised. If the risks of "single point of
compromise" outweigh the convenience of having a unified management interface,
then **don't use Sandpolis**.

## Layers

Features are organized into _layers_ that can be toggled on/off in the UI.

### Account

### Alert

Triggers user notifications when certain events are detected in the Sandpolis
network. For example, if a user's status is currently _AWAY_, an unexpected SSH
login from that user (anywhere in the network) will fire an urgent alert.

### Desktop

Provides access to remote desktop capabilities.

### Filesystem

Provides read/write access to agent filesystems. The Sandpolis client can also
mount a remote filesystem.

### Logging

### Package

Integrates with the package manager on agents to manages package versions.

### Probe

Probes are managable from the Sandpolis network, but don't run agent software.
Instead, a remote Sandpolis agent instance connects to probes over a standard
protocol like SSH, SNMP, Docker, etc.

### Shell

Provides an interactive remote shell.

### Tunnel

### User
