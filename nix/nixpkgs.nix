# The nixpkgs every other nix file here imports, pinned so a container build and
# the dev shell see the same libraries — the binary hardcodes the store path of
# the glibc it was linked against, so a runtime closure from a different nixpkgs
# would not be able to run it.
#
# Bumping this: any rev works as long as it carries a rustc at least as new as
# the workspace's rust-version (see sandpolis/Cargo.toml). Check with
#
#     nix-instantiate --eval -E '(import ./nix/nixpkgs.nix { }).rustc.version'
#
# and get the hash from `nix-prefetch-url --unpack <url>`.
{ ... }@args:

import (fetchTarball {
  url =
    "https://github.com/NixOS/nixpkgs/archive/e5bdc4a41d4c072fe1e3787eaa0320a384741d44.tar.gz";
  sha256 = "0ayq36r0m8aaa2sj5scr5imj8is6v3r6s17n1sa28pn6crg524hl";
}) args
