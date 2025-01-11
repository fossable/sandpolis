{ pkgs ? import <nixpkgs> { config.android_sdk.accept_license = true; } }:

pkgs.mkShell { buildInputs = with pkgs; [ android-studio-full glibc ]; }
