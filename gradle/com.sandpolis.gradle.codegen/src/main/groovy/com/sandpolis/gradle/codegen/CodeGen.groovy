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

import com.sandpolis.gradle.codegen.document.CoreDocumentBindingsGenerator
import com.sandpolis.gradle.codegen.document.JavaFxDocumentBindingsGenerator

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * This plugin adds code generation tasks to the build.
 *
 * @author cilki
 */
class CodeGen implements Plugin<Project> {

	void apply(Project project) {

		// Register the config extension
		def configuration = project.extensions.create('codegen', ConfigExtension)

		project.afterEvaluate {

			// Generate document bindings if configured
			if (configuration.documentBindings != null) {

				// Look for the document specification
				if (!project.file("attribute.json").exists())
					throw new RuntimeException("Specification not found")

				// Create the task
				switch (configuration.documentBindings) {
				case "javafx":
					project.tasks.getByName('compileJava').dependsOn(project.task("generateJavaFxBindings", type: JavaFxDocumentBindingsGenerator))
					break
				case "core":
					project.tasks.getByName('compileJava').dependsOn(project.task("generateCoreBindings", type: CoreDocumentBindingsGenerator))
					break
				default:
					throw new RuntimeException("Specification not found")
				}
			}

			// Setup automatic protobuf compilation
			if (project.file("src/main/proto").exists()) {
				// TODO
			}
		}
	}
}
