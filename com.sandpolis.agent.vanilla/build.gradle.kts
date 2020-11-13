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
}

eclipse {
	project {
		name = project.name
		comment = project.name
	}
}

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.1")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.1")

	implementation(project(":module:com.sandpolis.core.agent"))

	// https://github.com/javaee/jpa-spec
	implementation("javax.persistence:javax.persistence-api:2.2")
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
	images.add("sandpolis/agent/vanilla:${project.version}")
	images.add("sandpolis/agent/vanilla:latest")
}

task<Exec>("imageTest") {
	dependsOn(imageBuild)
	commandLine("docker", "run", "--rm", "sandpolis/agent/vanilla:latest")
}
