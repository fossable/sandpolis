//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//

import org.gradle.api.file.DuplicatesStrategy
import org.gradle.internal.os.OperatingSystem
import org.openbakery.xcode.Destination

plugins {
	id("org.s7s.build.module")
	id("org.s7s.build.instance")
	id("org.openbakery.xcode-plugin") version "0.20.1"
}

xcodebuild {
	scheme = "SandpolisClient"
	target = "SandpolisClient"

	setDestination(Destination("iOS Simulator", "iPhone 12", "14.4"))
}

dependencies {
	proto("org.s7s:core.foundation:+:swift@zip")
	proto("org.s7s:core.instance:+:swift@zip")
	proto("org.s7s:core.net:+:swift@zip")
	proto("org.s7s:core.clientserver:+:swift@zip")
	proto("org.s7s:plugin.desktop:+:swift@zip")
	proto("org.s7s:plugin.shell:+:swift@zip")
}

// Relocate generated sources
tasks.findByName("assembleProto")!!.doLast {
	copy {
		from("src/gen/swift")
		into("SandpolisClient/Gen")
		duplicatesStrategy = DuplicatesStrategy.INCLUDE
	}
}

tasks.findByName("clean")?.doLast {
	delete("SandpolisClient/Gen")
}

tasks.xcodebuild {
	dependsOn("assembleProto")
}

// Disable some tasks if we're not running on macOS
if (!OperatingSystem.current().isMacOsX()) {
	tasks.findByName("keychainClean")?.setEnabled(false)
	tasks.findByName("xcodebuild")?.setEnabled(false)
	tasks.findByName("xcodebuildConfig")?.setEnabled(false)
}
