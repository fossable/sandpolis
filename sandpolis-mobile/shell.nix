{ pkgs ? import <nixpkgs> { config.allowUnfree = true; } }:

with pkgs;

let
  android-nixpkgs = callPackage <android-nixpkgs> { channel = "stable"; };

  android-sdk = android-nixpkgs.sdk (sdkPkgs:
    with sdkPkgs; [
      cmdline-tools-latest
      build-tools-34-0-0
      platform-tools
      platforms-android-34
      emulator
      cmake-3-22-1
      ndk-26-1-10909125
    ]);

in mkShell {
  buildInputs = [ android-studio android-sdk jdk ];
  nativeBuildInputs =
    [ cargo cargo-ndk rustc rust-analyzer rustfmt clippy rustup ];

  GRADLE_OPTS =
    "-Dorg.gradle.project.android.aapt2FromMavenOverride=${android-sdk}/share/android-sdk/build-tools/34.0.0/aapt2";
}
