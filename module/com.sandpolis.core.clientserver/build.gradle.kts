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
	id("com.google.protobuf") version "0.8.13"
	id("eclipse")
	id("java-library")
}

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.1")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.1")

	implementation(project(":module:com.sandpolis.core.instance"))
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
