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
	id("org.s7s.build.protobuf")
	id("org.s7s.build.plugin")
	id("org.s7s.build.codegen")
	id("org.s7s.build.publish")
}

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.+")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.+")

	//implementation platform("ai.djl:bom:0.13.0")
	//implementation "ai.djl.mxnet:mxnet-engine"
	//runtimeOnly "ai.djl.mxnet:mxnet-native-auto"

	if (project.getParent() == null) {
		compileOnly("org.s7s:core.instance:+")
	} else {
		compileOnly(project(":core:instance"))
	}
}

sandpolis_plugin {
	id = project.name
	coordinate = "org.s7s:sandpolis-plugin-alert"
	name = "Alert Plugin"
	description = ""
}
