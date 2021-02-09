//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//

plugins {
	id("java-library")
	id("sandpolis-java")
	id("sandpolis-module")
	id("sandpolis-protobuf")
	id("sandpolis-publish")
	id("sandpolis-soi")
	id("sandpolis-plugin")
}

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.1")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.1")

	api(project(":module:com.sandpolis.core.instance"))
	api(project(":module:com.sandpolis.core.net"))

	// https://github.com/javaee/jpa-spec
	implementation("javax.persistence:javax.persistence-api:2.2")
}

sandpolis_plugin {
	id = "com.sandpolis.plugin.upgrade"
	coordinate = "com.sandpolis:sandpolis-plugin-upgrade"
	name = "Upgrade Plugin"
	description = "A plugin that integrates with several upgrade mechanisms."
}
