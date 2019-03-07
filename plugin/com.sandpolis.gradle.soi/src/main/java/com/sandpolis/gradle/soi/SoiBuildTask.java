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

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import com.sandpolis.core.soi.Build.SO_Build;

/**
 * This task outputs a soi binary (called build.bin) containing build metadata.
 * 
 * @author cilki
 */
public class SoiBuildTask extends DefaultTask {

	/**
	 * The {@link SO_Build} binary.
	 */
	private File so_build = new File(getProject().getBuildDir().getAbsolutePath() + "/tmp_soi/soi/build.bin");

	@OutputFile
	public File getSoBuild() {
		return so_build;
	}

	@TaskAction
	public void run() {
		SO_Build.Builder so = SO_Build.newBuilder();

		// Build time
		so.setTime(System.currentTimeMillis());

		// Application version
		so.setVersion((String) getProject().findProperty("BUILD_VERSION"));

		// Build number
		String number = (String) getProject().findProperty("TRAVIS_BUILD_NUMBER");
		if (number != null)
			so.setNumber(Integer.parseInt(number));

		// Build platform
		so.setPlatform(String.format("%s (%s %s)", System.getProperty("os.name"), System.getProperty("os.version"),
				System.getProperty("os.arch")));

		// Java version
		so.setJavaVersion(
				String.format("%s (%s)", System.getProperty("java.version"), System.getProperty("java.vendor")));

		// Gradle version
		so.setGradleVersion(getProject().getGradle().getGradleVersion());

		// Write object
		try (FileOutputStream out = new FileOutputStream(so_build)) {
			so.build().writeTo(out);
		} catch (IOException e) {
			throw new RuntimeException("Failed to write SO_Build", e);
		}

	}

}
