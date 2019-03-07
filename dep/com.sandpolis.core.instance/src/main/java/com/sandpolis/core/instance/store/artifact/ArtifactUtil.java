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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.sandpolis.core.instance.Environment.EnvPath.JLIB;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.google.common.io.Files;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.soi.Dependency.SO_DependencyMatrix.Artifact;
import com.sandpolis.core.util.NetUtil;

/**
 * Utilities for managing dependency artifacts.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class ArtifactUtil {

	private static final Logger log = LoggerFactory.getLogger(ArtifactUtil.class);

	/**
	 * Get an artifact's file from the environment's library directory.
	 *
	 * @param artifact The artifact
	 * @return The artifact's local file
	 */
	public static Path getArtifactFile(Artifact artifact) {
		checkNotNull(artifact);

		return getArtifactFile(artifact.getCoordinates());
	}

	/**
	 * Get an artifact's file from the environment's library directory.
	 *
	 * @param artifact The artifact
	 * @return The artifact's local file
	 */
	public static Path getArtifactFile(String artifact) {
		checkNotNull(artifact);

		return Environment.get(JLIB).resolve(getArtifactFilename(artifact));
	}

	/**
	 * Convert an artifact's coordinates to a filename.
	 *
	 * @param coordinates The artifact's coordinates
	 * @return The artifact's filename
	 */
	public static String getArtifactFilename(String coordinates) {
		String formatMessage = "Coordinate format: group:artifact:version";

		checkNotNull(coordinates);
		checkArgument(coordinates.indexOf(':') != -1, formatMessage);
		checkArgument(coordinates.indexOf(':') != coordinates.lastIndexOf(':'), formatMessage);

		// Remove group ID
		coordinates = coordinates.substring(coordinates.indexOf(':') + 1);

		// Remove version if empty
		if (coordinates.endsWith(":"))
			coordinates = coordinates.substring(0, coordinates.length() - 1);

		checkArgument(!coordinates.isEmpty(), formatMessage);
		checkArgument(!coordinates.equals(":"), formatMessage);
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
	public static boolean download(Path directory, String artifact) throws IOException {
		checkNotNull(directory);
		checkNotNull(artifact);

		String[] id = artifact.split(":");
		checkArgument(id.length == 3, "Invalid artifact ID");

		String filename = id[1] + "-" + id[2] + ".jar";
		String url = "http://repo1.maven.org/maven2/" // Base URL
				+ id[0].replaceAll("\\.", "/") + "/"// Group name
				+ id[1] + "/" // Artifact name
				+ id[2] + "/" // Artifact version
				+ filename; // Artifact
		log.debug("Downloading artifact: {}", url);

		// Download the file hash first
		byte[] hash = BaseEncoding.base16().lowerCase().decode(new String(NetUtil.download(url + ".sha1")));

		File output = directory.resolve(filename).toFile();
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

	private ArtifactUtil() {
	}
}
