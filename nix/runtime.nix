# The runtime environment for each container image: one directory holding every
# library, binary and data file the instance needs, whose closure is what gets
# copied into the image. `/share` and `/etc` are linked because that's where the
# vulkan ICD manifests, xkb keymaps, gstreamer plugins and CA bundle live —
# leaving them out produces an image that starts and then can't find a GPU or
# take keyboard input.
{ pkgs ? import ./nixpkgs.nix { } }:

let
  deps = import ./deps.nix { inherit pkgs; };

  env = instance: paths:
    pkgs.buildEnv {
      name = "sandpolis-runtime-${instance}";
      inherit paths;
      pathsToLink = [ "/lib" "/bin" "/share" "/etc" ];
      # Only the `lib` outputs are added: asking for `out` as well drags the
      # whole gcc compiler in behind stdenv.cc.cc.lib.
      extraOutputsToInstall = [ "lib" ];
      ignoreCollisions = true;
    };

in {
  server = env "server" deps.runtimeServer;
  agent = env "agent" deps.runtimeAgent;
  client = env "client" deps.runtimeClient;
  demo = env "demo" deps.runtimeDemo;
}
