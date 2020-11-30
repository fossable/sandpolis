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
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("eclipse")
	id("java-library")

	id("com.sandpolis.gradle.soi")
	id("com.sandpolis.gradle.codegen")
	id("org.openjfx.javafxplugin") version "0.0.9"
	id("com.bmuschko.docker-remote-api") version "6.6.0"
	kotlin("jvm") version "1.4.10"
}

repositories {
	maven {
		url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
	}
}

tasks.withType<KotlinCompile>().configureEach {
	kotlinOptions {
		jvmTarget = "1.8"
	}
}

javafx {
	modules = listOf( "javafx.controls", "javafx.fxml", "javafx.graphics" )
	version = "15"
}

eclipse {
	project {
		name = project.name
		comment = project.name
	}
}

sourceSets {
	main {
		java {
			srcDirs("gen/main/java")
		}
	}
}

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.1")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.1")
	testImplementation("org.testfx:testfx-core:4.0.16-alpha")
	testImplementation("org.testfx:testfx-junit5:4.0.16-alpha")
	testImplementation("org.testfx:openjfx-monocle:jdk-12.0.1+2")
	testImplementation("org.awaitility:awaitility:4.0.1")

	implementation(project(":module:com.sandpolis.core.client"))

	// https://github.com/sirolf2009/fxgraph
	implementation("com.sirolf2009:fxgraph:0.0.3")

	// https://github.com/nayuki/QR-Code-generator
	implementation("io.nayuki:qrcodegen:1.6.0")

	// https://github.com/javaee/jpa-spec
	implementation("javax.persistence:javax.persistence-api:2.2")
	
	implementation("no.tornado:tornadofx:2.0.0-SNAPSHOT")
	implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.10")
}

codegen {
	stateTree = project(":module:com.sandpolis.core.instance").file("state.json")
	javaFxStateTree = true
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
	images.add("sandpolis/client/lifegem:${project.version}")
	images.add("sandpolis/client/lifegem:latest")
}

task<Exec>("imageTest") {
	dependsOn(imageBuild)
	commandLine("docker", "run", "-v", "/tmp/.X11-unix/:/tmp/.X11-unix/", "-e", "DISPLAY", "--net", "host", "--rm", "sandpolis/client/lifegem:latest")
}
