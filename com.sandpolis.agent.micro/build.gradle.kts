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

import org.gradle.language.cpp.tasks.CppCompile
import org.gradle.nativeplatform.tasks.LinkExecutable

plugins {
	id("eclipse")
	id("cpp-application")
}

eclipse {
	project {
		name = project.name
		comment = project.name
	}
}

application {
	privateHeaders {
		from(project.file("src/main/headers"))
		from(project(":module:com.sandpolis.core.foundation").file("gen/main/cpp"))
		from(project(":module:com.sandpolis.core.instance").file("gen/main/cpp"))
		from(project(":module:com.sandpolis.core.net").file("gen/main/cpp"))
	}
	source {
		from(project.file("src/main/cpp"))
		from(project(":module:com.sandpolis.core.foundation").file("gen/main/cpp"))
		from(project(":module:com.sandpolis.core.instance").file("gen/main/cpp"))
		from(project(":module:com.sandpolis.core.net").file("gen/main/cpp"))
	}
}

tasks.withType<LinkExecutable> {
	lib {
		file("/usr/local/lib/libprotobuf-lite.a")
	}
}

tasks.withType<CppCompile> {
	dependsOn(":module:com.sandpolis.core.foundation:generateProto")
	dependsOn(":module:com.sandpolis.core.instance:generateProto")
	dependsOn(":module:com.sandpolis.core.net:generateProto")
	compilerArgs.set(listOf("-pthread"))
}
