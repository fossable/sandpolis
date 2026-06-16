{ pkgs ? import (fetchTarball
  "https://github.com/NixOS/nixpkgs/archive/nixos-unstable.tar.gz") { } }:

with pkgs;

mkShell rec {
  nativeBuildInputs = [ pkg-config cargo rustc rust-analyzer rustfmt clippy ];
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
  LD_LIBRARY_PATH = lib.makeLibraryPath buildInputs;
  LIBCLANG_PATH = "${libclang.lib}/lib";
  # libwebm (pulled in by rustdesk's scrap via rust-webm) predates the
  # stricter modern g++ and uses fixed-width ints without <cstdint>.
  CXXFLAGS = "-include cstdint";
  # rustdesk's scrap and v4l2-sys run bindgen, whose libclang cannot find the
  # glibc / gcc / kernel headers by default under nix. Feed it the cc-wrapper's
  # own cflags plus the clang resource dir and kernel uapi headers.
  shellHook = ''
    export BINDGEN_EXTRA_CLANG_ARGS="$(< ${stdenv.cc}/nix-support/libc-crt1-cflags) \
      $(< ${stdenv.cc}/nix-support/libc-cflags) \
      $(< ${stdenv.cc}/nix-support/cc-cflags) \
      $(< ${stdenv.cc}/nix-support/libcxx-cxxflags) \
      -idirafter ${libclang.lib}/lib/clang/${lib.versions.major (lib.getVersion clang)}/include \
      -isystem ${linuxHeaders}/include"
  '';
}
