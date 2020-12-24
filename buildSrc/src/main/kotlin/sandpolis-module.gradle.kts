//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//

import java.io.ByteArrayOutputStream

// Determine the module's version according to git

var gitDescribe = ByteArrayOutputStream()

project.exec {
	commandLine = listOf("git", "describe", "--tags")
	workingDir = project.getProjectDir()
	standardOutput = gitDescribe
	errorOutput = ByteArrayOutputStream()
	setIgnoreExitValue(true)
}

var v = gitDescribe.toString().trim()

if (!v.startsWith("v")) {

	project.version = "0.0.0"

} else {

	// Remove version prefix
	v = v.substring(1)

	// Remove version suffix if it's a release
	if (v.contains("-0-")) {
		v = v.substring(0, v.indexOf("-"))
	}

	project.version = v
}
