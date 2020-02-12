## Sandpolis Micro Client (`com.sandpolis.client.micro`)

The *micro* client is a lightweight alternative to `com.sandpolis.client.mega`. It doesn't support as many features, but it's **small** and **fast**.

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