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
	id("java-library")
	id("sandpolis-java")
	id("sandpolis-module")
	id("sandpolis-protobuf")
	id("sandpolis-publish")
}

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.1")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.1")
	testImplementation("org.awaitility:awaitility:4.0.1")

	implementation(project(":module:com.sandpolis.core.instance"))

	// https://github.com/netty/netty
	api("io.netty:netty-common:4.1.56.Final")
	api("io.netty:netty-codec:4.1.48.Final")
	api("io.netty:netty-codec-dns:4.1.56.Final")
	api("io.netty:netty-transport:4.1.56.Final")
	api("io.netty:netty-handler:4.1.48.Final")
	api("io.netty:netty-resolver-dns:4.1.56.Final")
}
