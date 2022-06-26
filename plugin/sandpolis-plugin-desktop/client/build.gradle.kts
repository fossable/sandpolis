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
	kotlin("jvm") version "1.6.0"
	id("org.s7s.build.module")
}

import org.gradle.internal.os.OperatingSystem

repositories {
	maven {
		url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
	}
}

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.+")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.+")

	compileOnly(project.getParent()!!)

	findProject(":instance:org.s7s.instance.client.desktop")?.let {
		compileOnly(it)
	} ?: run {
		if (OperatingSystem.current().isMacOsX()) {
			compileOnly("org.s7s:client.lifegem:+:macos")
		} else if (OperatingSystem.current().isLinux()) {
			compileOnly("org.s7s:client.lifegem:+:linux")
		} else {
			compileOnly("org.s7s:client.lifegem:+:windows")
		}
	}
}

eclipse {
	project {
		name = "org.s7s.plugin.desktop:client:lifegem"
		comment = "org.s7s.plugin.desktop:client:lifegem"
	}
}
