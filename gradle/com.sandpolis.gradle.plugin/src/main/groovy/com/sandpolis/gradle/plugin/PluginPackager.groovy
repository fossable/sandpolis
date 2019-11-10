/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Jar

/**
 * This plugin packages a Sandpolis plugin into an installable archive.
 *
 * @author cilki
 */
public class PluginPackager implements Plugin<Project> {

	void apply(Project root) {
		def cert = root.file(root.name + ".cert").text.replaceAll("\\R", "").replace("-----BEGIN CERTIFICATE-----", "").replace("-----END CERTIFICATE-----", "")
		def extension = root.extensions.create('sandpolis_plugin', ConfigExtension)

		// Create jar task
		def pluginArchive = root.tasks.create('pluginArchive', Jar.class)
		pluginArchive.from(root.sourceSets.main.output)
		pluginArchive.archiveBaseName = 'core'
		pluginArchive.archiveVersion = ''

		root.tasks.getByName('jar').dependsOn(pluginArchive)

		root.subprojects {
			afterEvaluate {
				if (tasks.findByPath('jar') != null) {
					// Clear version identifier
					tasks.getByName('jar').archiveVersion = ''

					// Setup task dependency
					root.tasks.getByName('jar').dependsOn(tasks.getByName('jar'))

					// Add artifact to root project's jar task
					root.tasks.getByName('jar').from(tasks.getByName('jar').archivePath, {into parent.name})
				}
			}
		}

		root.afterEvaluate {

			root.tasks.getByName('jar').from(root.tasks.getByName('pluginArchive').archivePath)
			root.tasks.getByName('jar').exclude {
				!it.path.endsWith('.jar')
			}

			// Setup manifest
			root.tasks.getByName('jar').manifest {
				attributes(
					'Plugin-Id': extension.id,
					'Plugin-Coordinate': extension.coordinate + ':' + root.version,
					'Plugin-Name': extension.name,
					'Plugin-Description': extension.description,
					'Plugin-Cert': cert
				)
			}
		}
	}
}
