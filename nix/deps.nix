# Every native dependency the workspace has, in one place: `shell.nix` builds the
# dev shell out of these lists and `runtime.nix` builds the container images out
# of them, so a library that the dev loop needs can't quietly go missing from an
# image.
{ pkgs }:

with pkgs;

rec {
  nativeBuildInputs =
    [ pkg-config cargo rustc rust-analyzer rustfmt clippy mold ];

  buildInputs = [
    udev
    cmake
    alsa-lib
    vulkan-loader
    libyuv
    libvpx
    libaom
    libclang
    libgcc
    libx11
    libxcursor
    libxi
    libxrandr
    libxkbcommon
    libGL
    wayland
    fuse3
    systemd
    openssl
    # Required by rustdesk's scrap (X11 screen capture) and enigo (input)
    libxcb
    libxtst
    xdotool
    # Required by scrap's `wayland` feature (GStreamer-based capture)
    glib
    dbus
    gst_all_1.gstreamer
    gst_all_1.gst-plugins-base
    # Kernel uapi headers for v4l2-sys (pulled in by scrap via nokhwa)
    linuxHeaders
  ];

  # What every image needs before it can run anything at all. The binary names an
  # absolute `/nix/store/…-glibc-…/lib/ld-linux-*.so.2` as its ELF interpreter
  # and pulls in libgcc_s, neither of which is a `buildInputs` entry. Bash is
  # just as load-bearing: `sandpolis-shell` opens sessions by executing `/bin/sh`
  # and `sandpolis-filesystem` shells out to `fusermount3`.
  runtimeBase =
    [ glibc stdenv.cc.cc.lib bashInteractive coreutils gnugrep cacert tzdata ];

  # `udev` is systemd's minimal libs output — the full `systemd` in buildInputs
  # is four times the closure for a libudev nothing else uses at runtime. Nothing
  # links openssl (the `openssl` crate is only a dev-dependency), and anything
  # that did would be picked up from the binary's RUNPATH anyway.
  runtimeServer = runtimeBase ++ [ udev ];

  runtimeAgent = runtimeServer ++ [
    alsa-lib
    fuse3
    libyuv
    libvpx
    libaom
    # Screen capture and input synthesis, dlopened or linked by sandpolis-desktop
    libxcb
    libx11
    libxtst
    xdotool
    glib
    dbus
    gst_all_1.gstreamer
    gst_all_1.gst-plugins-base
    gst_all_1.gst-plugins-good
  ];

  runtimeClient = runtimeServer ++ [
    alsa-lib
    fuse3
    # winit talks wayland directly and dlopens the X11 libraries, so both paths
    # have to be present for the GUI to find a compositor either way
    wayland
    libxkbcommon
    xkeyboard-config
    libx11
    libxcb
    libxcursor
    libxi
    libxrandr
    # wgpu dlopens the vulkan loader, which finds its drivers through mesa's ICD
    # manifests; libglvnd/mesa are the GL fallback
    vulkan-loader
    mesa
    libglvnd
    libGL
  ];

  # The demo image runs all three from one --all-features binary.
  runtimeDemo = lib.unique (runtimeServer ++ runtimeAgent ++ runtimeClient);
}
