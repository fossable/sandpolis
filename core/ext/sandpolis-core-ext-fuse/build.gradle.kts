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
	id("org.s7s.build.module")
	id("org.s7s.build.publish")
	id("org.s7s.build.jextract")
}

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.+")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.+")

	if (project.getParent() == null) {
		implementation("org.s7s:core.foundation:+")
	} else {
		implementation(project(":core:foundation"))
	}
}

jextract {
	invocations = mapOf(
		"fuse3/fuse_lowlevel.h" to listOf("jextract", "--source", "-d", "src/gen/java", "-t", project.name, "-C", "-DFUSE_USE_VERSION=30")
	)
}
