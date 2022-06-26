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
	kotlin("multiplatform") version "1.6.10"
	id("org.s7s.build.module")
	id("org.s7s.build.protobuf")
	id("org.s7s.build.publish")
}

kotlin {
	jvm()
	linuxX64()
}

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.+")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.+")
	testImplementation("org.junit.jupiter:junit-jupiter-params:5.+")
	testImplementation("net.jodah:concurrentunit:0.4.6")

	// https://github.com/google/guava
	api ("com.google.guava:guava:30.1.1-jre") {
		exclude(group = "com.google.code.findbugs", module = "jsr305")
		exclude(group = "com.google.guava", module = "listenablefuture")
		exclude(group = "org.checkerframework", module = "checker-qual")
		exclude(group = "com.google.errorprone", module = "error_prone_annotations")
		exclude(group = "com.google.j2objc", module = "j2objc-annotations")
	}

	// https://github.com/protocolbuffers/protobuf
	api("com.google.protobuf:protobuf-java:3.18.0")

	// https://github.com/qos-ch/slf4j
	api("org.slf4j:slf4j-api:1.7.30")
}
