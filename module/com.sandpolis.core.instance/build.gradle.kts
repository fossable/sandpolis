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
	id("sandpolis-codegen")
}

dependencies {
	testImplementation("net.jodah:concurrentunit:0.4.6")
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.1")
	testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.1")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.1")

	api(project(":module:com.sandpolis.core.foundation"))
}
