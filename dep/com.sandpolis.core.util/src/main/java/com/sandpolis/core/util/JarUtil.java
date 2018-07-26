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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.jar.JarFile;

import com.google.common.io.Resources;

/**
 * Utilities for working with jar files and their resources.
 * 
 * @author cilki
 * @since 4.0.0
 */
public final class JarUtil {
	private JarUtil() {
	}

	/**
	 * Retrieve the value of a manifest attribute from the specified jar.
	 * 
	 * @param attribute The attribute to query
	 * @param jarFile   The target jar file
	 * @return The attribute's value
	 * @throws IOException
	 */
	public static String getManifestValue(String attribute, File jarFile) throws IOException {
		if (attribute == null)
			throw new IllegalArgumentException();
		if (jarFile == null)
			throw new IllegalArgumentException();

		try (JarFile jar = new JarFile(jarFile)) {
			if (jar.getManifest() == null)
				throw new IOException("Manifest not found");

			return jar.getManifest().getMainAttributes().getValue(attribute);
		}
	}

	/**
	 * Calculate the size of a resource by (probably) reading it entirely.
	 * 
	 * @param path    Absolute location of target resource within the given jar
	 * @param jarFile The target jar file
	 * @return The size of the target resource in bytes
	 * @throws IOException
	 */
	public static long getResourceSize(String path, File jarFile) throws IOException {
		if (path == null)
			throw new IllegalArgumentException();
		if (jarFile == null)
			throw new IllegalArgumentException();

		if (!path.startsWith("/"))
			path = "/" + path;

		return Resources.asByteSource(new URL("jar:file:" + jarFile.getAbsolutePath() + "!" + path)).size();
	}

}
