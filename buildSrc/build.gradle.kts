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
	`kotlin-dsl`
}

repositories {
	gradlePluginPortal()
}

dependencies {

	// For sandpolis-protobuf plugin
	implementation("com.google.protobuf:protobuf-gradle-plugin:0.8.14")

	// For sandpolis-module plugin
	implementation("org.ajoberstar.grgit:grgit-core:4.1.0")

	// For sandpolis-codegen plugin
	implementation("com.squareup:javapoet:1.11.1")
	implementation("com.google.guava:guava:30.1-jre")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
}
