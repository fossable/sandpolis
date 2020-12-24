//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.gradle.codegen;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.Delete;

import com.sandpolis.gradle.codegen.state.AttributeValueGenerator;
import com.sandpolis.gradle.codegen.state.CoreSTGenerator;
import com.sandpolis.gradle.codegen.state.JavaFxSTGenerator;

/**
 * This Gradle plugin adds code generation tasks to the build.
 */
public class CodeGen implements Plugin<Project> {

	public void apply(Project project) {

		// Register the config extension
		var configuration = project.getExtensions().create("codegen", ConfigExtension.class);

		project.afterEvaluate(p -> {

			// Prepare to add dependencies on these tasks
			Task generateProto = project.getTasks().findByName("generateProto");
			Task compileJava = project.getTasks().findByName("compileJava");
			Task compileKotlin = project.getTasks().findByName("compileKotlin");
			Task clean = project.getTasks().findByName("clean");

			if (configuration.stateTree == null && project.file("state.json").exists()) {
				configuration.stateTree = project.file("state.json");
			}

			// Generate state tree if configured
			if (configuration.stateTree != null) {

				if (!configuration.stateTree.exists())
					throw new RuntimeException("Specification not found");

				// Create the generation tasks
				if (configuration.javaFxStateTree) {
					Task generateJavaFxStateTree = project.getTasks().create("generateJavaFxStateTree",
							JavaFxSTGenerator.class);

					if (generateProto != null) {
						generateJavaFxStateTree.dependsOn(generateProto);
					}
					if (compileKotlin != null) {
						compileKotlin.dependsOn(generateJavaFxStateTree);
					}
					compileJava.dependsOn(generateJavaFxStateTree);
				} else if (configuration.coreStateTree) {
					Task generateCoreStateTree = project.getTasks().create("generateCoreStateTree",
							CoreSTGenerator.class);

					if (generateProto != null) {
						generateCoreStateTree.dependsOn(generateProto);
					}
					compileJava.dependsOn(generateCoreStateTree);
				}
			}

			// Generate server attribute value implementations
			if (project.getName().equals("com.sandpolis.core.server")) {
				Task generateHibernateAttributeValueImplementations = project.getTasks()
						.create("generateHibernateAttributeValueImplementations", AttributeValueGenerator.class);

				compileJava.dependsOn(generateHibernateAttributeValueImplementations);
			}

			// Remove generated sources in clean task
			clean.dependsOn(project.getTasks().create("cleanGeneratedSources", Delete.class, task -> {
				task.delete(project.file("gen/main/java"));
			}));

		});
	}
}
