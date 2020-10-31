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
	id("com.bmuschko.docker-remote-api") version "6.6.0"
	id("eclipse")
	id("java-library")
	id("com.sandpolis.gradle.soi")
}

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.1")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.1")

	implementation(project(":module:com.sandpolis.core.client"))

	// https://github.com/qos-ch/logback
	implementation("ch.qos.logback:logback-core:1.3.0-alpha5")
	implementation("ch.qos.logback:logback-classic:1.3.0-alpha5")

	// https://github.com/mabe02/lanterna
	implementation("com.googlecode.lanterna:lanterna:3.1.0-alpha1")

	// https://github.com/netty/netty
	implementation("io.netty:netty-codec:4.1.48.Final")
	implementation("io.netty:netty-common:4.1.48.Final")
	implementation("io.netty:netty-handler:4.1.48.Final")
	implementation("io.netty:netty-transport:4.1.48.Final")
}

eclipse {
	project {
		name = project.name
		comment = project.name
	}
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
    images.add("sandpolis/client/ascetic:${project.version}")
    images.add("sandpolis/client/ascetic:latest")
}

val createTestContainer by tasks.creating(DockerCreateContainer::class) {
    dependsOn(imageBuild)
    targetImageId(imageBuild.getImageId())
    hostConfig.portBindings.set(listOf("8080:8080"))
    hostConfig.autoRemove.set(true)
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
