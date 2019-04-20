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
package com.sandpolis.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy

/**
 * This plugin packages a Sandpolis plugin into an installable archive.
 *
 * @author cilki
 */
public class PluginPackager implements Plugin<Project> {

	void apply(Project project) {
		def cert = project.file(project.name + ".cert").text.replace("\n", "").replace("-----BEGIN CERTIFICATE-----", "").replace("-----END CERTIFICATE-----", "")
		def extension = project.extensions.create('sandpolis_plugin', ConfigExtension)

		project.subprojects {
			afterEvaluate {
				if (tasks.findByPath('jar') != null) {

					// Setup dependency
					project.tasks.getByName('jar').dependsOn(tasks.getByName('jar'))
					
					// Add artifact to root project's jar task
					project.tasks.getByName('jar')
						.from(tasks.getByName('jar').outputs.files.getFiles()[0].getParent(),
							{into parent.name})
				}
			}
		}

		// Setup plugin manifests
		project.allprojects {
			afterEvaluate {
				def name = extension.id.substring(extension.id.lastIndexOf('.') + 1)
				name = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase()

				if (tasks.findByPath('jar') != null) {
					jar {
						manifest {
							attributes(
								'Plugin-Id': extension.id,
								'Plugin-Version': extension.version,
								'Plugin-Name': extension.name,
								'Plugin-Description': extension.description,
								'Plugin-Class': "${extension.id}.${name}Plugin",
								'Plugin-Cert': cert
							)
						}
					}
				}
			}
		}
	}
}
