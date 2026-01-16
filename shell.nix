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
    libclang
    libgcc
    xorg.libX11
    xorg.libXcursor
    xorg.libXi
    xorg.libXrandr
    libxkbcommon
    libGL
    wayland
    fuse3
    systemd
    openssl
  ];
  LD_LIBRARY_PATH = lib.makeLibraryPath buildInputs;
}
