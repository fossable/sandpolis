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
}

dependencies {
	implementation(project(":module:com.sandpolis.core.instance"))
	implementation(project(":module:com.sandpolis.core.server"))

	// https://github.com/cilki/zipset
	implementation("com.github.cilki:zipset:1.2.1")
}

eclipse {
	project {
		name = "com.sandpolis.agent.installer:jar"
		comment = ""
	}
}

tasks.jar {
	archiveBaseName.set("sandpolis-agent-installer-jar")
	manifest {
		attributes(mapOf("Main-Class" to "com.sandpolis.agent.installer.jar.Main"))
	}
}
