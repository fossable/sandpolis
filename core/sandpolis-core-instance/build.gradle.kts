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
	id("org.s7s.build.codegen")
}

dependencies {
	testImplementation("net.jodah:concurrentunit:0.4.6")
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.+")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.+")
	testImplementation("org.junit.jupiter:junit-jupiter-params:5.+")
	testImplementation("org.awaitility:awaitility:4.1.0")

	// https://github.com/qos-ch/logback
	implementation("ch.qos.logback:logback-core:1.3.0-alpha11") {
		exclude(group = "edu.washington.cs.types.checker", module = "checker-framework")
	}
	implementation("ch.qos.logback:logback-classic:1.3.0-alpha11") {
		exclude(group = "edu.washington.cs.types.checker", module = "checker-framework")
		exclude(group = "com.sun.mail", module = "javax.mail")
	}

	// https://github.com/netty/netty
	api("io.netty:netty-common:4.1.70.Final")
	api("io.netty:netty-codec:4.1.70.Final")
	api("io.netty:netty-codec-dns:4.1.70.Final")
	api("io.netty:netty-transport:4.1.70.Final")
	api("io.netty:netty-handler:4.1.70.Final")
	api("io.netty:netty-resolver-dns:4.1.70.Final")

	// https://github.com/FasterXML/jackson-databind
	api("com.fasterxml.jackson.core:jackson-databind:2.12.4")

	if (project.getParent() == null) {
		api("org.s7s:core.foundation:+")
		api("org.s7s:core.protocol:+")
	} else {
		api(project(":core:foundation"))
		api(project(":core:protocol"))
	}
}
