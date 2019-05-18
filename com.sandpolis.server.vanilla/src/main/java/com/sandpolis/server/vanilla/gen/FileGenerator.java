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
package com.sandpolis.server.vanilla.gen;

import java.io.File;
import java.io.IOException;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.sandpolis.core.proto.util.Generator.GenConfig;

/**
 * A {@link Generator} that outputs to a file on the filesystem.
 * 
 * @author cilki
 * @since 4.0.0
 */
public abstract class FileGenerator extends Generator {

	/**
	 * The final result of the generation.
	 */
	protected File result;

	public FileGenerator(GenConfig config) {
		super(config);
	}

	/**
	 * Get the location of the generator output.
	 * 
	 * @return The location of the result.
	 */
	public File getResult() {
		return result;
	}

	/**
	 * Read the generator output into memory.
	 * 
	 * @return A byte array containing the generator output
	 * @throws IOException
	 */
	public byte[] readResult() throws IOException {
		return Files.toByteArray(result);
	}

	/**
	 * Compute file metadata and store in the report.
	 * 
	 * @return The success of the action
	 */
	@SuppressWarnings("deprecation")
	protected boolean computeMetadata() {
		if (result == null)
			return false;

		report.setOutputSize(result.length());

		try {
			report.setOutputMd5(Files.asByteSource(result).hash(Hashing.md5()).toString());
			report.setOutputSha256(Files.asByteSource(result).hash(Hashing.sha256()).toString());
			report.setOutputSha512(Files.asByteSource(result).hash(Hashing.sha512()).toString());
		} catch (IOException e) {
			return false;
		}

		return true;
	}
}
