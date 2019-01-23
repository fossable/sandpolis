<p align="center">
	<img src="screenshots/sandpolis.png" />
</p>

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Build Status](https://travis-ci.org/Subterranean-Security/Sandpolis.svg?branch=master)](https://travis-ci.org/Subterranean-Security/Sandpolis)

**Sandpolis** is a remote administration platform for both casual sysadmins and power users that is compatible with Linux, MacOS, BSD, and Windows. It provides real-time monitoring and management of servers or desktop computers while remaining usuable by the average user without compromising on features for serious administrators. The overall architecture of Sandpolis is similar to Canonical's [Landscape](https://landscape.canonical.com) and some features are common between the two.

Sandpolis is intended to be the ultimate general purpose administration system. This means it must meet the following criteria:

- compatible with most operating systems
- feature rich
- low latency and high concurrency
- flexible and configurable
- easily extensible to niche applications via plugins

Essentially, Sandpolis is a means to securely monitoring and interacting with many remote computers from one place.

### What it isn't
Sandpolis is **not** intended to be used on unauthorized systems, to manage a botnet, or any other illegal activity. To reduce the temptation to use Sandpolis in this way, features are only added if their legitimate use cases significantly outweigh their illegitimate ones. Unfortunately, the nature of dual-use technology like remote administration is that eliminating all malicious use cases also eliminates all legitimate usability.

Since some may try anyway, there are some obstacles that reduce the feasibility of using Sandpolis on unauthorized systems:

- The client must be installed to the filesystem and requires user confirmation
- The client process is not hidden or obfuscated in any way

### Why
If you're still unsure of what Sandpolis achieves, consider how you might manage a single remote computer: probably via SSH if it's Linux or Remote Desktop if it's Windows. This paradigm works well for many tasks, but others such as hardware monitoring or long background tasks are not well suited to a temporary session. Additionally, different operating systems handle common tasks differently, which requires an effective administrator to have specific knowledge of multiple different platforms. Now multiply these issues by the number of computers you need to manage to see the problem Sandpolis solves.

### Building and Running Sandpolis
The build is divided into sets of **instance modules**, **library** modules, and **plugin** modules:

|Module Type| Location | Description|
|-----------|----------|------------|
| **Instance Module** | `/` | Contains applications like the server or client |
| **Library Module** | `/dep` | Contains common utilities used by other modules |
| **Plugin Module** | `/plugin` | Contains a Gradle or Sandpolis plugin |

Building the entire project is as easy as running `./gradlew assemble` from the project's root directory. This will output executables into the `build/libs` directories for each instance module.

Setting up the execution environment for testing can be a burden, so the `com.sandpolis.gradle.deploy` plugin was created to make deploying an instance to any machine possible with a single command. To use it, create an entry in `remote.gradle` for the machine you wish to execute on and run the corresponding Gradle task. For example, `./gradlew :com.sandpolis.server:user@localhost` will deploy the server instance to the local machine via SSH and run it in a `screen` session as _user_.

### Sandpolis Cloud
It's 100% free to run your own Sandpolis server and always will be. If you'd rather not maintain another application, or want to take advantage of hyper-scaling in the cloud, we also offer a SaaS-style cloud service at [sandpolis.com](http://sandpolis.com). Sandpolis Cloud gives you access to a private server instance running in the cloud which can be used just like a self-hosted server.

### A Brief History of Sandpolis
Sandpolis evolved out of an immature project called Crimson which was initially released in 2013. Retrospectively, Crimson was an experiment in what kinds of features an administration system can (and should) contain. Overall, Crimson provided valuable experience which heavily informed the development of Sandpolis.

After four years of sporadic development, Crimson was officially abandoned and Sandpolis was created to take its place. The old [repository](https://github.com/Subterranean-Security/Crimson) is now archived for the software archaeologists out there. Although almost none of the Crimson codebase survived into Sandpolis, the overall goal has remained the same: **build the ultimate administration utility**.
