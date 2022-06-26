//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//

import org.gradle.internal.os.OperatingSystem

plugins {
	id("java-library")
	id("application")
	id("org.s7s.build.module")
	id("org.s7s.build.instance")
	id("org.s7s.build.publish")
	id("com.github.johnrengelman.shadow") version "7.1.0"
}

application {
	mainModule.set("org.s7s.instance.installer.java")
	mainClass.set("org.s7s.instance.installer.java.Main")
}

tasks.named<JavaExec>("run") {
	environment.put("S7S_DEVELOPMENT_MODE", "true")
	environment.put("S7S_LOG_LEVELS", "io.netty=WARN,java.util.prefs=OFF,org.s7s=TRACE")
}

tasks.withType<Jar>() {
	manifest {
		attributes["Application-Name"] = "Sandpolis"
		attributes["SplashScreen-Image"] = "image/logo.png"
	}
}

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.+")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.+")
	testImplementation("org.testfx:testfx-core:4.0.16-alpha")
	testImplementation("org.testfx:testfx-junit5:4.0.16-alpha")
	testImplementation("org.testfx:openjfx-monocle:jdk-12.0.1+2")
	testImplementation("org.awaitility:awaitility:4.0.1")

	// https://github.com/nayuki/QR-Code-generator
	implementation("io.nayuki:qrcodegen:1.7.0")

	// https://github.com/FasterXML/jackson-databind
	implementation("com.fasterxml.jackson.core:jackson-databind:2.12.4")

	if (project.getParent() == null) {
		implementation("org.s7s:core.foundation:+")
		implementation("org.s7s:core.integration.pacman:+")
		implementation("org.s7s:core.integration.systemd:+")
		implementation("org.s7s:core.integration.apt:+")
		implementation("org.s7s:core.integration.launchd:+")
	} else {
		implementation(project(":core:foundation"))
		implementation(project(":core:ext:pacman"))
		implementation(project(":core:ext:systemd"))
		implementation(project(":core:ext:apt"))
		implementation(project(":core:ext:launchd"))
	}

	if (OperatingSystem.current().isMacOsX()) {
		implementation("org.openjfx:javafx-base:17:mac")
		implementation("org.openjfx:javafx-graphics:17:mac")
		implementation("org.openjfx:javafx-controls:17:mac")
		implementation("org.openjfx:javafx-fxml:17:mac")
		implementation("org.openjfx:javafx-web:17:mac")
	} else if (OperatingSystem.current().isLinux()) {
		implementation("org.openjfx:javafx-base:17:linux")
		implementation("org.openjfx:javafx-graphics:17:linux")
		implementation("org.openjfx:javafx-controls:17:linux")
		implementation("org.openjfx:javafx-fxml:17:linux")
		implementation("org.openjfx:javafx-web:17:linux")
	} else if (OperatingSystem.current().isWindows()) {
		implementation("org.openjfx:javafx-base:17:windows")
		implementation("org.openjfx:javafx-graphics:17:windows")
		implementation("org.openjfx:javafx-controls:17:windows")
		implementation("org.openjfx:javafx-fxml:17:windows")
		implementation("org.openjfx:javafx-web:17:windows")
	}
}

/*githubRelease {
	token = System.properties["publishing.github.token"]
	owner = "sandpolis"
	repo = "sandpolis"
	draft = true
	prerelease = true
}*/
