# The dev shell. Its package lists live in nix/deps.nix so the container images
# in sandpolis/Dockerfile build against exactly what this shell provides. Pass
# `--arg pkgs 'import <nixpkgs> { }'` to escape the pin in nix/nixpkgs.nix.
{ pkgs ? import ./nix/nixpkgs.nix { } }:

with pkgs;

let deps = import ./nix/deps.nix { inherit pkgs; };

in mkShell rec {
  inherit (deps) nativeBuildInputs buildInputs;

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
