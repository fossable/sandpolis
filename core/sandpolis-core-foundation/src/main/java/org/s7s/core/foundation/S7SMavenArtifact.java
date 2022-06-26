//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.foundation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.list;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.xml.sax.SAXException;

import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.google.common.io.Files;

/**
 * Represents a Maven Central artifact.
 */
public record S7SMavenArtifact(String groupId, String artifactId, String version, String classifier, String filename) {

	private static final String MAVEN_CENTRAL_URL = "https://repo1.maven.org/maven2";

	public static S7SMavenArtifact of(String group, String artifact, String version, String classifier) {
		if (classifier == null) {
			return of(group, artifact, version);
		}
		return of(group + ":" + artifact + ":" + version + ":" + classifier);
	}

	public static S7SMavenArtifact of(String group, String artifact, String version) {
		return of(group + ":" + artifact + ":" + version);
	}

	public static S7SMavenArtifact of(String coordinates) {
		checkNotNull(coordinates);
		checkArgument(!coordinates.isBlank());

		String[] gav = coordinates.split(":");

		// Trim fields
		for (int i = 0; i < gav.length; i++)
			gav[i] = gav[i].trim();

		switch (gav.length) {
		case 1:
			if (gav[0].isBlank())
				throw new IllegalArgumentException();

			return new S7SMavenArtifact(null, gav[0], null, null, String.format("%s.jar", gav[0]));
		case 2:
			return new S7SMavenArtifact(gav[0].isBlank() ? null : gav[0], gav[1], null, null,
					String.format("%s.jar", gav[1]));
		case 3:
			return new S7SMavenArtifact(gav[0].isBlank() ? null : gav[0], gav[1], gav[2], null,
					String.format("%s-%s.jar", gav[1], gav[2]));
		case 4:
			return new S7SMavenArtifact(gav[0].isBlank() ? null : gav[0], gav[1], gav[2], gav[3],
					String.format("%s-%s-%s.jar", gav[1], gav[2], gav[3]));
		default:
			throw new IllegalArgumentException();
		}
	}

	/**
	 * @return The artifact's bytes
	 * @throws IOException
	 */
	public InputStream download() throws IOException {

		String url = MAVEN_CENTRAL_URL + "/" // Base URL
				+ groupId.replaceAll("\\.", "/") + "/"// Group name
				+ artifactId + "/" // Artifact name
				+ version + "/" // Artifact version
				+ filename; // Artifact filename

		return new URL(url).openConnection().getInputStream();
	}

	/**
	 * Query the latest version of the artifact.
	 *
	 * @return The artifact's latest version string
	 * @throws IOException
	 */
	public String getLatestVersion() throws IOException {

		String url = MAVEN_CENTRAL_URL + "/" // Base URL
				+ groupId.replaceAll("\\.", "/") + "/"// Group name
				+ artifactId // Artifact name
				+ "/maven-metadata.xml";

		try (var in = new URL(url).openStream()) {
			return XPathFactory.newDefaultInstance().newXPath().evaluate("/metadata/versioning/latest",
					DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in));
		} catch (XPathExpressionException | SAXException | ParserConfigurationException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Check the artifact's hash.
	 *
	 * @param artifact The artifact to check
	 * @return Whether the given file matches the remote hash
	 * @throws IOException
	 */
	@SuppressWarnings("deprecation")
	public boolean checkHash(Path artifact) throws IOException {

		if (!exists(artifact))
			throw new FileNotFoundException();

		String url = MAVEN_CENTRAL_URL + "/" // Base URL
				+ groupId.replaceAll("\\.", "/") + "/"// Group name
				+ artifactId + "/" // Artifact name
				+ version + "/" // Artifact version
				+ filename // Artifact filename
				+ ".sha1";

		// Download the file hash
		byte[] hash = BaseEncoding.base16().lowerCase().decodingStream(new InputStreamReader(new URL(url).openStream()))
				.readAllBytes();

		// Compare hash
		return Arrays.equals(Files.asByteSource(artifact.toFile()).hash(Hashing.sha1()).asBytes(), hash);
	}

	/**
	 * Get the artifact file from the given directory.
	 *
	 * @param directory The directory containing artifacts
	 * @return The artifact's local file
	 */
	public Path getArtifactFile(Path directory) {
		checkNotNull(directory);

		return directory.resolve(filename);
	}

	/**
	 * Find all versions of the artifact in the given directory. The results will be
	 * in decreasing version order if applicable.
	 *
	 * @param directory The directory to search
	 * @return A stream of all matching artifacts
	 * @throws IOException
	 */
	public Stream<Path> findFileVersions(Path directory) throws IOException {
		return list(directory).filter(path -> path.getFileName().toString().matches("^" + artifactId + "-.*\\.jar"))
				// Sort by semantic version number
				.sorted((path1, path2) -> {
					String[] v1 = path1.getFileName().toString().replace(artifactId, "").split("\\.");
					String[] v2 = path2.getFileName().toString().replace(artifactId, "").split("\\.");

					for (int i = 0; i < Math.min(v1.length, v2.length); i++) {
						try {
							int compare = Integer.compare(Integer.parseInt(v1[i]), Integer.parseInt(v2[i]));
							if (compare != 0)
								return compare;

						} catch (NumberFormatException e) {
							return 0;
						}
					}
					return 0;
				});
	}

}
