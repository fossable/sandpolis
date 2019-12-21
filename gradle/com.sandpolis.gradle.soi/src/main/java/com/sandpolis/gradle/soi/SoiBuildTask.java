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
import java.io.FileOutputStream;
import java.io.IOException;

import org.gradle.api.Task;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.specs.Spec;

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

	public SoiBuildTask() {
		getOutputs().upToDateWhen(new Spec<Task>() {
			public boolean isSatisfiedBy(Task element) {
				return false;
			}
		});
	}

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
		so.setVersion((String) getProject().findProperty("SANDPOLIS_VERSION"));

		// Build mode
		String mode = (String) getProject().findProperty("DEVELOPMENT");
		so.setDevelopment(mode == null || mode.equalsIgnoreCase("true"));

		// Build platform
		so.setPlatform(String.format("%s (%s)", System.getProperty("os.name"), System.getProperty("os.arch")));

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
