## Sandpolis Micro Agent (`com.sandpolis.agent.micro`)

The *micro* agent is a lightweight alternative to `com.sandpolis.agent.mega`. It doesn't support as many features, but it's **small** and **fast**.

#### Linux Prerequisites
Build Protocol Buffer library: 

```sh
# Download release
wget https://github.com/protocolbuffers/protobuf/releases/download/v3.11.3/protobuf-cpp-3.11.3.tar.gz
tar xf protobuf-cpp-3.11.3.tar.gz
cd protobuf-cpp-3.11.3

# Generate makefiles
./configure --disable-shared

# Compile and install
make
sudo make install
```

#### Why provide a C++ agent?
The *micro* agent is designed for embedded systems with low amounts of memory and disk space. It provides barebones functionality in a non-portable package.

#### What platforms are supported?
The *micro* agent is compatible with Linux and Windows on a variety of CPU architectures.

#### What are the resource requirements?
The *micro* agent requires at least 2 MiB of storage space and 3 MiB of free memory.

#### UEFI Mode
Operations like cold snapshots require the device to be booted in a special environment. The micro agent can be installed to the system's ESP to achieve this. The standard agent writes a command to an EFI variable and triggers a reset into the boot agent. If the boot agent receives a command, it executes the command and clears the EFI variable on success. If no command is found, it boots the next entry.
