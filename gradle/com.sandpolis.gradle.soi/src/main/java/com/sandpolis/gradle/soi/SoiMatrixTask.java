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

import com.sandpolis.core.soi.Dependency.SO_DependencyMatrix;

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
