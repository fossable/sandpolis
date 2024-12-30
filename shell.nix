with import <nixpkgs> { };
stdenv.mkDerivation {
  name = "env";
  nativeBuildInputs = [ pkg-config ];
  buildInputs = [ fuse3 systemd wayland alsa-lib ];
}

# cargo
# rustc
# rust-analyzer
# rustfmt
# clippy
# gcc
# alsa-lib
# dbus
# pkg-config
# udev
# wayland
# libxkbcommon

# LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath [
#       # stdenv.cc.cc
#       pkgs.libxkbcommon
# 		pkgs.vulkan-loader
#     ];

# 	env = { 
# 		RUST_BACKTRACE = "full";
# 		WINIT_UNIX_BACKEND="wayland";
# 	}; 
