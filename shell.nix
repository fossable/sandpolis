{ pkgs ? import <nixpkgs> { } }:

with pkgs;

mkShell rec {
  nativeBuildInputs = [ pkg-config ];
  buildInputs = [
    udev
    cmake
    alsa-lib
    vulkan-loader
    libyuv
    libvpx
    libclang
    xorg.libX11
    xorg.libXcursor
    xorg.libXi
    xorg.libXrandr
    libxkbcommon
    wayland
    fuse3
    systemd
    openssl
  ];
  LD_LIBRARY_PATH = lib.makeLibraryPath buildInputs;
}
