//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//

import java.util.Properties
import java.io.FileOutputStream

val writeSoi by tasks.creating(DefaultTask::class) {

	doLast {
		val props = Properties()

		// Build time
		props.setProperty("build.timestamp", "${System.currentTimeMillis()}")

		// Instance version
		props.setProperty("instance.version", "${project.version}")

		// Core version
		props.setProperty("core.version", "${project.rootProject.version}")

		// Build platform
		props.setProperty("build.platform", "${System.getProperty("os.name")} (${System.getProperty("os.arch")})")

		// Java version
		props.setProperty("build.java.version",	"${System.getProperty("java.version")} (${System.getProperty("java.vendor")})")

		// Gradle version
		props.setProperty("build.gradle.version", project.getGradle().getGradleVersion())

		// Write object
		project.file("src/main/resources").mkdirs()
		val out = FileOutputStream(project.file("src/main/resources/build.properties"))
		props.store(out, null)
	}
}

tasks.findByName("processResources")?.dependsOn(writeSoi)
