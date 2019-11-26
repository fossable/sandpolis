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
package com.sandpolis.core.soi;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.soi.Build.SO_Build;
import com.sandpolis.core.soi.Dependency.SO_DependencyMatrix;

/**
 * A SOI object utility.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class SoiUtil {

	private static final Logger log = LoggerFactory.getLogger(SoiUtil.class);

	/**
	 * Read the {@link SO_Build} object from the given zip.
	 *
	 * @param file A zip containing an entry: /soi/build.bin
	 * @return A new build object
	 * @throws IOException If the file does not exist, have the required entry, or
	 *                     could not be read
	 */
	public static SO_Build readBuild(Path file) throws IOException {
		Objects.requireNonNull(file);

		try (FileSystem zip = FileSystems.newFileSystem(file, (ClassLoader) null)) {
			try (var in = Files.newInputStream(zip.getPath("/soi/build.bin"))) {
				return SO_Build.parseFrom(in);
			}
		} catch (ProviderNotFoundException e) {
			// ZipFile fallback
			log.debug("Reading object with zip fallback");
			try (ZipFile zip = new ZipFile(file.toFile())) {
				try (var in = zip.getInputStream(zip.getEntry("/soi/build.bin"))) {
					return SO_Build.parseFrom(in);
				}
			}
		}
	}

	/**
	 * Read the {@link SO_DependencyMatrix} object from the given zip.
	 *
	 * @param file A zip containing an entry: /soi/matrix.bin
	 * @return A new dependency matrix
	 * @throws IOException If the file does not exist, have the required entry, or
	 *                     could not be read
	 */
	public static SO_DependencyMatrix readMatrix(Path file) throws IOException {
		Objects.requireNonNull(file);

		try (FileSystem zip = FileSystems.newFileSystem(file, (ClassLoader) null)) {
			try (var in = Files.newInputStream(zip.getPath("/soi/matrix.bin"))) {
				return SO_DependencyMatrix.parseFrom(in);
			}
		} catch (ProviderNotFoundException e) {
			// ZipFile fallback
			log.debug("Reading object with zip fallback");
			try (ZipFile zip = new ZipFile(file.toFile())) {
				try (var in = zip.getInputStream(zip.getEntry("/soi/matrix.bin"))) {
					return SO_DependencyMatrix.parseFrom(in);
				}
			}
		}
	}

	public static Dep getMatrix(Path file) throws IOException {
		SO_DependencyMatrix matrix = readMatrix(file);
		return new Dep(matrix, matrix.getArtifact(0));
	}

	private SoiUtil() {
	}
}
