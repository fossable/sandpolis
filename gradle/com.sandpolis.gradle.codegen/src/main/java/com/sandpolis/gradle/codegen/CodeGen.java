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

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import com.sandpolis.gradle.codegen.state.impl.AttributeValueGenerator;
import com.sandpolis.gradle.codegen.state.impl.JavaFxSTGenerator;
import com.sandpolis.gradle.codegen.state.impl.CoreSTGenerator;

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

			if (configuration.stateTree == null && project.file("state.json").exists()) {
				configuration.stateTree = project.file("state.json");
			}

			// Generate state tree if configured
			if (configuration.stateTree != null) {

				if (!configuration.stateTree.exists())
					throw new RuntimeException("Specification not found");

				// Create the generation tasks
				if (configuration.javaFxStateTree) {
					project.getTasks().getByName("compileJava")
							.dependsOn(project.getTasks().create("generateJavaFxStateTree", JavaFxSTGenerator.class));
				}
				if (configuration.coreStateTree) {
					project.getTasks().getByName("compileJava")
							.dependsOn(project.getTasks().create("generateCoreStateTree", CoreSTGenerator.class));

				}
			}

			// Generate attribute implementations
			if (project.getName().equals("com.sandpolis.core.instance")) {
				project.getTasks().getByName("compileJava").dependsOn(
						project.getTasks().create("generateStateTreeAttributeValues", AttributeValueGenerator.class));
			}

			// Setup automatic protobuf compilation
			if (project.file("src/main/proto").exists()) {
				// TODO
			}
		});
	}
}
