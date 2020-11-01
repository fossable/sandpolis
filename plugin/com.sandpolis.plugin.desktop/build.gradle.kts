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

import com.google.protobuf.gradle.*

plugins {
	id("eclipse")
	id("java-library")
	id("com.sandpolis.gradle.soi")
	id("com.sandpolis.gradle.plugin")
	id("com.google.protobuf") version "0.8.13"
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
			srcDirs("src/main/proto")
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

protobuf {
	protoc {
		artifact = "com.google.protobuf:protoc:3.13.0"
	}

	generatedFilesBaseDir = "$projectDir/gen/"

	tasks {
		clean {
			delete(generatedFilesBaseDir)
		}
	}
	generateProtoTasks {
		ofSourceSet("main").forEach { task ->
			task.builtins {
				remove("java")
				id("java") {
					option("lite")
				}
				if (project.properties["instances.agent.micro"] == "true") {
					id("cpp") {
						option("lite")
					}					
				}
				if (project.properties["instances.client.lockstone"] == "true") {
					id("swift") {
						option("lite")
					}
				}
			}
		}
	}
}

sandpolis_plugin {
	id = project.name
	coordinate = "com.sandpolis:sandpolis-plugin-desktop"
	name = "Desktop Plugin"
	description = ""
}
