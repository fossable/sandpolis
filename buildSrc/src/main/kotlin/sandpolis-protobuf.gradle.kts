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
	id("java")
	id("com.google.protobuf")
}

sourceSets {
	main {
		java {
			srcDirs("src/main/proto")
			srcDirs("gen/main/java")
		}
	}
}

protobuf {
	protoc {
		artifact = "com.google.protobuf:protoc:3.14.0"
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
				if (findProject(":com.sandpolis.agent.micro") != null) {
					id("cpp") {
						option("lite")
					}
				}
				if (findProject(":com.sandpolis.client.lockstone") != null) {
					id("swift")
				}
			}
		}
	}
}

// Ignore errors in generated protobuf sources
tasks {
	javadoc {
		setFailOnError(false)
	}
}
