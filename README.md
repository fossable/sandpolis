<p align="center">
	<img src="screenshots/sandpolis.png" />
</p>

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Build Status](https://travis-ci.org/Subterranean-Security/Sandpolis.svg?branch=master)](https://travis-ci.org/Subterranean-Security/Sandpolis)

**Sandpolis** is a new multi-OS remote administration platform for both serious sysadmins and casual users. It provides real-time monitoring and management capabilities for servers or desktop computers from (eventually) any device. The overall architecture of Sandpolis is coincidentally similar to Canonical's [Landscape service](https://landscape.canonical.com), but Sandpolis is not an open-source imitation of Landscape.

Sandpolis is intended to be the ultimate general purpose administration system. Here are some fundamental qualities that Sandpolis tries to achieve:

- compatible with as many operating systems as possible
- flexible, configurable, and easily extensible to niche applications via plugins
- uncompromising on performance and security
- low latency and high concurrency
- user friendly

More simply, Sandpolis is a convenient means to **securely** interacting with many remote computers from one graphical interface.

### What it isn't
Sandpolis is **not** intended to be used on unauthorized systems, to manage a botnet, or for any other illegal activity. 

There are many _remote administration tools_ out there that claim to be legitimate, but have strange features like ransomware, password stealers, and network flooders. While there may be legitimate use-cases for all of those, the reality is that way more people are going to use them for harm than good. Fortunately, most _remote administration tools_ are so brittle and poorly designed that they are practically useless anyway.

### Why
If you're still unsure of what Sandpolis achieves, consider how you might manage a single remote computer: probably via SSH if it's Linux or Remote Desktop if it's Windows. This paradigm works well for many tasks, but others such as hardware monitoring, long background tasks, and distributed tasks are not well suited to a temporary session. Additionally, different operating systems handle common tasks differently, which requires an effective administrator to have specific knowledge of multiple different platforms. Now multiply these issues by the number of computers you have to see the problem Sandpolis solves.

### How
There are three components to any Sandpolis network: a **client** agent installed on a remote computer, a **viewer** agent that you use to manage clients, and a **server** instance that facilitates communication between the two.  

### Installing
- **Windows**
`// TODO`

- **Arch Linux**
Download the [development package](https://aur.archlinux.org/pkgbase/sandpolis-git) or the [stable package](https://aur.archlinux.org/pkgbase/sandpolis) from the AUR and install with `makepkg -si`. The server can be started with `systemctl start sandpolisd`.

- **Debian/Ubuntu**
`// TODO`

- **MacOS**
`// TODO`

- **Docker**
`// TODO`

### Building and Testing
The build is divided into sets of **instance modules**, **library modules**, and **plugin modules**:

|Module Type| Location | Description|
|-----------|----------|------------|
| Instance Module | `/` | Contains applications like the server or client |
| Library Module | `/dep` | Contains common utilities used by other modules |
| Plugin Module | `/plugin` | Contains a Gradle or Sandpolis plugin |

Building the entire project is as easy as running `./gradlew assemble` from the project's root directory. This will output executables into the `build/libs` directories for each instance module.

Setting up the execution environment for testing can be a burden, so the `com.sandpolis.gradle.deploy` plugin was created to make deploying an instance to any machine possible with a single command. To use it, create an entry in `remote.gradle` for the machine you wish to execute on and run the corresponding Gradle task. For example, `./gradlew :com.sandpolis.server:user@localhost` will deploy the server instance to the local machine via SSH and run it in a `screen` session.

### Sandpolis Cloud
It's 100% free to run your own Sandpolis server and always will be. If you'd rather not maintain another application, or want to take advantage of hyper-scaling in the cloud, we also offer a SaaS-style cloud service at [sandpolis.com](http://sandpolis.com). Sandpolis Cloud gives you access to a private server instance running in the cloud which can be used just like a self-hosted server.

### A Brief History of Sandpolis
Sandpolis evolved out of an immature project called Crimson which was initially released in 2013. Retrospectively, Crimson was an experiment in what kinds of features an administration system can (and should) contain. Overall, Crimson provided valuable experience which heavily informed the development of Sandpolis.

After four years of sporadic development, Crimson was officially abandoned and Sandpolis was created to take its place. The old [repository](https://github.com/Subterranean-Security/Crimson) is now archived for the software archaeologists out there. Although almost none of the Crimson codebase survived into Sandpolis, the overall goal has remained the same: **build the ultimate system management utility**.
