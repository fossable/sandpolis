/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.server.vanilla.gen;

import static com.sandpolis.core.instance.Environment.EnvPath.GEN;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.sandpolis.core.instance.Environment;
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
	private Path resultArchive;

	public FileGenerator(GenConfig config) {
		super(config);
	}

	@Override
	public void generate() throws Exception {
		super.generate();

		if (result != null) {
			resultArchive = Environment.get(GEN).resolve(config.getId() + "." + packager.getFileExtension());
			Files.write(resultArchive, result);

			computeMetadata();
		}
	}

	/**
	 * Get the location of the generator output in the archive directory.
	 *
	 * @return The location of the result
	 */
	public Path getResultPath() {
		return resultArchive;
	}

	/**
	 * Read the generator output from the archive.
	 *
	 * @return A byte array containing the generator output
	 * @throws IOException
	 */
	public byte[] readResult() throws IOException {
		return Files.readAllBytes(resultArchive);
	}

	/**
	 * Compute output metadata and store in the report.
	 */
	protected void computeMetadata() {
		if (result == null)
			throw new IllegalStateException();

		report.setOutputSize(result.length);

		try {
			report.setOutputSha256(ByteSource.wrap(result).hash(Hashing.sha256()).toString());
			report.setOutputSha512(ByteSource.wrap(result).hash(Hashing.sha512()).toString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
