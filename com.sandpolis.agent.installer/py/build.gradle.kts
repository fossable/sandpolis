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
}

eclipse {
	project {
		name = "com.sandpolis.agent.installer:py"
		comment = ""
	}
}

tasks.jar {
	archiveBaseName.set("sandpolis-agent-installer-py")
}
