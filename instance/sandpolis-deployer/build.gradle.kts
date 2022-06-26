//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//

plugins {
	id("org.s7s.build.module")
	id("org.s7s.build.instance")
	id("org.s7s.build.publish")
}

// Build on the current platform
tasks.findByName("assemble")?.let { task ->
	task.outputs.dir("target")
	task.dependsOn("writeBuildConfig")
	task.doLast {
		exec {
			workingDir(project.getProjectDir())
			commandLine(listOf("cargo", "build", "--color=never"))
		}
	}
}

tasks.findByName("clean")?.doLast {
	delete("target")
}

// Run on the current platform
val run by tasks.creating(Exec::class) {
	dependsOn("writeBuildConfig")
	workingDir(project.getProjectDir())
	commandLine(listOf("cargo", "run", "--color=never"))
	environment.put("RUST_LOG", "deployer=debug")
}

val buildLinuxAmd64 by tasks.creating(Exec::class) {
	workingDir(project.getProjectDir())
	commandLine(listOf("cross", "build", "--release", "--target=x86_64-unknown-linux-musl", "--color=never"))
	outputs.file("target/x86_64-unknown-linux-musl/release/deployer")
}

val buildLinuxAarch64 by tasks.creating(Exec::class) {
	workingDir(project.getProjectDir())
	commandLine(listOf("cross", "build", "--release", "--target=aarch64-unknown-linux-musl", "--color=never"))
	outputs.file("target/aarch64-unknown-linux-musl/release/deployer")
}

val buildMacosAmd64 by tasks.creating(Exec::class) {
	workingDir(project.getProjectDir())
	commandLine(listOf("cross", "build", "--release", "--target=x86_64-apple-darwin", "--color=never"))
	outputs.file("target/x86_64-apple-darwin/release/deployer")
}

val buildMacosAarch64 by tasks.creating(Exec::class) {
	workingDir(project.getProjectDir())
	commandLine(listOf("cross", "build", "--release", "--target=aarch64-apple-darwin", "--color=never"))
	outputs.file("target/aarch64-apple-darwin/release/deployer")
}

val buildWindowsAmd64 by tasks.creating(Exec::class) {
	workingDir(project.getProjectDir())
	commandLine(listOf("cross", "build", "--release", "--target=x86_64-pc-windows-gnu", "--color=never"))
	outputs.file("target/x86_64-pc-windows-gnu/release/deployer.exe")
}

tasks.findByName("build")?.dependsOn(buildLinuxAmd64, buildLinuxAarch64, buildMacosAmd64, buildMacosAarch64, buildWindowsAmd64)

publishing {
	publications {
		create<MavenPublication>("deployer") {
			groupId = "org.s7s"
			artifactId = project.name.toString().replace("org.s7s.", "")
			version = project.version.toString()

			artifact("target/x86_64-unknown-linux-musl/release/deployer") {
				classifier = "linux-amd64"
			}

			artifact("target/aarch64-unknown-linux-musl/release/deployer") {
				classifier = "linux-aarch64"
			}

			artifact("target/x86_64-apple-darwin/release/deployer") {
				classifier = "macos-amd64"
			}

			artifact("target/aarch64-apple-darwin/release/deployer") {
				classifier = "macos-aarch64"
			}

			artifact("target/x86_64-pc-windows-gnu/release/deployer.exe") {
				classifier = "windows-amd64"
			}
		}
		tasks.findByName("publishDeployerPublicationToCentralStagingRepository")?.dependsOn("build")
	}
}
