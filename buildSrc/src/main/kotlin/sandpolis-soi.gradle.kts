//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//

import java.util.Properties
import java.util.stream.Collectors
import java.io.FileOutputStream

val writeSoi by tasks.creating(DefaultTask::class) {

	doLast {
		val props = Properties()

		// Build time
		props.setProperty("build.timestamp", "${System.currentTimeMillis()}")

		// Instance version
		props.setProperty("instance.version", project.version.toString())

		// Core version
		props.setProperty("core.version", project(":module:com.sandpolis.core.instance").version.toString())

		// Build platform
		props.setProperty("build.platform", "${System.getProperty("os.name")} (${System.getProperty("os.arch")})")

		// Java version
		props.setProperty("build.java.version",	"${System.getProperty("java.version")} (${System.getProperty("java.vendor")})")

		// Gradle version
		props.setProperty("build.gradle.version", project.getGradle().getGradleVersion())

		// Module dependencies
		props.setProperty("build.dependencies", project.getConfigurations().getByName("runtimeClasspath").getResolvedConfiguration().getResolvedArtifacts().stream().map {
			it.getModuleVersion().getId().getGroup() + ":" + it.getModuleVersion().getId().getName() + ":" + it.getModuleVersion().getId().getVersion()
		}.collect(Collectors.joining(",")))

		// Write object
		project.file("src/main/resources").mkdirs()
		val out = FileOutputStream(project.file("src/main/resources/build.properties"))
		props.store(out, null)
	}
}

tasks.findByName("processResources")?.dependsOn(writeSoi)
