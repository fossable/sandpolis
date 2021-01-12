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

import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.creating

open class ConfigExtension {

    /**
     * The plugin's Sandpolis ID.
     */
    lateinit var id: String

    /**
     * The plugin's Maven group and artifact name.
     */
    lateinit var coordinate: String

    /**
     * The plugin's user-friendly name.
     */
    lateinit var name: String

    /**
     * The plugin's user-friendly description.
     */
    lateinit var description: String
}

val cert = project.file(project.name + ".cert").readText()
        .replace("\\R".toRegex(), "")
        .replace("-----BEGIN CERTIFICATE-----", "")
        .replace("-----END CERTIFICATE-----", "")
val extension = project.extensions.create("sandpolis_plugin", ConfigExtension::class)

// Create a jar task for the plugin archive
val pluginArchive by tasks.creating(Jar::class)

// Rename the original jar task
project.tasks.named<Jar>("jar") {
    archiveBaseName.set("core")
    archiveVersion.set("")
}
pluginArchive.dependsOn(project.tasks.getByName("jar"))

// Add components to the plugin archive
project.subprojects {
    afterEvaluate {
        val jarTask = tasks.findByName("jar") as? Jar
        if (jarTask != null) {

            // Clear version identifier
            jarTask.archiveVersion.set("")

            // Setup task dependency
            pluginArchive.dependsOn(jarTask)

            // Add artifact to the plugin archive
            pluginArchive.from(jarTask.archiveFile) {
                into(parent!!.name)
            }
        }
    }
}

project.afterEvaluate {

    // Add core component
    val jarTask = tasks.findByName("jar") as Jar
    pluginArchive.from(jarTask.archiveFile)

    // Setup plugin manifest
    pluginArchive.manifest {
        attributes(mapOf(
                "Plugin-Id" to extension.id,
                "Plugin-Coordinate" to extension.coordinate + ":" + project.version,
                "Plugin-Name" to extension.name,
                "Plugin-Description" to extension.description,
                "Plugin-Cert" to cert
        ))
    }
}
