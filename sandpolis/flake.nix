{
  description =
    "Sandpolis Agent UKI (Unified Kernel Image) Bootable EFI Application";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachSystem [ "x86_64-linux" "aarch64-linux" ] (system:
      let
        pkgs = import nixpkgs { inherit system; };

        # Build the Sandpolis agent with only agent and layer-power features
        sandpolis-agent = pkgs.rustPlatform.buildRustPackage {
          pname = "sandpolis-agent";
          version = "0.1.0";

          src = ../.;

          cargoLock = {
            lockFile = ../Cargo.lock;
            outputHashes = {
              "native_db-0.8.1" =
                "sha256-HXhS1BRjnG2WDGv+9o5HGnbpEOy08TjJcjJoYqfaOwY=";
            };
          };

          nativeBuildInputs = [ pkgs.pkg-config pkgs.perl ];

          buildInputs = [ pkgs.udev pkgs.openssl pkgs.systemd ];

          # Build only the agent binary with specific features
          buildPhase = ''
            cargo build --package sandpolis --bin sandpolis --release \
              --no-default-features \
              --features agent,layer-power
          '';

          installPhase = ''
            mkdir -p $out/bin
            cp target/release/sandpolis $out/bin/sandpolis
          '';

          # Skip tests for UKI build
          doCheck = false;
        };

        # Minimal init script to launch the agent
        initScript = pkgs.writeScript "init" ''
          #!/bin/sh
          set -e

          # Mount essential filesystems
          mount -t proc proc /proc
          mount -t sysfs sys /sys
          mount -t devtmpfs dev /dev

          # Create necessary directories
          mkdir -p /tmp /run /var

          # Set up networking (lo interface)
          ip link set lo up

          # Launch the Sandpolis agent
          echo "Starting Sandpolis agent..."
          exec /sbin/sandpolis
        '';

        # Create initramfs with dracut
        buildInitramfs = kernel:
          pkgs.stdenv.mkDerivation {
            name = "sandpolis-initramfs";

            nativeBuildInputs = [ pkgs.dracut pkgs.cpio pkgs.findutils ];

            buildCommand = ''
              mkdir -p $out

              # Create a minimal root filesystem structure
              mkdir -p rootfs/{bin,sbin,dev,proc,sys,tmp,run,var,lib,lib64,etc}

              # Copy the agent binary
              cp ${sandpolis-agent}/bin/sandpolis rootfs/sbin/sandpolis
              chmod +x rootfs/sbin/sandpolis

              # Copy the init script
              cp ${initScript} rootfs/init
              chmod +x rootfs/init

              # Copy required shared libraries
              mkdir -p rootfs/lib/x86_64-linux-gnu 2>/dev/null || true
              mkdir -p rootfs/lib/aarch64-linux-gnu 2>/dev/null || true

              # Find and copy all required shared libraries
              for lib in $(ldd ${sandpolis-agent}/bin/sandpolis | grep -o '/nix/store/[^ ]*'); do
                if [ -f "$lib" ]; then
                  cp -L "$lib" rootfs/lib/ 2>/dev/null || \
                  cp -L "$lib" rootfs/lib64/ 2>/dev/null || true
                fi
              done

              # Copy essential busybox for shell commands in init
              cp ${pkgs.busybox}/bin/busybox rootfs/bin/sh
              ln -s sh rootfs/bin/mount
              ln -s sh rootfs/bin/ip
              ln -s sh rootfs/bin/mkdir
              ln -s sh rootfs/bin/echo
              ln -s sh rootfs/bin/exec

              # Create the initramfs with dracut
              ${pkgs.dracut}/bin/dracut \
                --force \
                --no-hostonly \
                --no-hostonly-cmdline \
                --kmoddir ${kernel}/lib/modules \
                --kver "${kernel.version}" \
                --include rootfs / \
                --no-compress \
                $out/initramfs.img

              echo "Initramfs created at $out/initramfs.img"
            '';
          };

        kernel = pkgs.linuxPackages_latest.kernel;

        initramfs = buildInitramfs kernel;

        # Build the UKI (Unified Kernel Image)
        sandpolis-uki = pkgs.stdenv.mkDerivation {
          name = "sandpolis-uki";

          nativeBuildInputs = [
            pkgs.systemd # provides ukify
            pkgs.binutils
          ];

          buildCommand = ''
            mkdir -p $out

            # Use systemd's ukify to create the UKI
            ${pkgs.systemd}/lib/systemd/ukify build \
              --linux=${kernel}/bzImage \
              --initrd=${initramfs}/initramfs.img \
              --cmdline="console=ttyS0 console=tty0 quiet" \
              --output=$out/sandpolis.efi

            echo "UKI created at $out/sandpolis.efi"
          '';
        };

        # Create bootable ISO image with the UKI
        sandpolis-iso = pkgs.stdenv.mkDerivation {
          name = "sandpolis-iso";

          nativeBuildInputs = [ pkgs.xorriso pkgs.dosfstools pkgs.mtools ];

          buildCommand = ''
            mkdir -p iso/EFI/BOOT

            # Copy UKI to the ESP (EFI System Partition) location
            ${if system == "x86_64-linux" then ''
              cp ${sandpolis-uki}/sandpolis.efi iso/EFI/BOOT/BOOTX64.EFI
            '' else ''
              cp ${sandpolis-uki}/sandpolis.efi iso/EFI/BOOT/BOOTAA64.EFI
            ''}

            # Create the ISO image
            ${pkgs.xorriso}/bin/xorriso \
              -as mkisofs \
              -o $out/sandpolis.iso \
              -isohybrid-mbr ${pkgs.syslinux}/share/syslinux/isohdpfx.bin \
              -c boot.cat \
              -b EFI/BOOT/${
                if system == "x86_64-linux" then
                  "BOOTX64.EFI"
                else
                  "BOOTAA64.EFI"
              } \
              -no-emul-boot \
              -boot-load-size 4 \
              -boot-info-table \
              --efi-boot EFI/BOOT/${
                if system == "x86_64-linux" then
                  "BOOTX64.EFI"
                else
                  "BOOTAA64.EFI"
              } \
              -efi-boot-part \
              --efi-boot-image \
              --protective-msdos-label \
              iso

            echo "ISO created at $out/sandpolis.iso"
          '';
        };

      in {
        packages = {
          default = sandpolis-uki;
          sandpolis-uki = sandpolis-uki;
          sandpolis-agent = sandpolis-agent;
          sandpolis-iso = sandpolis-iso;
        };

        devShells.default = pkgs.mkShell {
          buildInputs = [
            pkgs.cargo
            pkgs.rustc
            pkgs.pkg-config
            pkgs.rust-analyzer
            pkgs.rustfmt
            pkgs.clippy

            # Build dependencies
            pkgs.udev
            pkgs.cmake
            pkgs.openssl
            pkgs.systemd
            pkgs.dracut

            # Testing/debugging tools
            pkgs.qemu
            pkgs.ovmf
          ];

          shellHook = ''
            echo "Sandpolis UKI Development Environment"
            echo "Available commands:"
            echo "  nix build .#sandpolis-uki  - Build the UKI EFI application"
            echo "  nix build .#sandpolis-iso  - Build bootable ISO image"
            echo "  nix build .#sandpolis-agent - Build just the agent binary"
            echo ""
            echo "Test with QEMU:"
            echo "  qemu-system-x86_64 -enable-kvm -m 2G -bios ${pkgs.OVMF.fd}/FV/OVMF.fd -drive format=raw,file=result/sandpolis.iso"
          '';
        };
      });
}
