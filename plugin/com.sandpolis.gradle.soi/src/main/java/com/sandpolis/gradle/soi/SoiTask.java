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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import com.sandpolis.core.proto.soi.Build.SO_Build;
import com.sandpolis.core.proto.soi.Dependency.SO_DependencyMatrix;

/**
 * This task parses the multiproject and writes soi binaries to the output
 * directory.
 * 
 * @author cilki
 */
public class SoiTask extends DefaultTask {

	/**
	 * The root project.
	 */
	private Project root = getProject();

	/**
	 * The SOI output directory.
	 */
	private File soi = new File(root.getBuildDir().getAbsolutePath() + "/soi");

	@OutputDirectory
	public File getSoi() {
		return soi;
	}

	@TaskAction
	public void run() {
		writeBuildSO();

		writeMatrixSO();
	}

	/**
	 * Gather and write build information to the soi directory.
	 */
	private void writeBuildSO() {
		SO_Build.Builder so = SO_Build.newBuilder();
		so.setTime(System.currentTimeMillis());
		so.setVersion((String) root.findProperty("BUILD_VERSION"));

		String number = (String) root.findProperty("TRAVIS_BUILD_NUMBER");
		if (number != null)
			so.setNumber(Integer.parseInt(number));

		so.setPlatform(String.format("%s (%s %s)", System.getProperty("os.name"), System.getProperty("os.version"),
				System.getProperty("os.arch")));
		so.setJavaVersion(
				String.format("%s (%s)", System.getProperty("java.version"), System.getProperty("java.vendor")));
		so.setGradleVersion(root.getGradle().getGradleVersion());

		// Write object
		File output = new File(soi.getAbsolutePath() + "/build.bin");
		try (FileOutputStream out = new FileOutputStream(output)) {
			so.build().writeTo(out);
		} catch (IOException e) {
			throw new RuntimeException("Failed to write SO_Build", e);
		}
	}

	/**
	 * Compute and write the dependency matrix to the soi directory.
	 */
	private void writeMatrixSO() {
		DependencyProcessor processor = new DependencyProcessor(root);

		root.subprojects(sub -> {
			Map<String, Configuration> conf = sub.getConfigurations().getAsMap();

			if (conf.containsKey("runtimeClasspath")) {
				for (ResolvedDependency dep : conf.get("runtimeClasspath").getResolvedConfiguration()
						.getFirstLevelModuleDependencies()) {
					if (dep.getModuleArtifacts().size() != 1)
						continue;

					processor.add(sub.getName(), dep);
				}
			}
		});

		SO_DependencyMatrix so = processor.build();
		testMatrixSO(so);

		// Write object
		File output = new File(soi.getAbsolutePath() + "/matrix.bin");
		try (FileOutputStream out = new FileOutputStream(output)) {
			so.writeTo(out);
		} catch (IOException e) {
			throw new RuntimeException("Failed to write SO_DependencyMatrix", e);
		}
	}

	/**
	 * Test a {@link SO_DependencyMatrix} for cycles.
	 * 
	 * @param so The dependency matrix
	 */
	private void testMatrixSO(SO_DependencyMatrix so) {
		// TODO test for cycles
	}

}
