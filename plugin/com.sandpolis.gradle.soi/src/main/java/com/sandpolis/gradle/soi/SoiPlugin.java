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
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSetContainer;

/**
 * The SOI plugin gathers information about the build and injects it into the
 * instance modules for use at runtime.<br>
 * <br>
 * There are two general uses for the information this plugin provides:
 * <ul>
 * <li>Showing build statistics to the user</li>
 * <li>The server requires a dependency graph to dynamically build client
 * stubs</li>
 * </ul>
 * 
 * @author cilki
 */
public class SoiPlugin implements Plugin<Project> {

	/**
	 * The destination for static object binaries.
	 */
	private File soi;

	@Override
	public void apply(Project root) {

		// Reset the SOI directory
		soi = new File(root.getBuildDir().getAbsolutePath() + "/soi");
		soi.mkdirs();
		for (File f : soi.listFiles())
			f.delete();

		// Create the SOI task
		Task task = root.getTasks().create("soi", SoiTask.class);

		// Iterate over instance modules
		root.subprojects(e -> {
			e.afterEvaluate(sub -> {
				if (sub.getPlugins().hasPlugin(JavaPlugin.class)
						&& !sub.getPlugins().hasPlugin(JavaLibraryPlugin.class)) {

					// Depend on SOI task
					sub.getTasks().getByName("jar").dependsOn(task);

					// Add SOI directory to source set so it's included in the instance jar
					SourceSetContainer sourceSets = (SourceSetContainer) sub.getProperties().get("sourceSets");
					sourceSets.getByName("main").getResources().srcDir(soi.getParent());
				}
			});
		});
	}
}
