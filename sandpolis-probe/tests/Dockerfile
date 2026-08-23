# syntax=docker/dockerfile:1.7
#
# A test fixture that brings up one server per protocol the probe layer speaks —
# VNC, RDP, NFS, SMB, RTSP, NUT, SSH, HTTP, SNMP, and IPMI — so every probe can
# be exercised by hand against a real implementation. Built from the same pinned
# nixpkgs as the shipped images (see nix/), so the daemons track the rest of the
# tree.
#
#   docker build -f sandpolis-probe/tests/Dockerfile -t sandpolis/test-probe .
#   docker run --rm --network host sandpolis/test-probe
#
# See README.md for the per-port publish form and for what each daemon serves.
#
# The build context is the repository root, not this directory: the image needs
# nix/nixpkgs.nix alongside packages.nix. Unlike the shipped images this is a
# normal running image rather than a `FROM scratch` rootfs — it runs stock
# daemons and doesn't need the sandpolis binary's glibc-pinning rigor.

FROM nixos/nix:2.35.2

# The nix sandbox wants user namespaces, which are unreliable under BuildKit.
# `/etc/nix/nix.conf` is a symlink into the read-only store, so replace it with a
# writable copy first rather than appending through it into a store path.
RUN cp --remove-destination "$(readlink -f /etc/nix/nix.conf)" /etc/nix/nix.conf \
 && chmod 644 /etc/nix/nix.conf \
 && printf '%s\n' 'sandbox = false' 'filter-syscalls = false' >>/etc/nix/nix.conf

# The source layout is reproduced here so `packages.nix` resolves the pin
# through the same relative path it uses in a checkout.
WORKDIR /opt/sandpolis
COPY nix/nixpkgs.nix ./nix/nixpkgs.nix
COPY sandpolis-probe/tests/packages.nix ./sandpolis-probe/tests/packages.nix

# Realize the daemons into a fixed out-link rather than installing them into the
# default profile: the base image's profile already ships bash-interactive, and
# `buildEnv`'s `ignoreCollisions` only covers the env's own contents, so a
# profile install collides on files like `lib/bash/logname`.
RUN nix-build --out-link /opt/test-probe sandpolis-probe/tests/packages.nix

COPY --chmod=0755 sandpolis-probe/tests/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN bash -n /usr/local/bin/entrypoint.sh

# The entrypoint generates /etc/fonts/fonts.conf, which points at the fonts in
# TEST_PROBE_PREFIX; the image ships no system fontconfig configuration.
ENV TEST_PROBE_PREFIX=/opt/test-probe \
    PATH=/opt/test-probe/bin:/opt/test-probe/sbin:/usr/local/bin:/usr/bin:/bin \
    DISPLAY=:0 \
    HOME=/root \
    XKB_CONFIG_ROOT=/opt/test-probe/share/X11/xkb \
    FONTCONFIG_PATH=/etc/fonts \
    NUT_CONFPATH=/etc/nut \
    NUT_STATEPATH=/var/state/ups

EXPOSE 22 80 111 111/udp 161/udp 445 554 623/udp 2049 2049/udp 2050 2050/udp 3389 3493 5900
ENTRYPOINT [ "/usr/local/bin/entrypoint.sh" ]
