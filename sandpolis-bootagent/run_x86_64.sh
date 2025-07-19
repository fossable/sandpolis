#!/usr/bin/env bash

cp target/x86_64-unknown-uefi/release/agent.efi target/build/esp/EFI/Boot/BootX64.efi
qemu-system-x86_64 \
	-nodefaults --enable-kvm -m 256M -machine q35 -smp 4 \
	-drive if=pflash,format=raw,file=/usr/share/ovmf/x64/OVMF_CODE.fd,readonly=on \
	-drive if=pflash,format=raw,file=/usr/share/ovmf/x64/OVMF_VARS.fd,readonly=on \
	-drive format=raw,file=fat:rw:target/build/esp \
	-netdev user,id=user.0 -device rtl8139,netdev=user.0 \
	-object filter-dump,id=id,netdev=user.0,file=/tmp/test.pcap \
	-serial stdio -device isa-debug-exit,iobase=0xf4,iosize=0x04 -vga std
