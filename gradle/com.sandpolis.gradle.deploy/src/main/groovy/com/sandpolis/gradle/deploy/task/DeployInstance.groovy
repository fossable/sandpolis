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
package com.sandpolis.gradle.deploy.task

import org.gradle.api.tasks.*

import com.sandpolis.gradle.deploy.RemoteTask

class DeployInstance extends RemoteTask {

	void lin() {
		project_root.ssh.run {
			session(rhost) {
				// Kill the process
				execute 'pkill -9 -f "java -jar ' + directory + '"', ignoreError: true

				// Reset working directory
				remove directory
				execute 'mkdir -p ' + directory + '/lib'

				// Reset Java Preferences
				execute 'rm -rf ~/.java/.userPrefs/com'

				// Transfer instance binary
				put from: project_deploy.jar.archivePath, into: directory

				// Transfer libraries
				put from: project_deploy.configurations.runtimeClasspath, into: directory + '/lib'

				// Transfer all core plugins for server only
				if (project_deploy.name.equals("com.sandpolis.server.vanilla"))
					project_root.subprojects { sub ->
						if (sub.name.startsWith("com.sandpolis.plugin"))
							put from: sub.jar.archivePath, into: directory + '/lib'
					}

				// Check for screen session
				if(!execute('screen -ls', ignoreError: true).contains(project_deploy.name))
					// Create a new detached session
					execute 'screen -d -m -S ' + project_deploy.name

				// Run the artifact
				execute 'screen -S ' + project_deploy.name + ' -X stuff "clear && java -jar ' + directory + '/' + project_deploy.archivesBaseName + '-' + project_deploy.version + '.jar\n"'
			}
		}
	}

	void win() {
		project_root.ssh.run {
			session(rhost) {
				// Kill the process
				execute "wmic path win32_process where \"CommandLine Like '%${project_deploy.name}.jar%'\" call terminate", ignoreError: true

				// Reset working directory
				remove directory
				execute 'md "' + directory + '/lib"'

				// Reset Java Preferences
				execute 'reg delete "HKEY_CURRENT_USER\\Software\\JavaSoft\\Prefs" /f', ignoreError: true

				// Transfer instance binary
				put from: project_deploy.jar.archivePath, into: directory

				// Transfer libraries
				put from: project_deploy.configurations.runtimeClasspath, into: directory + '/lib'

				// Transfer all core plugins for server only
				if (project_deploy.name.equals("com.sandpolis.server.vanilla"))
					project_root.subprojects { sub ->
						if (sub.name.startsWith("com.sandpolis.plugin"))
							put from: sub.jar.archivePath, into: directory + '/lib'
					}

				// Run the artifact manually
			}
		}
	}

	void osx() {
		// No difference for now
		lin()
	}
}
