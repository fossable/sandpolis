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
package com.sandpolis.gradle.soi;

import java.io.File;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.AbstractCopyTask;

/**
 * The SOI plugin gathers information about the build and injects it into the
 * instance modules for use at runtime.
 *
 * @author cilki
 */
public class SoiPlugin implements Plugin<Project> {

	@Override
	public void apply(Project root) {

		// Depend on SOI tasks
		AbstractCopyTask processResources = (AbstractCopyTask) root.getTasks().getByName("processResources");
		processResources.dependsOn(root.getTasks().create("soiBuild", SoiBuildTask.class));
		processResources.dependsOn(root.getTasks().create("soiMatrix", SoiMatrixTask.class));

		// Configure task to include SOI directory
		processResources.from(new File(root.getBuildDir().getAbsolutePath() + "/tmp_soi"));

	}
}
