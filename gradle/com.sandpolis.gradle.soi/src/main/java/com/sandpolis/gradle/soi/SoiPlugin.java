/******************************************************************************
 *                                                                            *
 *                    Copyright 2017 Subterranean Security                    *
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
package com.sandpolis.gradle.soi;

import java.io.File;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSetContainer;

/**
 * The SOI plugin gathers information about the build and injects it into the
 * instance modules for use at runtime.
 * 
 * @author cilki
 */
public class SoiPlugin implements Plugin<Project> {

	@Override
	public void apply(Project root) {

		// Setup the SOI directory
		File soi = new File(root.getBuildDir().getAbsolutePath() + "/tmp_soi/soi");
		soi.mkdirs();

		// Depend on SOI tasks
		root.getTasks().getByName("jar").dependsOn(root.getTasks().create("soiBuild", SoiBuildTask.class));
		root.getTasks().getByName("jar").dependsOn(root.getTasks().create("soiMatrix", SoiMatrixTask.class));

		// Add SOI directory to source set so it's included in the jar
		SourceSetContainer sourceSets = (SourceSetContainer) root.getProperties().get("sourceSets");
		sourceSets.getByName("main").getResources().srcDir(soi.getParent());

	}
}
