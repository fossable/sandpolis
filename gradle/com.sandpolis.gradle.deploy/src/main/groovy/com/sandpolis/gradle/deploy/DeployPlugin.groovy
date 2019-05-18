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

	void apply(Project root) {

		// Abort if no remote configuration is found
		if(!root.file("remote.gradle").exists())
			return;

		// Apply the required SSH plugin to the root project
		root.apply(plugin: SshPlugin)

		// Load the remote settings
		root.apply(from: "remote.gradle")

		// Add the deployment tasks to each instance module
		root.subprojects { sub ->
			afterEvaluate {

				def deploy_type
				switch (getName()){
					case "com.sandpolis.charcoal":
					case "com.sandpolis.server.vanilla":
					case "com.sandpolis.client.mega":
					case "com.sandpolis.viewer.cli":
					case "com.sandpolis.viewer.jfx":
						deploy_type = DeployInstance
						break
					default:
						return
				}

				// Create deploy tasks
				remotes.each { remote ->
					def taskName = remote.user + "@" + remote.name
					task(taskName, type: deploy_type, group: 'deploy') {
						rhost = remote
						project_deploy = sub
						project_root = root

						// TODO: parse platform and directory by name rather than rely on order
						platform = remote.extensions[0].platform()
						directory = remote.extensions[1].directory() + sub.getName()
					}

					// Setup task dependencies
					setupDepends(sub.tasks.getByName(taskName), sub)
				}
			}
		}
	}
}