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
	id("java")
	id("eclipse")
	id("idea")
}

// Configure Java plugin
java {
	modularity.inferModulePath.set(true)

	withJavadocJar()
	withSourcesJar()

	toolchain {
		languageVersion.set(JavaLanguageVersion.of(14))
	}
}

// Configure unit testing
tasks.test {
	useJUnitPlatform()

	testLogging {
		setExceptionFormat("full")
	}
}

// Configure Eclipse plugin
eclipse {
	project {
		name = project.name
		comment = project.name
	}
}

// Configure artifact filename
tasks.jar {
	archiveBaseName.set(project.path.substring(4).replace(".", "-").replace(":", "-"))
}
