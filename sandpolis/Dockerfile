# syntax=docker/dockerfile:1.7
#
# Every Sandpolis image, built from one nix toolchain so a container links
# against exactly what `nix-shell shell.nix` gives the dev loop:
#
#   docker build --target server -f sandpolis/Dockerfile -t sandpolis/server .
#   docker build --target agent  -f sandpolis/Dockerfile -t sandpolis/agent  .
#   docker build --target client -f sandpolis/Dockerfile -t sandpolis/client .
#   docker build --target demo   -f sandpolis/Dockerfile -t sandpolis/demo   .
#
# The build context is the repository root, not this directory: `build.rs` walks
# every workspace member.

FROM nixos/nix:2.35.2 AS nix-base

# The nix sandbox wants user namespaces, which are unreliable under BuildKit, and
# the derivations here are all fetched or built the same either way.
RUN printf '%s\n' \
      'sandbox = false' \
      'filter-syscalls = false' \
      'max-jobs = auto' \
      'cores = 0' \
    >>/etc/nix/nix.conf

# Assemble the rootfs of one instance's image. Written once here rather than
# repeated per instance below.
RUN <<'INSTALL'
set -eu
cat >/usr/local/bin/mkrootfs <<'MKROOTFS'
#!/bin/sh
set -eu

instance="${1:?usage: mkrootfs <instance>}"
binary=/out/bin/sandpolis

env_path="$(nix-build --no-out-link /work/nix/runtime.nix -A "$instance")"

# The binary names absolute store paths — its ELF interpreter and every RUNPATH
# entry — so those have to come along or it cannot exec at all. Scanning it is
# what keeps this honest as dependencies change. mkShell's own outputs are named
# in RUNPATH too but never realized, hence the existence test.
linked=""
for path in $(grep -aoE '/nix/store/[a-z0-9]{32}-[+._?=a-zA-Z0-9-]+' "$binary" | sort -u); do
    if [ -e "$path" ]; then
        linked="$linked $path"
    fi
done

# The env's own closure covers what scanning cannot see: libraries opened with
# dlopen (vulkan, GL, the X11 libraries, libxdo) and data files (vulkan ICD
# manifests, xkb keymaps, gstreamer plugins, the CA bundle).
mkdir -p /rootfs/nix/store
nix-store -qR $linked "$env_path" >/tmp/closure
while read -r path; do
    cp -a --parents -t /rootfs "$path"
done </tmp/closure
ln -s "$env_path" /rootfs/runtime

install -Dm755 "$binary" /rootfs/bin/sandpolis
ln -s /runtime/bin/bash /rootfs/bin/sh

mkdir -p /rootfs/etc/ssl/certs /rootfs/data
mkdir -m 1777 -p /rootfs/tmp
ln -s /runtime/etc/ssl/certs/ca-bundle.crt /rootfs/etc/ssl/certs/ca-certificates.crt
printf 'root:x:0:0:root:/root:/bin/sh\nnobody:x:65534:65534:nobody:/:/bin/sh\n' >/rootfs/etc/passwd
printf 'root:x:0:\nnogroup:x:65534:\n' >/rootfs/etc/group
printf 'hosts: files dns\n' >/rootfs/etc/nsswitch.conf
printf 'NAME="Sandpolis"\nID=sandpolis\nPRETTY_NAME="Sandpolis Container"\n' >/rootfs/etc/os-release

# Bind mountpoints the runtime fills in.
: >/rootfs/etc/hosts
: >/rootfs/etc/resolv.conf
: >/rootfs/etc/hostname

# Fail the build rather than ship an image that cannot start: this is the whole
# rootfs, so if the binary runs here it runs in the image.
chroot /rootfs /bin/sandpolis --version
MKROOTFS
chmod +x /usr/local/bin/mkrootfs
sh -n /usr/local/bin/mkrootfs
INSTALL

# Realize the toolchain before the source arrives, so editing code doesn't
# re-download several gigabytes of nixpkgs.
FROM nix-base AS deps
WORKDIR /build
COPY nix/ ./nix/
COPY shell.nix ./
RUN nix-shell shell.nix --run true

FROM deps AS build-server
COPY . .
ARG TARGETPLATFORM
RUN --mount=type=cache,id=sandpolis-registry-${TARGETPLATFORM},sharing=locked,target=/root/.cargo/registry \
    --mount=type=cache,id=sandpolis-git-${TARGETPLATFORM},sharing=locked,target=/root/.cargo/git \
    --mount=type=cache,id=sandpolis-target-server-${TARGETPLATFORM},sharing=locked,target=/build/target \
    nix-shell shell.nix --run "cargo build --release --bin sandpolis --features server" \
    && install -Dm755 target/release/sandpolis /out/bin/sandpolis

FROM deps AS build-agent
COPY . .
ARG TARGETPLATFORM
RUN --mount=type=cache,id=sandpolis-registry-${TARGETPLATFORM},sharing=locked,target=/root/.cargo/registry \
    --mount=type=cache,id=sandpolis-git-${TARGETPLATFORM},sharing=locked,target=/root/.cargo/git \
    --mount=type=cache,id=sandpolis-target-agent-${TARGETPLATFORM},sharing=locked,target=/build/target \
    nix-shell shell.nix --run "cargo build --release --bin sandpolis --features agent" \
    && install -Dm755 target/release/sandpolis /out/bin/sandpolis

FROM deps AS build-client
COPY . .
ARG TARGETPLATFORM
RUN --mount=type=cache,id=sandpolis-registry-${TARGETPLATFORM},sharing=locked,target=/root/.cargo/registry \
    --mount=type=cache,id=sandpolis-git-${TARGETPLATFORM},sharing=locked,target=/root/.cargo/git \
    --mount=type=cache,id=sandpolis-target-client-${TARGETPLATFORM},sharing=locked,target=/build/target \
    nix-shell shell.nix --run "cargo build --release --bin sandpolis --features client" \
    && install -Dm755 target/release/sandpolis /out/bin/sandpolis

# One binary that can be started as any of the three.
FROM deps AS build-demo
COPY . .
ARG TARGETPLATFORM
RUN --mount=type=cache,id=sandpolis-registry-${TARGETPLATFORM},sharing=locked,target=/root/.cargo/registry \
    --mount=type=cache,id=sandpolis-git-${TARGETPLATFORM},sharing=locked,target=/root/.cargo/git \
    --mount=type=cache,id=sandpolis-target-demo-${TARGETPLATFORM},sharing=locked,target=/build/target \
    nix-shell shell.nix --run "cargo build --release --bin sandpolis --all-features" \
    && install -Dm755 target/release/sandpolis /out/bin/sandpolis

FROM nix-base AS rootfs-server
COPY nix/ /work/nix/
COPY --from=build-server /out/bin/sandpolis /out/bin/sandpolis
RUN mkrootfs server

FROM nix-base AS rootfs-agent
COPY nix/ /work/nix/
COPY --from=build-agent /out/bin/sandpolis /out/bin/sandpolis
RUN mkrootfs agent

FROM nix-base AS rootfs-client
COPY nix/ /work/nix/
COPY --from=build-client /out/bin/sandpolis /out/bin/sandpolis
RUN mkrootfs client

FROM nix-base AS rootfs-demo
COPY nix/ /work/nix/
COPY --from=build-demo /out/bin/sandpolis /out/bin/sandpolis
RUN mkrootfs demo

# Bring up a server, an agent and the GUI client in one container. A process is
# exactly one instance, so this is three of them around one binary.
RUN <<'INSTALL'
set -eu
cat >/rootfs/bin/sandpolis-demo <<'DEMO'
# Started by the image's entrypoint as `bash /bin/sandpolis-demo`; there is no
# /usr/bin/env here for a shebang to use.
set -u

data="${SANDPOLIS_DEMO_DATA:-/data}"
export HOME=/tmp

# Each instance opens <data>/default.db under an exclusive lock, so they can't
# share a directory. --data is passed explicitly for the same reason: all three
# subcommands read $S7S_DATA alike, and one value would collide.
mkdir -p "$data/server" "$data/agent" "$data/client"

pids=""
shutdown() {
    trap - TERM INT EXIT
    [ -n "$pids" ] && kill $pids 2>/dev/null
    wait 2>/dev/null
}
trap shutdown TERM INT EXIT

# The server's log is captured to a file (the admin password below is only ever
# written there) and streamed on to the container's stderr by a follower, which
# keeps $! the server's own pid rather than some pipeline member's.
log=/tmp/server.log
: >"$log"
sandpolis server --data "$data/server" --listen 0.0.0.0:8768 2>>"$log" &
pids="$pids $!"
tail -n +1 -f "$log" >&2 &
pids="$pids $!"

# The server mints a realm cert before it starts listening, and the agent and
# client both retry with backoff, so the file appearing is all the readiness
# needed. Its common name falls back to 127.0.0.1:<listen port>, which is where
# the other two are.
realm="$data/server/default.realm.pem"
for _ in $(seq 1 600); do
    [ -s "$realm" ] && break
    sleep 0.1
done
if [ ! -s "$realm" ]; then
    echo "demo: the server never wrote $realm" >&2
    exit 1
fi

# The admin password is generated once and only ever logged, so surface it here
# instead of making someone go find it.
credentials="$(grep -m1 'Created default admin user' "$log")"
password="${credentials##*password=}"
password="${password%% *}"
echo "=================================================================="
if [ -n "$credentials" ]; then
    echo " Sandpolis demo — log in as admin / $password"
else
    echo " Sandpolis demo — reusing $data, whose admin password was printed"
    echo " the first time this volume was used and is not logged again."
fi
echo "=================================================================="

sandpolis agent --data "$data/agent" --realm "$realm" &
pids="$pids $!"

# The GUI needs a compositor handed in from outside the container.
socket="${WAYLAND_DISPLAY:-}"
case "$socket" in
    "" | /*) ;;
    *) socket="${XDG_RUNTIME_DIR:-/run/user/1000}/$socket" ;;
esac

if { [ -n "$socket" ] && [ -S "$socket" ]; } || [ -n "${DISPLAY:-}" ]; then
    sandpolis client --data "$data/client" --realm "$realm" &
    pids="$pids $!"

    # Whichever instance exits first takes the container with it, so a dead
    # server doesn't leave something that looks healthy.
    wait -n
    status=$?
    echo "demo: an instance exited ($status), shutting down" >&2
    exit "$status"
fi

cat >&2 <<'USAGE'
demo: no wayland socket and no DISPLAY, so the GUI client wasn't started. Hand
a compositor in to get it:

  docker run --rm -it \
    -e XDG_RUNTIME_DIR=/run/user/1000 \
    -e WAYLAND_DISPLAY="$WAYLAND_DISPLAY" \
    -v "$XDG_RUNTIME_DIR/$WAYLAND_DISPLAY":/run/user/1000/"$WAYLAND_DISPLAY" \
    --device /dev/dri \
    -v sandpolis-demo:/data \
    sandpolis/demo
USAGE

if [ ! -t 0 ]; then
    echo "demo: no terminal attached (run with -it for a shell), waiting" >&2
    wait -n
    status=$?
    echo "demo: an instance exited ($status), shutting down" >&2
    exit "$status"
fi

# There's still an estate to talk to without a GUI, so hand over a shell aimed at
# it. $S7S_REALM and $S7S_DATA are what a client subcommand would otherwise need
# spelled out every time, and only client subcommands run from here — the server
# and agent already have their own directories.
cat >&2 <<'BANNER'

Dropping into a shell against the running demo. Try:

  sandpolis server list
  sandpolis agent list
  sandpolis agent list --json

Leaving the shell stops the demo.

BANNER

cd "$data"
S7S_REALM="$realm" S7S_DATA="$data/client" PS1='[sandpolis demo] \w \$ ' \
    bash --norc -i
status=$?
echo "demo: shell exited ($status), shutting down" >&2
exit "$status"
DEMO
chmod +x /rootfs/bin/sandpolis-demo
/rootfs/runtime/bin/bash -n /rootfs/bin/sandpolis-demo
INSTALL

FROM scratch AS server
COPY --from=rootfs-server /rootfs/ /
ENV PATH=/bin:/runtime/bin \
    LD_LIBRARY_PATH=/runtime/lib \
    SSL_CERT_FILE=/etc/ssl/certs/ca-certificates.crt \
    LC_ALL=C \
    HOME=/tmp \
    S7S_DATA=/data
VOLUME /data
EXPOSE 8768
ENTRYPOINT [ "/bin/sandpolis" ]
CMD [ "server" ]

FROM scratch AS agent
COPY --from=rootfs-agent /rootfs/ /
ENV PATH=/bin:/runtime/bin \
    LD_LIBRARY_PATH=/runtime/lib \
    SSL_CERT_FILE=/etc/ssl/certs/ca-certificates.crt \
    LC_ALL=C \
    HOME=/tmp \
    S7S_DATA=/data \
    GST_PLUGIN_SYSTEM_PATH_1_0=/runtime/lib/gstreamer-1.0 \
    ALSA_CONFIG_PATH=/runtime/share/alsa/alsa.conf
VOLUME /data
ENTRYPOINT [ "/bin/sandpolis" ]
CMD [ "agent" ]

# The GUI reads more of the environment than the daemons do: the vulkan loader
# finds its drivers under $XDG_DATA_DIRS, and without XKB_CONFIG_ROOT the window
# comes up but takes no keyboard input.
FROM scratch AS client
COPY --from=rootfs-client /rootfs/ /
ENV PATH=/bin:/runtime/bin \
    LD_LIBRARY_PATH=/runtime/lib \
    SSL_CERT_FILE=/etc/ssl/certs/ca-certificates.crt \
    LC_ALL=C \
    HOME=/tmp \
    S7S_DATA=/data \
    XDG_DATA_DIRS=/runtime/share \
    XKB_CONFIG_ROOT=/runtime/share/X11/xkb \
    LIBGL_DRIVERS_PATH=/runtime/lib/dri \
    __EGL_VENDOR_LIBRARY_DIRS=/runtime/share/glvnd/egl_vendor.d \
    ALSA_CONFIG_PATH=/runtime/share/alsa/alsa.conf
VOLUME /data
ENTRYPOINT [ "/bin/sandpolis" ]
CMD [ "client" ]

# No S7S_DATA here: all three subcommands would read it and collide on one
# database. The entrypoint gives each instance its own directory under /data.
FROM scratch AS demo
COPY --from=rootfs-demo /rootfs/ /
ENV PATH=/bin:/runtime/bin \
    LD_LIBRARY_PATH=/runtime/lib \
    SSL_CERT_FILE=/etc/ssl/certs/ca-certificates.crt \
    LC_ALL=C \
    HOME=/tmp \
    XDG_DATA_DIRS=/runtime/share \
    XKB_CONFIG_ROOT=/runtime/share/X11/xkb \
    LIBGL_DRIVERS_PATH=/runtime/lib/dri \
    __EGL_VENDOR_LIBRARY_DIRS=/runtime/share/glvnd/egl_vendor.d \
    GST_PLUGIN_SYSTEM_PATH_1_0=/runtime/lib/gstreamer-1.0 \
    ALSA_CONFIG_PATH=/runtime/share/alsa/alsa.conf
VOLUME /data
EXPOSE 8768
ENTRYPOINT [ "/runtime/bin/bash", "/bin/sandpolis-demo" ]
