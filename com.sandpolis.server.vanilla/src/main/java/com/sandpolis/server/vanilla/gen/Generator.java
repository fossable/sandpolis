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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import com.sandpolis.core.instance.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.sandpolis.core.proto.util.Generator.GenConfig;
import com.sandpolis.core.proto.util.Generator.GenReport;

import static com.google.common.io.Files.getNameWithoutExtension;

/**
 * The parent class of all output generators.
 *
 * @author cilki
 * @since 4.0.0
 */
public abstract class Generator implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(Generator.class);

	/**
	 * The generator's configuration.
	 */
	protected GenConfig config;

	/**
	 * The generation report.
	 */
	protected GenReport.Builder report;

	/**
	 * The final result of the generation.
	 */
	protected byte[] result;

	/**
	 * The final result of the generation.
	 */
	protected Path resultArchive;

	protected Generator(GenConfig config) {
		this.config = Objects.requireNonNull(config);
	}

	/**
	 * Compute output metadata.
	 *
	 * @throws IOException
	 */
	protected void computeMetadata() throws IOException {
		if (result == null)
			throw new IllegalStateException();

		report.setResult(true);
		report.setOutputSize(result.length);
		report.setOutputSha256(ByteSource.wrap(result).hash(Hashing.sha256()).toString());
		report.setOutputSha512(ByteSource.wrap(result).hash(Hashing.sha512()).toString());
		report.setDuration(System.currentTimeMillis() - report.getDuration());
	}

	/**
	 * Performs the generation synchronously.
	 */
	protected abstract byte[] generate() throws Exception;

	/**
	 * Get the generation report. Each invocation of this method returns a new (yet
	 * equivalent) object.
	 *
	 * @return The completed generation report
	 */
	public GenReport getReport() {
		if (report == null)
			throw new IllegalStateException("The generator has not been started");

		return report.build();
	}

	/**
	 * Get the generation result.
	 *
	 * @return The final result
	 */
	public byte[] getResult() {
		return result;
	}

	/**
	 * Get the location of the generator output in the archive directory.
	 *
	 * @return The location of the result
	 */
	public Path getResultPath() {
		return resultArchive;
	}

	@Override
	public void run() {
		if (report != null)
			throw new IllegalStateException("The generator has already been started");

		report = GenReport.newBuilder().setTimestamp(System.currentTimeMillis())
				.setDuration(System.currentTimeMillis());

		try {
			result = generate();
			computeMetadata();

			// Write to archive
			int seq = Files.list(Environment.GEN.path()).mapToInt(path -> {
				return Integer.parseInt(getNameWithoutExtension(path.getFileName().toString()));
			}).sorted().findFirst().orElse(-1) + 1;

			Files.write(Environment.GEN.path().resolve("" + seq), result);
		} catch (Exception e) {
			log.error("Generation failed", e);
			report.setResult(false);
		}
	}
}
