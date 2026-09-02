#!/bin/busybox sh
# PID 1 inside the boot agent's unified kernel image: bring up just enough of
# a system for the agent, then hand it the console. Packaged into the
# initramfs by the sandpolis-agent nixpkgs package (`sandpolis-agent.efi`).

set -e

# Create busybox symlinks
/bin/busybox --install -s /bin

# Mount essential filesystems
mount -t proc proc /proc
mount -t sysfs sys /sys
mount -t devtmpfs dev /dev

# Create necessary directories
mkdir -p /tmp /run /var

# Set up networking (lo interface)
ip link set lo up

# The slint UI draws straight to the display
export SLINT_BACKEND=linuxkms-noseat

# Launch the agent
exec /sbin/sandpolis agent
