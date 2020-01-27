## Sandpolis Micro Client (`com.sandpolis.client.micro`)

The *micro* client is a lightweight alternative to `com.sandpolis.client.mega`. It doesn't support as many features, but it's **small** and **fast**.

```sh
g++ -I/usr/include src/main/cpp/*.cc src/main/cpp/*.c++ /usr/local/lib/libprotobuf-lite.a -pthread
```