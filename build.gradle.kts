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
	id("com.diffplug.spotless") version "5.9.0"
}

spotless {
	cpp {
		target("**/*.cc", "**/*.hh")

		// Exclude build directory
		targetExclude("**/build/**", "**/gen/main/cpp/**")

		eclipseCdt()
		endWithNewline()
		indentWithTabs()

		licenseHeaderFile(file("gradle/resources/header_cpp.txt"), "(\\#include|\\#ifndef)")
	}
	kotlin {
		target("**/*.kt")

		// Exclude build directory
		targetExclude("**/build/**")

		licenseHeaderFile(file("gradle/resources/header_gradle.txt"), "(plugins|import|package)")
	}
	kotlinGradle {
		target("**/*.kts")

		// Exclude build directory
		targetExclude("**/build/**")

		licenseHeaderFile(file("gradle/resources/header_gradle.txt"), "(plugins|import|buildscript|rootProject)")
	}
	java {
		target("**/*.java")

		// Exclude build directory and generated sources
		targetExclude("**/build/**", "**/gen/main/java/**", "**/src/main/java/com/sandpolis/core/instance/converter/**")

		eclipse().configFile("gradle/resources/EclipseConventions.xml")
		trimTrailingWhitespace()
		endWithNewline()

		licenseHeaderFile(file("gradle/resources/header_java.txt"), "package")
	}
	format("javaModules") {
		target("**/module-info.java")

		// Exclude build directory
		targetExclude("**/build/**")

		trimTrailingWhitespace()
		endWithNewline()

		licenseHeaderFile(file("gradle/resources/header_java.txt"), "(module|open module)")
	}
	format("proto") {
		target("**/*.proto")

		// Exclude build directory
		targetExclude("**/build/**")

		trimTrailingWhitespace()
		endWithNewline()
		indentWithSpaces()

		licenseHeaderFile(file("gradle/resources/header_java.txt"), "syntax")
	}
	format("swift") {
		target("**/*.swift")

		// Exclude build directory
		targetExclude("**/build/**")

		trimTrailingWhitespace()
		endWithNewline()
		indentWithTabs()

		licenseHeaderFile(file("gradle/resources/header_swift.txt"), "import")
	}
	format("css") {
		target("**/*.css")

		// Exclude build directory
		targetExclude("**/build/**")

		eclipseWtp(com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep.CSS)
	}
	format("json") {
		target("**/*.json")

		// Exclude build directory and iOS projects
		targetExclude("**/build/**", "**/com.sandpolis.client.lockstone/**")

		eclipseWtp(com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep.JSON)
	}
}

task<Exec>("enableMicro") {
	commandLine("git", "submodule", "update", "--init", "--remote", "com.sandpolis.agent.micro")
}

task<Exec>("disableMicro") {
	commandLine("git", "submodule", "deinit", "com.sandpolis.agent.micro")
}

task("updateJavaVersion") {
	doLast {
		project.fileTree(".") {
			include("**/Dockerfile")
			include("**/*.gradle.kts")
			include("**/*.yml")
			exclude("/build.gradle.kts")
		}.forEach {
			it.writeText(it.readText()
				.replace("java-version: 15", "java-version: 16")
				.replace("FROM adoptopenjdk:15-hotspot", "FROM adoptopenjdk:16-hotspot")
				.replace("JavaLanguageVersion.of(15)", "JavaLanguageVersion.of(16)")
				.replace("jvmTarget = \"15\"", "jvmTarget = \"16\"")
				.replace("version = \"15\"", "version = \"16\""))
		}
	}
}

// Uncheckout all buildSrc directories in instance modules
for ((name, sub) in project.getChildProjects()) {
	if (name.startsWith("com.sandpolis")) {
		sub.afterEvaluate {
			exec {
				commandLine = listOf("git", "submodule", "--quiet", "deinit", "buildSrc")
				workingDir = sub.getProjectDir()
				setIgnoreExitValue(true)
				setErrorOutput(java.io.ByteArrayOutputStream())
			}
		}
	}
}
