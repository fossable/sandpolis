/******************************************************************************
 *                                                                            *
 *                    Copyright 2016 Subterranean Security                    *
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
package com.sandpolis.core.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import com.google.common.io.Resources;

/**
 * Utilities for working with jar files and their resources.
 * 
 * @author cilki
 * @since 4.0.0
 */
public final class JarUtil {

	/**
	 * Retrieve the value of a manifest attribute from the given jar.
	 * 
	 * @param file      The target jar file
	 * @param attribute The attribute to query
	 * @return The attribute's value or {@code null} if not found
	 * @throws IOException
	 */
	public static String getManifestValue(Path file, String attribute) throws IOException {
		checkNotNull(file);
		checkNotNull(attribute);

		return getManifestValue(file.toFile(), attribute);
	}

	/**
	 * Retrieve the value of a manifest attribute from the given jar.
	 * 
	 * @param file      The target jar file
	 * @param attribute The attribute to query
	 * @return The attribute's value or {@code null} if not found
	 * @throws IOException
	 */
	public static String getManifestValue(File file, String attribute) throws IOException {
		checkNotNull(file);
		checkNotNull(attribute);

		return getManifest(file).getValue(attribute);
	}

	/**
	 * Retrieve the attribute map from the manifest of the given jar.
	 * 
	 * @param file The target jar file
	 * @return The jar's manifest attributes
	 * @throws IOException
	 */
	public static Attributes getManifest(File file) throws IOException {
		checkNotNull(file);

		try (JarFile jar = new JarFile(file, false)) {
			if (jar.getManifest() == null)
				throw new NoSuchFileException("Manifest not found");

			return jar.getManifest().getMainAttributes();
		}
	}

	/**
	 * Calculate the size of a resource by (probably) reading it entirely.
	 * 
	 * @param file     The target jar file
	 * @param resource Absolute location of target resource within the given jar
	 * @return The size of the target resource in bytes
	 * @throws IOException
	 */
	public static long getResourceSize(File file, String resource) throws IOException {
		checkNotNull(resource);
		checkNotNull(file);

		return getResourceSize(file.toPath(), resource);
	}

	/**
	 * Calculate the size of a resource by (probably) reading it entirely.
	 * 
	 * @param file     The target jar file
	 * @param resource Absolute location of target resource within the given jar
	 * @return The size of the target resource in bytes
	 * @throws IOException
	 */
	public static long getResourceSize(Path file, String resource) throws IOException {
		checkNotNull(resource);
		checkNotNull(file);

		if (!resource.startsWith("/"))
			resource = "/" + resource;

		return Resources.asByteSource(getResourceUrl(file, resource)).size();
	}

	/**
	 * Get a {@link URL} representing some entry of a jar file.
	 * 
	 * @param file     The target jar file
	 * @param resource Absolute location of target resource within the given jar
	 * @return A URL representing the resource
	 * @throws MalformedURLException
	 */
	public static URL getResourceUrl(Path file, String resource) throws MalformedURLException {
		checkNotNull(file);
		checkNotNull(resource);

		return new URL(String.format("jar:file:%s!%s", file.toAbsolutePath().toString(), resource));
	}

	private JarUtil() {
	}
}
