<p align="center">
	<img src="https://s3.us-east-2.amazonaws.com/github.sandpolis.com/header.png" />
</p>

![GitHub release](https://img.shields.io/github/release-pre/Subterranean-Security/Sandpolis.svg?color=blue)
[![Build Status](https://travis-ci.org/Subterranean-Security/Sandpolis.svg?branch=master)](https://travis-ci.org/Subterranean-Security/Sandpolis)
[![Build status](https://ci.appveyor.com/api/projects/status/8a2xdoy8pt21k77g?svg=true)](https://ci.appveyor.com/project/cilki/sandpolis)

**Sandpolis** is a remote administration platform for servers, desktop computers, embedded devices, and anything in-between. Although designed primarily for sysadmins and enthusiasts, it should also be usable by the average hominin capable of reaching this page.

:zap: **This project is unfinished and should only be used in a secure testing environment!** :zap:

### Introduction
Sandpolis is a real-time distributed application composed of three types of components:

- an **agent** installed on remote systems that carries out tasks on behalf of users
- a **client** application that users interact with
- a **server** that facilitates communication between instances in the network

In a typical setup, the server is hosted by a cloud provider like AWS or GCP, the client is installed on the administrator's machine, and the agent is installed on a large number of machines that need to be monitored/controlled.

##### Plugins
Sandpolis supports plugins as a first-class feature. In fact, *all* end-user functionality in Sandpolis (file transfers, remote desktop, etc) is implemented through plugins. Third-party plugins can also be installed, but must be signed with a trusted code-signing certificate.

##### Widely compatible
Sandpolis works on many different operating systems and CPU architectures thanks to the JVM. It can also be used to monitor systems with minimal resources via the native agent written in C++.

##### Low latency, high concurrency
Sandpolis is a real-time application which leads to a more satisfying user experience. Data is available right away and operations happen immediately.

To scale effectively, Sandpolis can utilize multiple servers in the same network which enables a large number of total concurrent connections from agents.

##### Uncompromising on performance and security
Every reasonable measure has been taken to ensure Sandpolis is both secure and performant. When it's not possible to achieve every desirable design objective, these two are prioritized.

### Installation
- **Windows**/**MacOS**/**Linux**
    - Install Java 14 or later
    - Download the latest [Sandpolis Installer](https://sandpolis.com/download) for your operating system
    - Start the installer by running `java -jar SandpolisInstaller-*.jar`

- **Arch Linux**
    - Download the [development package](https://aur.archlinux.org/pkgbase/sandpolis-git) or the [stable package](https://aur.archlinux.org/pkgbase/sandpolis) from the AUR
    - Extract and install with `makepkg -si`
    - The server daemon can be started with `systemctl start sandpolisd`

- **Docker**
    - The server can be installed from [DockerHub](https://hub.docker.com/r/sandpolis/sandpolis-server): `docker run -d -p 8768:8768 --name=sandpolis --restart=always sandpolis/sandpolis-server`

### Building and Testing
The project is divided into groups of **instance modules**, **library modules**, and **plugin modules**:

| Location | Description|
|-----------|------------|
| `/` | Instance modules |
| `/module` | Common library modules |
| `/plugin` | Sandpolis plugins |
| `/gradle` | Gradle plugins |

Building the entire project is as easy as running `./gradlew assemble` from the project's root directory. This will output (almost ready-to-run) executables into the `build/libs` directories of each instance module. Running the unit tests can be accomplished with `./gradew test`.
