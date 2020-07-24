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
package com.sandpolis.gradle.codegen;

import com.sandpolis.gradle.codegen.profile_tree.impl.AttributeImplementationGenerator;
import com.sandpolis.gradle.codegen.profile_tree.impl.StandardProfileTreeGenerator;
import com.sandpolis.gradle.codegen.profile_tree.impl.JavaFxProfileTreeGenerator;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * This plugin adds code generation tasks to the build.
 *
 * @author cilki
 */
public class CodeGen implements Plugin<Project> {

	public void apply(Project project) {

		// Register the config extension
		var configuration = project.getExtensions().create("codegen", ConfigExtension.class);

		project.afterEvaluate(p -> {

			// Generate document bindings if configured
			if (configuration.profileTreeType != null) {

				// Find the specification file
				if (configuration.profileTreeSpec == null)
					throw new RuntimeException("Specification not defined");
				if (!configuration.profileTreeSpec.exists())
					throw new RuntimeException("Specification not found");

				// Create the task
				switch (configuration.profileTreeType) {
				case "javafx":
					project.getTasks().getByName("compileJava").dependsOn(
							project.getTasks().create("generateProfileTree", JavaFxProfileTreeGenerator.class));
					break;
				case "core":
					project.getTasks().getByName("compileJava").dependsOn(
							project.getTasks().create("generateProfileTree", StandardProfileTreeGenerator.class));
					break;
				default:
					throw new RuntimeException("Specification not found");
				}
			}

			// Generate attribute implementations
			if (project.getName().equals("com.sandpolis.core.instance")) {
				project.getTasks().getByName("compileJava").dependsOn(project.getTasks()
						.create("generateAttributeImplementations", AttributeImplementationGenerator.class));
			}

			// Setup automatic protobuf compilation
			if (project.file("src/main/proto").exists()) {
				// TODO
			}
		});
	}
}
