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

plugins {
	id("eclipse")
	id("java-library")
	id("com.sandpolis.gradle.soi")
	id("com.sandpolis.gradle.plugin")
	id("com.sandpolis.gradle.codegen")
}

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.1")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.1")

	api(project(":module:com.sandpolis.core.instance"))
	api(project(":module:com.sandpolis.core.net"))
}

eclipse {
	project {
		name = project.name
		comment = project.name
	}
}

sourceSets {
	main {
		java {
			srcDirs("gen/main/java")
		}
	}
}

tasks {
	javadoc {
		// Ignore errors in generated protobuf sources
		setFailOnError(false)
	}
}

sandpolis_plugin {
	id = project.name
	coordinate = "com.sandpolis:sandpolis-plugin-sysinfo"
	name = "System Info Plugin"
	description = ""
}
