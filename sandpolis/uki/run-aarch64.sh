#!/bin/sh
# Boot a sandpolis UKI in QEMU (aarch64). Takes the path to the .efi as $1;
# OVMF firmware locations come from $OVMF_CODE / $OVMF_VARS.

set -eu

EFI=${1:?usage: run-aarch64.sh path/to/sandpolis.efi}
OVMF_CODE=${OVMF_CODE:-/usr/share/AAVMF/AAVMF_CODE.fd}
OVMF_VARS=${OVMF_VARS:-/usr/share/AAVMF/AAVMF_VARS.fd}

# Set up ESP directory structure in temp directory
ESP_DIR=$(mktemp -d)
trap 'rm -rf "$ESP_DIR"' EXIT
mkdir -p "$ESP_DIR/EFI/Boot"
cp "$EFI" "$ESP_DIR/EFI/Boot/BootAA64.efi"

qemu-system-aarch64 \
  -nodefaults --enable-kvm -m 256M -machine virt -cpu cortex-a72 -smp 4 \
  -drive if=pflash,format=raw,file="$OVMF_CODE",readonly=on \
  -drive if=pflash,format=raw,file="$OVMF_VARS",readonly=on \
  -drive format=raw,file=fat:rw:"$ESP_DIR" \
  -netdev user,id=user.0 -device rtl8139,netdev=user.0 \
  -serial stdio -device isa-debug-exit,iobase=0xf4,iosize=0x04 -vga std
