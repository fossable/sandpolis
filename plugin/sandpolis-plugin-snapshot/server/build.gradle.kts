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
}

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.2")

	compileOnly(project.getParent()!!)

	if (project.getParent() == null) {
		implementation("org.s7s.core.ext.qemu:+")
	} else {
		implementation(project(":core:ext:qemu"))
	}
}

eclipse {
	project {
		name = "org.s7s.plugin.snapshot:server"
		comment = "org.s7s.plugin.snapshot:server"
	}
}
