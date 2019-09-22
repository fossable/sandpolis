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

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.*

import org.hidetake.groovy.ssh.core.Remote
import org.hidetake.groovy.ssh.session.BadExitStatusException

/**
 * The RemoteTask deploys the given module to the remote host via SSH.
 *
 * @author cilki
 */
class RemoteTask extends DefaultTask {

	/**
	 * The remote host
	 */
	@Input
	Remote rhost

	/**
	 * The project to deploy
	 */
	@Input
	Project project_deploy

	/**
	 * The deployment JVM arguments.
	 */
	@Input
	List<String> jvmArgs

	@TaskAction
	void action() {
		def platform
		def directory

		// Figure out OS type first
		project_deploy.ssh.run {
			session(rhost) {
				try {
					execute 'whoami'

					platform = "linux"
					directory = "/home/" + rhost.user + "/.deploy/" + project_deploy.name
				} catch (BadExitStatusException e) {
					platform = "windows"
					directory = "C:\\Users\\" + rhost.user + "\\.deploy\\"+ project_deploy.name
				}
			}
		}

		// Execute the deployment
		"$platform"(directory)
	}
}
