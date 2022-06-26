## Sandpolis Graphical UI Client

_This instance module is a part of
[Sandpolis](https://github.com/sandpolis/sandpolis)._

#### Docker on Linux

To allow the container to access the host's X server, run:

```sh
xhost local:root
```

To run the container:

```sh
docker run -it -v /tmp/.X11-unix/:/tmp/.X11-unix/ -e DISPLAY --net host sandpolis/client/lifegem:debug
```
