/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
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

  	// Look for attribute files
  	if (project.file("attribute.yml").exists()) {
  		project.tasks.getByName('compileJava').dependsOn(project.task("generateAttributes", type: AttributeGenerator))
  	}

  	// TODO Protobuf
  }
}
