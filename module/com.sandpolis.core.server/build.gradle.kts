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
	id("java-library")
	id("sandpolis-java")
	id("sandpolis-module")
	id("sandpolis-publish")
	id("de.jjohannes.extra-java-module-info") version "0.4"
}

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.1")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.1")

	api(project(":module:com.sandpolis.core.clientserver"))
	api(project(":module:com.sandpolis.core.instance"))
	api(project(":module:com.sandpolis.core.net"))
	api(project(":module:com.sandpolis.core.serveragent"))
	
	// https://github.com/FasterXML/jackson-databind
	implementation("com.fasterxml.jackson.core:jackson-databind:2.12.0")

	// https://github.com/netty/netty
	implementation("io.netty:netty-codec:4.1.56.Final")
	implementation("io.netty:netty-common:4.1.57.Final")
	implementation("io.netty:netty-handler:4.1.56.Final")
	implementation("io.netty:netty-transport:4.1.56.Final")

	// https://github.com/javaee/jpa-spec
	implementation("javax.persistence:javax.persistence-api:2.2")

	// https://github.com/hibernate/hibernate-ogm
	implementation("org.hibernate.ogm:hibernate-ogm-mongodb:5.4.1.Final")

	// https://github.com/cilki/zipset
	implementation("com.github.cilki:zipset:1.2.1")

	// https://github.com/jchambers/java-otp
	//implementation("com.eatthepath:java-otp:0.2.0")

	implementation("javax.xml.bind:jaxb-api:2.3.0")
	
	// https://github.com/hierynomus/sshj
	implementation("com.hierynomus:sshj:0.30.0")
}

extraJavaModuleInfo {
	automaticModule("sshj-0.30.0.jar", "sshj")
	automaticModule("asn-one-0.4.0.jar", "asn.one")
	automaticModule("jzlib-1.1.3.jar", "jzlib")
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

sourceSets {
	main {
		java {
			srcDirs("gen/main/java")
		}
	}
}
