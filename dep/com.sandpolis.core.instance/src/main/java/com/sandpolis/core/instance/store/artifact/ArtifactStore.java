/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
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
package com.sandpolis.core.instance.store.artifact;

import static com.sandpolis.core.instance.Environment.EnvPath.JLIB;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.google.common.io.Files;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.proto.soi.Dependency.SO_DependencyMatrix.Artifact;
import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.util.NetUtil;

/**
 * Utilities for managing dependency artifacts.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class ArtifactStore {
	private ArtifactStore() {
	}

	private static final Logger log = LoggerFactory.getLogger(ArtifactStore.class);

	/**
	 * Get the root artifact for the given instance.
	 *
	 * @param instance The instance type
	 * @return The artifact for the instance
	 */
	public static Artifact getInstanceArtifact(Instance instance) {
		if (instance == null)
			throw new IllegalArgumentException();

		for (Artifact artifact : Core.SO_MATRIX.getArtifactList()) {
			String coordinates = artifact.getCoordinates();
			if (coordinates.startsWith(":") && coordinates.endsWith(":")
					&& coordinates.contains(instance.toString().toLowerCase())) {
				return artifact;
			}
		}

		return null;
	}

	/**
	 * Get all direct and transitive dependencies of an instance.
	 *
	 * @param instance The instance type
	 * @return An unordered stream of dependencies
	 */
	public static Stream<Artifact> getDependencies(Instance instance) {
		if (instance == null)
			throw new IllegalArgumentException();

		Stream<Artifact> all = Stream.empty();
		for (int id : getInstanceArtifact(instance).getDependencyList()) {
			all = Stream.concat(all, getDependencies(Core.SO_MATRIX.getArtifact(id)));
		}

		return all.distinct();
	}

	/**
	 * Get a stream of an artifact and all its dependencies using a recursive call.
	 *
	 * @param artifact The artifact
	 * @return A stream of the artifact and its dependencies
	 */
	private static Stream<Artifact> getDependencies(Artifact artifact) {
		return Stream.concat(Stream.of(artifact), artifact.getDependencyList().stream()
				.map(id -> Core.SO_MATRIX.getArtifact(id)).flatMap(ArtifactStore::getDependencies));
	}

	/**
	 * Get an artifact's file from the environment's library directory.
	 *
	 * @param artifact The artifact
	 * @return The artifact's local file
	 */
	public static File getArtifactFile(Artifact artifact) {
		return Environment.get(JLIB).resolve(getArtifactFilename(artifact.getCoordinates())).toFile();
	}

	/**
	 * Convert an artifact's coordinates to a filename.
	 *
	 * @param coordinates The artifact's coordinates
	 * @return The artifact's filename
	 */
	public static String getArtifactFilename(String coordinates) {
		if (coordinates == null)
			throw new IllegalArgumentException();
		if (coordinates.indexOf(':') == -1)
			throw new IllegalArgumentException();
		if (coordinates.indexOf(':') == coordinates.lastIndexOf(':'))
			throw new IllegalArgumentException();

		// Remove group ID
		coordinates = coordinates.substring(coordinates.indexOf(':') + 1);

		// Remove version if empty
		if (coordinates.endsWith(":"))
			coordinates = coordinates.substring(0, coordinates.length() - 1);

		return coordinates.replaceAll(":", "-") + ".jar";
	}

	/**
	 * Download an artifact from Maven Central to the library directory. If an
	 * artifact already exists in the directory, its hash will be checked against
	 * the Maven Central hash.
	 *
	 * @param directory The output directory
	 * @param artifact  The artifact identifier in standard Gradle form:
	 *                  group:name:version
	 * @return Whether the artifact was actually downloaded
	 * @throws IOException
	 */
	@SuppressWarnings("deprecation")
	public static boolean download(File directory, String artifact) throws IOException {
		if (artifact == null)
			throw new IllegalArgumentException();

		String[] id = artifact.split(":");
		if (id.length != 3)
			throw new IllegalArgumentException("Invalid artifact ID");

		String filename = id[1] + "-" + id[2] + ".jar";
		String url = "http://repo1.maven.org/maven2/" // Base URL
				+ id[0].replaceAll("\\.", "/") + "/"// Group name
				+ id[1] + "/" // Artifact name
				+ id[2] + "/" // Artifact version
				+ filename; // Artifact
		log.debug("Downloading artifact: {}", url);

		// Download the file hash first
		byte[] hash = BaseEncoding.base16().lowerCase().decode(new String(NetUtil.download(url + ".sha1")));

		File output = new File(directory.getAbsolutePath() + "/" + filename);
		if (output.exists()) {
			// Check the hash of the existing file to catch partial downloads
			if (Arrays.equals(Files.asByteSource(output).hash(Hashing.sha1()).asBytes(), hash))
				return false;

			output.delete();
		}

		NetUtil.download(url, output);

		// Check hash of download
		if (!Arrays.equals(Files.asByteSource(output).hash(Hashing.sha1()).asBytes(), hash))
			throw new IOException("Hash verification failed");

		return true;
	}

}
