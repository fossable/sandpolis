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

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import com.sandpolis.core.foundation.soi.Dependency.SO_DependencyMatrix;

/**
 * This task parses the multiproject and outputs a soi binary containing
 * dependency metadata.
 *
 * @author cilki
 */
public class SoiMatrixTask extends DefaultTask {

	/**
	 * The {@link SO_DependencyMatrix} binary.
	 */
	private File so_matrix = new File(getProject().getBuildDir().getAbsolutePath() + "/tmp_soi/soi/matrix.bin");

	@OutputFile
	public File getSoMatrix() {
		return so_matrix;
	}

	@TaskAction
	public void run() {

		// Write object
		try (FileOutputStream out = new FileOutputStream(so_matrix)) {
			new DependencyProcessor(getProject()).build().writeTo(out);
		} catch (IOException e) {
			throw new RuntimeException("Failed to write SO_DependencyMatrix", e);
		}
	}
}
