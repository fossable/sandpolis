<p align="center">
	<img src="https://s3.us-east-2.amazonaws.com/github.sandpolis.com/header.png" />
</p>

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Build Status](https://travis-ci.org/Subterranean-Security/Sandpolis.svg?branch=master)](https://travis-ci.org/Subterranean-Security/Sandpolis)

**Sandpolis** is a cross-platform remote administration utility for both serious sysadmins and casual users. It provides real-time monitoring and management capabilities for servers, regular desktops, and anything else that runs Java. Simply put, Sandpolis is a convenient means to securely interacting with many remote computers from one application.

Sandpolis is intended to be the ultimate general-purpose administration system. Here are some fundamental qualities it tries to achieve:

- compatible with as many operating systems as possible
- flexible, configurable, and easily extensible to niche applications via plugins
- uncompromising on performance and security
- low latency and high concurrency
- user friendly

### How
There are three components to any Sandpolis network: a **client** agent installed on a remote computer, a **viewer** agent that you directly use to manage clients, and a **server** instance that facilitates communication between the two. For convenience, there are many ways to install the Sandpolis client on your machines.

### Why
Applications like TeamViewer and Canonical's Landscape both offer remote management capabilities, but are on completely opposite sides of the table. Sandpolis is somewhat like a fusion of the two; it's certainly not for customer support on desktop computers, and it's definitely not the best at managing supercomputers, but it's really good at the stuff in-between. If the middle-ground is what you seek, Sandpolis may be just the application for you.

### What it isn't
Sandpolis is **not** intended to be used on unauthorized systems, to manage a botnet, or for any other illegal activity. 

There are many _remote administration tools_ out there that claim to be for legitimate use, but have strange features like ransomware, password stealers, and network flooders. While there may be a legitimate use-case for all of those, the reality is that more people are going to use those features for harm than for good. Fortunately, most _remote administration tools_ are so brittle and poorly designed that they are practically useless anyway.

### Installing
- **Windows**
`// TODO`

- **Arch Linux**
    - Download the [development package](https://aur.archlinux.org/pkgbase/sandpolis-git) or the [stable package](https://aur.archlinux.org/pkgbase/sandpolis) from the AUR
    - Extract and install with `makepkg -si`
    - If you installed the server too, it can be started with `systemctl start sandpolisd`

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
It's 100% free to run your own Sandpolis server and always will be. If you'd rather not maintain another application, or want to take advantage of mindless hyper-scaling in the cloud, we also offer a SaaS-style cloud service at [sandpolis.com](http://sandpolis.com). Sandpolis Cloud gives you access to a private server instance running in the cloud which can be used just like a self-hosted server. Support included!

### A Brief History of Sandpolis
Sandpolis evolved out of an immature project called Crimson which was initially released in 2013. Retrospectively, Crimson was an experiment in what kinds of features an administration system can (and should) contain. Overall, Crimson provided valuable experience which heavily informed the development of Sandpolis.

After four years of sporadic development, Crimson was officially abandoned and Sandpolis was created to take its place. The old [repository](https://github.com/Subterranean-Security/Crimson) is now archived for the software archaeologists out there. Although almost none of the Crimson codebase survived into Sandpolis, the overall goal has remained the same: **build the ultimate system management utility**.
