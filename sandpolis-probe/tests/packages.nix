# Package set for the probe test container. It brings up one server per protocol
# the probe layer speaks, so every probe can be exercised end to end against a
# real implementation. Built from the same pinned nixpkgs as the rest of the tree
# (see `nix/nixpkgs.nix`), so the fixture tracks the images it tests against.
#
# The `../../nix` path is reproduced inside the image (see `Dockerfile`), so this
# file evaluates the same way in a checkout and in the container build.
{ pkgs ? import ../../nix/nixpkgs.nix { } }:

pkgs.buildEnv {
  name = "sandpolis-test-probe";

  # A wide set collides in `/share` (man pages, X app-defaults); none of the
  # collisions matter to a throwaway fixture.
  ignoreCollisions = true;

  paths = with pkgs; [
    # `Xvnc` is an X server that is itself a VNC server (port 5900 for :0), plus
    # `vncpasswd` to write its password file.
    tigervnc
    # `freerdp-shadow-cli` mirrors an existing X display over RDP (port 3389).
    freerdp
    # `unfsd` is a userspace NFSv3 server (mount + nfs), which a container can
    # run without the kernel nfsd's privileges.
    unfs3
    # `rpcbind` is the portmapper (port 111) `unfs3` registers with, so the
    # probe's resolve-ports-via-GETPORT path is exercised too.
    rpcbind
    # `smbd` serves SMB2/3 (port 445); `smbpasswd` seeds its user database.
    samba
    # `mediamtx` is an RTSP server (port 554) that restreams what ffmpeg
    # publishes into it.
    mediamtx
    # `ffmpeg` generates the H.264 test pattern behind the RTSP path.
    ffmpeg-full
    # NUT: `upsd` (port 3493) plus the `dummy-ups` driver that feeds it.
    nut
    # `sshd` (port 22) and `ssh-keygen` for its host keys.
    openssh
    # `lighttpd` serves a static document root over HTTP (port 80).
    lighttpd
    # `snmpd` answers SNMP v1/v2c (port 161/udp).
    net-snmp
    # `ipmi_sim` simulates a BMC over IPMI LAN (port 623/udp).
    openipmi

    # A window manager and a terminal, so the served desktop isn't blank.
    openbox
    xterm
    xsetroot
    xdpyinfo
    xauth
    xkeyboard-config
    dejavu_fonts

    bashInteractive
    coreutils
    gnused
    # `useradd`/`chpasswd` create the account SMB and SSH share.
    shadow
    # `ss` prints the listening-port summary the entrypoint ends with.
    iproute2
    procps
  ];
}
