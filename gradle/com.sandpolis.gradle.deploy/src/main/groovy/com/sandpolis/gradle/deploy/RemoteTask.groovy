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
