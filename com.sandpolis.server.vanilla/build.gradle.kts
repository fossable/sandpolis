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

task<Exec>("imageTest") {
	dependsOn(imageBuild)
	commandLine("docker", "run", "-p", "8768:8768", "--rm", "sandpolis/server/vanilla:latest")
}
