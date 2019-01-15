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
				execute 'mkdir -p ' + directory + '/jlib'

				// Reset Java Preferences
				execute 'rm -rf ~/.java/.userPrefs/com'

				// Transfer instance binary
				put from: project_deploy.jar.archivePath, into: directory
				
				// Transfer libraries
				put from: project_deploy.configurations.runtimeClasspath, into: directory + '/jlib'
				
				// Transfer all core plugins for server only
				if (project_deploy.name.equals("com.sandpolis.server"))
					project_root.subprojects { sub ->
						if (sub.name.startsWith("com.sandpolis.plugin"))
							put from: sub.jar.outputs.files, into: directory + '/jlib'
					}

				// Check for screen session
				if(!execute('screen -ls', ignoreError: true).contains(project_deploy.getName()))
					// Create a new detached session
					execute 'screen -d -m -S ' + project_deploy.getName()

				// Run the artifact
				execute 'screen -S ' + project_deploy.getName() + ' -X stuff "clear && java -jar ' + directory + '/' + project_deploy.getName() + '.jar\n"'
			}
		}
	}

	void win() {
		project_root.ssh.run {
			session(rhost) {
				// Kill the process
				execute 'taskkill /f /t /im ' + project_deploy.getName() + '.jar', ignoreError: true

				// Reset working directory
				remove directory
				execute 'mkdir ' + directory + '/jlib'

				// Reset Java Preferences
				execute 'reg delete "HKEY_CURRENT_USER\\Software\\JavaSoft\\Prefs"'

				// Transfer instance binary
				put from: project_deploy.jar.archivePath, into: directory
				
				// Transfer libraries
				put from: project_deploy.configurations.runtimeClasspath, into: directory + '/jlib'
				
				// Transfer all core plugins for server only
				if (project_deploy.name.equals("com.sandpolis.server"))
					project_root.subprojects { sub ->
						if (sub.name.startsWith("com.sandpolis.plugin"))
							put from: sub.jar.outputs.files, into: directory + '/jlib'
					}

				// Run the artifact
				// TODO
			}
		}
	}

	void osx() {
		// No difference for now
		lin()
	}
}