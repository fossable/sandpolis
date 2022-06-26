//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//

plugins {
	id("java-library")
	id("application")
	id("org.s7s.build.module")
	id("org.s7s.build.instance")
	id("org.s7s.build.publish")
}

application {
	mainModule.set("org.s7s.instance.server.java")
	mainClass.set("org.s7s.instance.server.java.Main")
	applicationDefaultJvmArgs = listOf("--enable-native-access=org.s7s.core.foreign")
}

tasks.named<JavaExec>("run") {
	environment.put("S7S_DEVELOPMENT_MODE", "true")
	environment.put("S7S_LOG_LEVELS", "io.netty=WARN,java.util.prefs=OFF,org.s7s=TRACE")
}

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.+")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.+")

	if (project.getParent() == null) {
		api("org.s7s:core.instance:+")
	} else {
		api(project(":core:instance"))
	}
}

// Also build plugins unless this is the root project
if (project.getParent() != null) {
	val syncPlugins by tasks.creating(Copy::class) {
		into("build/plugin")

		project(":plugin").subprojects {
			afterEvaluate {
				tasks.findByName("pluginArchive")?.let { pluginArchiveTask ->
					from(pluginArchiveTask)
				}
			}
		}
	}

	afterEvaluate {
		tasks.findByName("run")?.dependsOn(syncPlugins)
	}
}
