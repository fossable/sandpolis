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
package com.sandpolis.gradle.deploy

import com.sandpolis.gradle.deploy.task.DeployInstance

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ProjectDependency

import org.hidetake.gradle.ssh.plugin.SshPlugin;

/**
 * This class adds deployment tasks to all compatible modules according to the
 * remote configuration (remote.gradle).
 *
 * @author cilki
 */
public class DeployPlugin implements Plugin<Project> {

	/**
	 * A recursive method that sets up dependencies for the deploy task by
	 * walking down the project's dependencies.
	 */
	void setupDepends(Task deploy, Project dependent) {

		if (dependent.configurations.hasProperty('runtimeClasspath')) {
			addDepends(deploy, dependent)
		} else {
			dependent.afterEvaluate { addDepends(deploy, dependent) }
		}
	}

	/**
	 * A convenience method that links the project's jar task to the deploy
	 * task. This method should only be called by setupDepends.
	 */
	void addDepends(Task deploy, Project dependent) {
		deploy.dependsOn(dependent.tasks.getByName('jar'))

		dependent.configurations.runtimeClasspath.getAllDependencies().withType(ProjectDependency).each {
			setupDepends(deploy, it.getDependencyProject())
		}
	}

	void apply(Project project) {

		// Setup deploy extension
		def extension = project.extensions.create('deploy', DeployExtension)

		// Apply the required SSH plugin to the root project
		project.apply(plugin: SshPlugin)

		// Load the remote settings
		project.apply(from: project.rootProject.file("remote.gradle"))

		// Add the deployment tasks
		project.afterEvaluate {

			// Create a task for each remote
			project.remotes.each { remote ->
				def taskName = remote.user + "@" + remote.name
				project.task(taskName, type: DeployInstance, group: 'deploy') {
					rhost = remote
					project_deploy = project
					jvmArgs = project.extensions.deploy.jvmArgs
				}

				// Setup task dependencies
				setupDepends(project.tasks.getByName(taskName), project)
			}
		}
	}
}
