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

import com.bmuschko.gradle.docker.tasks.container.*
import com.bmuschko.gradle.docker.tasks.image.*

plugins {
	id("eclipse")
	id("java-library")

	id("com.sandpolis.gradle.soi")
	id("com.bmuschko.docker-remote-api") version "6.6.0"
	id("de.jjohannes.extra-java-module-info") version "0.3"
}

dependencies {
	testImplementation(project(path = ":module:com.sandpolis.core.net", configuration = "tests"))
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.1")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.1")

	implementation(project(":module:com.sandpolis.core.server"))

	// https://github.com/hibernate/hibernate-ogm
	implementation("org.hibernate.ogm:hibernate-ogm-mongodb:5.4.1.Final")

	// https://github.com/netty/netty
	implementation("io.netty:netty-codec:4.1.48.Final")
	implementation("io.netty:netty-common:4.1.48.Final")
	implementation("io.netty:netty-handler:4.1.48.Final")
	implementation("io.netty:netty-transport:4.1.48.Final")

	implementation(project(":com.sandpolis.agent.installer:go"))
	implementation(project(":com.sandpolis.agent.installer:jar"))
	implementation(project(":com.sandpolis.agent.installer:py"))
}

eclipse {
	project {
		name = project.name
		comment = project.name
	}
}

// TODO remove
extraJavaModuleInfo {
	automaticModule("java-otp-0.2.0.jar", "java.otp")
	automaticModule("hibernate-ogm-mongodb-5.4.1.Final.jar", "hibernate.ogm.mongodb")
	module("hibernate-ogm-core-5.4.1.Final.jar", "hibernate.ogm.core", "5.4.1")
	automaticModule("hibernate-hql-parser-1.5.0.Final.jar", "hibernate.hql.parser")
	automaticModule("mongo-java-driver-3.9.1.jar", "mongo.java.driver")
	automaticModule("antlr-runtime-3.4.jar", "antlr.runtime")
	module("parboiled-core-1.1.8.jar", "parboiled.core", "1.1.8")
	module("parboiled-java-1.1.8.jar", "parboiled.java", "1.1.8")
	automaticModule("asm-analysis-5.2.jar", "asm.analysis")
	automaticModule("asm-util-5.2.jar", "asm.util")
	automaticModule("asm-tree-5.2.jar", "asm.tree")
	automaticModule("asm-5.2.jar", "asm")
	automaticModule("javassist-3.23.1-GA.jar", "javassist")
	automaticModule("stringtemplate-3.2.1.jar", "stringtemplate")
	automaticModule("antlr-2.7.7.jar", "antlr")
	automaticModule("jandex-2.0.5.Final.jar", "jandex")
	automaticModule("dom4j-1.6.1.jar", "dom4j")
	automaticModule("failureaccess-1.0.1.jar", "failureaccess")
	automaticModule("checker-framework-1.7.0.jar", "checker.framework")
}

val imageSyncBuildContext by tasks.creating(Sync::class) {
	dependsOn(tasks.named("jar"))
	from(configurations.runtimeClasspath)
	from(tasks.named("jar"))
	into("${buildDir}/docker/lib")
}

val imageBuild by tasks.creating(DockerBuildImage::class) {
	dependsOn(imageSyncBuildContext)
	inputDir.set(file("."))
	images.add("sandpolis/server/vanilla:${project.version}")
	images.add("sandpolis/server/vanilla:latest")
}

val createTestContainer by tasks.creating(DockerCreateContainer::class) {
	dependsOn(imageBuild)
	targetImageId(imageBuild.getImageId())
	hostConfig.portBindings.set(listOf("8768:8768"))
	hostConfig.autoRemove.set(true)
	attachStdout.set(true)
	attachStderr.set(true)
	attachStdin.set(true)
}

val startTestContainer by tasks.creating(DockerStartContainer::class) {
	dependsOn(createTestContainer)
	targetContainerId(createTestContainer.getContainerId())
}

val stopTestContainer by tasks.creating(DockerStopContainer::class) {
	targetContainerId(createTestContainer.getContainerId())
}

tasks.create("imageTest") {
	dependsOn(startTestContainer)
	finalizedBy(stopTestContainer)
}
