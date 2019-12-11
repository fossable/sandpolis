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
package com.sandpolis.gradle.codegen

import com.sandpolis.gradle.codegen.AttributeGenerator

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * This plugin adds code generation tasks to the build.
 *
 * @author cilki
 */
public class CodeGen implements Plugin<Project> {

	void apply(Project project) {

		// Look for attribute descriptor
		if (project.file("attribute.json").exists()) {
			project.tasks.getByName('compileJava').dependsOn(project.task("generateAttributes", type: AttributeGenerator))
		}

		// Setup protobuf compilation
		if (project.file("src/main/proto").exists()) {
			// TODO
		}
	}
}
