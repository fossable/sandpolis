plugins {
	id("org.s7s.build.module")
	id("org.s7s.build.instance")
	id("org.s7s.build.publish")
}

dependencies {
	proto("org.s7s:core.foundation:+:rust@zip")
	proto("org.s7s:core.instance:+:rust@zip")
	proto("org.s7s:core.net:+:rust@zip")
	proto("org.s7s:plugin.snapshot:+:rust@zip")
}

val buildAmd64 by tasks.creating(Exec::class) {
	dependsOn("assembleProto")
	workingDir(project.getProjectDir())
	commandLine(listOf("cargo", "+nightly", "build", "--release", "--target=x86_64-unknown-uefi"))
	outputs.files("target/x86_64-unknown-uefi/release/agent.efi")
}

val buildAarch64 by tasks.creating(Exec::class) {
	dependsOn("assembleProto")
	workingDir(project.getProjectDir())
	commandLine(listOf("cargo", "+nightly", "build", "--release", "--target=aarch64-unknown-uefi"))
	outputs.files("target/aarch64-unknown-uefi/release/agent.efi")
}

tasks.findByName("build")?.dependsOn(buildAmd64, buildAarch64)

tasks.findByName("clean")?.doLast {
	delete("src/gen")
}

val runAmd64 by tasks.creating(Exec::class) {
	dependsOn(buildAmd64)
	workingDir(project.getProjectDir())
	commandLine(listOf(
		"qemu-system-x86_64",

		// Setup system
		"-nodefaults", "--enable-kvm", "-m", "256M", "-machine", "q35", "-smp", "4",

		// UEFI firmware stuff
		"-drive", "if=pflash,format=raw,file=/usr/share/ovmf/x64/OVMF_CODE.fd,readonly=on", "-drive", "if=pflash,format=raw,file=/usr/share/ovmf/x64/OVMF_VARS.fd,readonly=on",

		// Mount build directory
		"-drive", "format=raw,file=fat:rw:${project.getProjectDir()}/build/esp",

		// Setup NIC
		"-netdev", "user,id=user.0", "-device", "rtl8139,netdev=user.0", // "-object", "filter-dump,id=id,netdev=user.0,file=/tmp/test.pcap",

		// Setup STDOUT
		"-serial", "stdio", "-device", "isa-debug-exit,iobase=0xf4,iosize=0x04", "-vga", "std"
	))

	doFirst {
		copy {
			from("target/x86_64-unknown-uefi/release/agent.efi")
			into("${project.getProjectDir()}/build/esp/EFI/Boot/BootX64.efi")
		}
	}
}

val runAarch64 by tasks.creating(Exec::class) {
	dependsOn(buildAarch64)
	workingDir(project.getProjectDir())
	commandLine(listOf(
		"qemu-system-aarch64",

		// Setup system
		"-nodefaults", "--enable-kvm", "-m", "256M", "-machine", "virt", "-cpu", "cortex-a72", "-smp", "4",

		// UEFI firmware stuff
		"-drive", "if=pflash,format=raw,file=/usr/share/ovmf/x64/OVMF_CODE.fd,readonly=on", "-drive", "if=pflash,format=raw,file=/usr/share/ovmf/x64/OVMF_VARS.fd,readonly=on",

		// Mount build directory
		"-drive", "format=raw,file=fat:rw:${project.getProjectDir()}/build/esp",

		// Setup NIC
		"-netdev", "user,id=user.0", "-device", "rtl8139,netdev=user.0", // "-object", "filter-dump,id=id,netdev=user.0,file=/tmp/test.pcap",

		// Setup STDOUT
		"-serial", "stdio", "-device", "isa-debug-exit,iobase=0xf4,iosize=0x04", "-vga", "std"
	))

	doFirst {
		copy {
			from("target/aarch64-unknown-uefi/release/agent.efi")
			into("${project.getProjectDir()}/build/esp/EFI/Boot/BootAA64.efi")
		}
	}
}

publishing {
	publications {
		create<MavenPublication>("agent") {
			groupId = "org.s7s"
			artifactId = "agent.boot"
			version = project.version.toString()

			artifact("target/x86_64-unknown-uefi/release/agent.efi") {
				classifier = "amd64"
			}

			artifact("target/aarch64-unknown-uefi/release/agent.efi") {
				classifier = "aarch64"
			}
		}
		tasks.findByName("publishAgentPublicationToCentralStagingRepository")?.dependsOn("build")
	}
}
