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

buildscript {
	repositories {
		mavenCentral()
		jcenter()
	}

	dependencies {
		classpath(":com.sandpolis.gradle.plugin:")
		classpath(":com.sandpolis.gradle.codegen:")
	}
}

plugins {
	id("com.diffplug.spotless") version "5.8.2"
}

spotless {
	cpp {
		target("**/*.cc", "**/*.hh")

		// Exclude build directory
		targetExclude("**/build/**")

		eclipseCdt()
		endWithNewline()
		indentWithTabs()
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

		// Exclude build directory
		targetExclude("**/build/**")

		// Exclude generated sources
		targetExclude("**/gen/main/java/**")
		targetExclude("**/src/main/java/com/sandpolis/core/instance/converter/**")

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

		// Exclude build directory
		targetExclude("**/build/**")

		// Exclude iOS projects
		targetExclude("**/com.sandpolis.client.lockstone/**")

		eclipseWtp(com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep.JSON)
	}
}

// Apply repository configuration
allprojects {
	repositories {
		mavenLocal()
		mavenCentral()
	}

	buildscript {
		repositories {
			mavenCentral()
		}
	}
}
