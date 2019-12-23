<p align="center">
	<img src="https://s3.us-east-2.amazonaws.com/github.sandpolis.com/header.png" />
</p>

![GitHub release](https://img.shields.io/github/release-pre/Subterranean-Security/Sandpolis.svg?color=blue)
[![Build Status](https://travis-ci.org/Subterranean-Security/Sandpolis.svg?branch=master)](https://travis-ci.org/Subterranean-Security/Sandpolis)
[![Build status](https://ci.appveyor.com/api/projects/status/8a2xdoy8pt21k77g?svg=true)](https://ci.appveyor.com/project/cilki/sandpolis)

**Sandpolis** is a remote administration platform for servers, desktop computers, and anything in-between.

Sandpolis is designed for sysadmins and enthusiasts, but can be used by anyone once set up. Most users will use Sandpolis via the JavaFX desktop application or the [iOS mobile application](https://apps.apple.com/us/app/sandpolis/id1478068506).

:zap: **Sandpolis is unfinished and therefore should only be used in a secure testing environment!** :zap:

### How it Works
There are three components in any Sandpolis network:

- a **client** agent installed on a remote system
- a **viewer** application that you use to manage client systems
- a **server** that facilitates communication between clients and viewers

Clients maintain a persistent connection to the server, and viewers maintain a temporary connection to the server or directly to clients. This critically allows tasks to continue running in the background even after a viewer has logged out.

All user-functionality in Sandpolis (file management, remote desktop, etc) is implemented through plugins. Third-party plugins can also be installed, but require the publisher's plugin certificate to be trusted first. 

For convenience, there are a few ways to install the Sandpolis client on your machines:
- The simplest method is to generate an executable **.jar** file from the viewer interface and manually run it on each remote machine. Other output formats are also available like **.py** and **.sh**. 
- The client can also be installed from SandpolisInstaller by scanning a QR code with the [Sandpolis iOS application](https://apps.apple.com/us/app/sandpolis/id1478068506). 

Once installed, the client agent can automatically update itself independently of the server.

### Installing
- **Windows**/**MacOS**/**Linux**
    - Download the latest [Sandpolis Installer](https://sandpolis.com/download) for your operating system
    - Start the installer by running `java -jar Downloads/SandpolisInstaller-win-6.0.0.jar`

- **Arch Linux**
    - Download the [development package](https://aur.archlinux.org/pkgbase/sandpolis-git) or the [stable package](https://aur.archlinux.org/pkgbase/sandpolis) from the AUR
    - Extract and install with `makepkg -si`
    - The server daemon can be started with `systemctl start sandpolisd`

- **Docker**
    - The server can be started with: `sudo docker run sandpolis/sandpolis-server`
    - See [DockerHub](https://hub.docker.com/r/sandpolis/sandpolis-server) for more information

### Building and Testing
The project is divided into sets of **instance modules**, **library modules**, and **plugin modules**:

| Location | Description|
|-----------|------------|
| `/` | Instance modules like the server or client |
| `/module` | Common library modules |
| `/plugin` | Sandpolis plugins |
| `/gradle` | Gradle plugins |

Building the entire project is as easy as running `./gradlew assemble` from the project's root directory. This will output (almost ready-to-run) executables into the `build/libs` directories of each instance module.

Setting up the execution environment for integration testing can be a burden, so the `com.sandpolis.gradle.deploy` plugin was created to make deploying an instance to any machine possible with a single command. To use it, create an entry in `remote.gradle` for the machine you wish to execute on and run the corresponding Gradle task.

##### Example: deploying the server instance to the local machine
- Edit `remote.gradle` and add an entry describing the SSH information for the test machine
- Run `./gradlew :com.sandpolis.server.vanilla:user@localhost` to build the server instance and deploy
- To open the instance logs once deployed, run `screen -r com.sandpolis.server.vanilla` to attach to the session

The unit tests can be run with `./gradlew test`. 

### Why Build Another Remote Administration Utility 
Sandpolis is somewhat like a fusion of [TeamViewer](https://www.teamviewer.com) and Canonical's [Landscape](https://landscape.canonical.com). The overall architecture and usage of Sandpolis is very similar to Landscape, but Sandpolis also provides features that you would find in TeamViewer (like remote desktop). Sandpolis isn't intended to replace either TeamViewer or Landscape, but rather it serves the middle-ground between the two.

Here are some fundamental objectives that Sandpolis tries to achieve:

- compatible with as many operating systems as possible
- flexible, configurable, and easily extensible to niche applications via plugins
- uncompromising on performance and security
- low latency and high concurrency
- user friendly

### A Brief History of Sandpolis
Sandpolis evolved out of a similar application called Crimson which was initially released in 2013. After four years of sporadic development, Crimson was finally abandoned and Sandpolis was created to take its place. The old [repository](https://github.com/Subterranean-Security/Crimson) is now archived for the software archaeologists out there. Although none of the Crimson codebase survived into Sandpolis, the goal remains the same: **build the ultimate system management utility**.
