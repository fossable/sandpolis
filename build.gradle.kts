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
	id("com.diffplug.spotless") version "6.0.0"
	kotlin("jvm") version "1.5.20"
}

spotless {
	cpp {
		target("**/*.cc", "**/*.hh")
		targetExclude("**/build/**", "**/src/gen/**", "**/node_modules/**")

		eclipseCdt()
		endWithNewline()
		indentWithTabs()

		licenseHeaderFile(file("gradle/resources/header_cpp.txt"), "(\\#include|\\#ifndef)")
	}
	kotlin {
		target("**/*.kt")
		targetExclude("**/build/**", "**/src/gen/**", "**/node_modules/**")

		licenseHeaderFile(file("gradle/resources/header_gradle.txt"), "(plugins|import|package)")
	}
	kotlinGradle {
		target("**/*.kts")
		targetExclude("**/build/**", "**/src/gen/**", "**/node_modules/**")

		licenseHeaderFile(file("gradle/resources/header_gradle.txt"), "(plugins|import|buildscript|rootProject)")
	}
	java {
		target("**/*.java")
		targetExclude("**/build/**", "**/src/gen/**", "**/node_modules/**")

		eclipse().configFile("gradle/resources/EclipseConventions.xml")
		trimTrailingWhitespace()
		endWithNewline()

		licenseHeaderFile(file("gradle/resources/header_java.txt"), "package")
	}
	format("javaModules") {
		target("**/module-info.java")
		targetExclude("**/build/**", "**/src/gen/**", "**/node_modules/**")

		trimTrailingWhitespace()
		endWithNewline()

		licenseHeaderFile(file("gradle/resources/header_java.txt"), "(module|open module)")
	}
	format("proto") {
		target("**/*.proto")
		targetExclude("**/build/**", "**/src/gen/**", "**/node_modules/**")

		trimTrailingWhitespace()
		endWithNewline()
		indentWithSpaces()

		licenseHeaderFile(file("gradle/resources/header_java.txt"), "syntax")
	}
	format("swift") {
		target("**/*.swift")
		targetExclude("**/build/**", "**/src/gen/**", "**/node_modules/**")

		trimTrailingWhitespace()
		endWithNewline()
		indentWithTabs()

		licenseHeaderFile(file("gradle/resources/header_swift.txt"), "import")
	}
	format("css") {
		target("**/*.css")
		targetExclude("**/build/**", "**/src/gen/**", "**/node_modules/**")

		eclipseWtp(com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep.CSS)
	}
	format("json") {
		target("**/*.json")
		targetExclude("**/build/**", "**/src/gen/**", "**/node_modules/**", "**/com.sandpolis.client.lockstone/**")

		eclipseWtp(com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep.JSON)
	}
	format("markdown") {
		target("**/*.md")
		targetExclude("**/build/**", "**/src/gen/**", "**/node_modules/**")

		prettier().config(mapOf("proseWrap" to "always"))
	}
	format("yaml") {
		target("**/*.yml", "**/*.yaml")
		targetExclude("**/build/**", "**/src/gen/**", "**/node_modules/**")

		prettier()
	}
	format("javascript") {
		target("**/*.js")
		targetExclude("**/build/**", "**/src/gen/**", "**/node_modules/**")

		prettier()
	}
	python {
		target("**/*.py")
		targetExclude("**/build/**", "**/src/gen/**", "**/node_modules/**")

		black("21.8b0")
	}
}

// Create tasks to update Gradle wrapper
allprojects.forEach {
	if (it.name.startsWith("com.sandpolis")) {
		val updateWrappers by it.tasks.creating(Wrapper::class) {
			gradleVersion = "7.0.2"
		}
	}
}
