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
package com.sandpolis.gradle.deploy.task

import org.gradle.api.tasks.*

import com.sandpolis.gradle.deploy.RemoteTask

class DeployInstance extends RemoteTask {

	void linux(directory) {
		project_deploy.ssh.run {
			session(rhost) {
				// Kill the process
				execute 'pkill -9 -f "java -jar ' + directory + '"', ignoreError: true

				// Reset working directory
				remove directory
				execute 'mkdir -p ' + directory + '/lib'

				// Reset Java Preferences
				// execute 'rm -rf ~/.java/.userPrefs/com'

				// Transfer instance binary
				put from: project_deploy.jar.archivePath, into: directory + '/lib'

				// Transfer libraries
				put from: project_deploy.configurations.runtimeClasspath, into: directory + '/lib'

				// Check for screen session
				if(!execute('screen -ls', ignoreError: true).contains(project_deploy.name))
					// Create a new detached session
					execute 'screen -d -m -S ' + project_deploy.name

				// Run the artifact
				execute 'screen -S ' + project_deploy.name + ' -X stuff "clear && java --module-path ' + directory + '/lib ' + jvmArgs.join(' ') + '\n"'
			}
		}
	}

	void windows(directory) {
		project_deploy.ssh.run {
			session(rhost) {
				// Kill the process
				execute "wmic path win32_process where \"CommandLine Like '%${project_deploy.name}.jar%'\" call terminate", ignoreError: true

				// Reset working directory
				remove directory
				execute 'md "' + directory + '/lib"'

				// Reset Java Preferences
				execute 'reg delete "HKEY_CURRENT_USER\\Software\\JavaSoft\\Prefs" /f', ignoreError: true

				// Transfer instance binary
				put from: project_deploy.jar.archivePath, into: directory + '/lib'

				// Transfer libraries
				put from: project_deploy.configurations.runtimeClasspath, into: directory + '/lib'

				// Run the artifact manually
			}
		}
	}
}
