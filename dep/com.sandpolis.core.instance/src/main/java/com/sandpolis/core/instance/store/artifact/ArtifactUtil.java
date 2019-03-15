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
import static com.sandpolis.core.instance.store.artifact.ArtifactUtil.ParsedCoordinate.fromFilename;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.zafarkhaja.semver.Version;
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
	 * @param coordinates The artifact's coordinates
	 * @return The artifact's local file
	 */
	public static Path getArtifactFile(String coordinates) {
		checkNotNull(coordinates);

		return Environment.get(JLIB).resolve(ParsedCoordinate.fromCoordinate(coordinates).filename);
	}

	/**
	 * Find all artifacts with the given artifact name. The results will be in
	 * decreasing version order if applicable.
	 * 
	 * @param artifactId The artifact to search for
	 * @return A stream of all matching artifacts
	 * @throws IOException
	 */
	public static Stream<Path> findArtifactFile(String artifactId) throws IOException {
		return java.nio.file.Files.list(Environment.get(JLIB))
				.filter(path -> path.getFileName().toString().startsWith(artifactId))
				// Sort by semantic version number
				.sorted((path1, path2) -> {
					try {
						return Version.valueOf(fromFilename(path1.getFileName().toString()).version)
								.compareTo(Version.valueOf(fromFilename(path2.getFileName().toString()).version));
					} catch (Exception e) {
						return 0;
					}
				});
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

	/**
	 * A container for an artifact's colon-delimited coordinate.
	 */
	public static class ParsedCoordinate {

		public final String coordinate;
		public final String groupId;
		public final String artifactId;
		public final String version;
		public final String filename;

		private ParsedCoordinate(String coordinate, String groupId, String artifactId, String version,
				String filename) {
			this.coordinate = coordinate == null ? "" : coordinate;
			this.groupId = groupId == null ? "" : groupId;
			this.artifactId = artifactId == null ? "" : artifactId;
			this.version = version == null ? "" : version;
			this.filename = filename == null ? "" : filename;
		}

		/**
		 * Parse an artifact's filename.
		 * 
		 * @param filename The standard filename
		 * @return A new {@link ParsedCoordinate}
		 */
		public static ParsedCoordinate fromFilename(String filename) {
			checkNotNull(filename);
			filename = filename.trim();

			String version = filename.substring(filename.lastIndexOf('-') + 1, filename.lastIndexOf(".jar"));
			try {
				Version.valueOf(version);
				String artifact = filename.substring(0, filename.lastIndexOf('-'));
				return new ParsedCoordinate(":" + artifact + ":" + version, null, artifact, version, filename);
			} catch (Exception e) {
				// Missing version
				String artifact = filename.substring(0, filename.lastIndexOf(".jar"));
				return new ParsedCoordinate(":" + artifact + ":", null, artifact, null, filename);
			}
		}

		/**
		 * Parse an artifact's coordinate.
		 * 
		 * @param artifact The coordinate to parse
		 * @return A new {@link ParsedCoordinate}
		 */
		public static ParsedCoordinate fromArtifact(Artifact artifact) {
			checkNotNull(artifact);

			return fromCoordinate(artifact.getCoordinates());
		}

		/**
		 * Parse a coordinate.
		 * 
		 * @param coordinate The coordinate to parse
		 * @return A new {@link ParsedCoordinate}
		 */
		public static ParsedCoordinate fromCoordinate(String coordinate) {
			checkNotNull(coordinate);

			// Hack to produce an empty last element if necessary
			if (coordinate.endsWith(":"))
				coordinate += " ";

			String[] gav = coordinate.split(":");
			checkArgument(gav.length == 3, "Coordinate format: group:artifact:version");

			// Trim fields
			for (int i = 0; i < gav.length; i++)
				gav[i] = gav[i].trim();

			// Build canonical filename
			String filename = gav[1];
			if (!gav[2].isEmpty())
				filename += "-" + gav[2];
			filename += ".jar";

			return new ParsedCoordinate(coordinate.trim(), gav[0], gav[1], gav[2], filename);
		}
	}

	private ArtifactUtil() {
	}
}
