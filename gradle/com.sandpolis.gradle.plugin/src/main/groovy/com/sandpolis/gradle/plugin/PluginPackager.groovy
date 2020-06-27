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

		// Create a jar task for the plugin archive
		def pluginArchive = root.tasks.create('pluginArchive', Jar.class)

		// Rename the original jar task
		root.tasks.getByName('jar').archiveBaseName = 'core'
		root.tasks.getByName('jar').archiveVersion = ''
		pluginArchive.dependsOn(root.tasks.getByName('jar'))

		// Add components to the plugin archive
		root.subprojects {
			afterEvaluate {
				if (tasks.findByPath('jar') != null) {

					// Clear version identifier
					tasks.getByName('jar').archiveVersion = ''

					// Setup task dependency
					pluginArchive.dependsOn(tasks.getByName('jar'))

					// Add artifact to the plugin archive
					pluginArchive.from(tasks.getByName('jar').archivePath, { into parent.name })
				}
			}
		}

		root.afterEvaluate {

			// Add core component
			pluginArchive.from(root.tasks.getByName('jar').archivePath)

			// Setup plugin manifest
			pluginArchive.manifest {
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
