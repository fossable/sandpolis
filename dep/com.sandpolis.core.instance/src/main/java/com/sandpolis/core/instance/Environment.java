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
package com.sandpolis.core.instance;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This singleton contains information about the runtime environment. Only
 * standard Java classes may be used so that this class can be loaded regardless
 * of the dependency situation.<br>
 * <br>
 * 
 * Any symbolic links will be resolved before they are stored.
 * 
 * @author cilki
 * @since 4.0.0
 */
public final class Environment {

	/**
	 * The initialization timestamp of the process.
	 */
	public static final Date JVM_TIMESTAMP = new Date(ManagementFactory.getRuntimeMXBean().getStartTime());

	/**
	 * The location of the main instance jar.
	 */
	public static final Path JAR = discoverJar();

	/**
	 * The location of the base directory.
	 */
	public static final Path BASE = discoverBase();

	public enum EnvPath {

		/**
		 * The location of the database directory.
		 */
		DB,

		/**
		 * The location of the temporary directory.
		 */
		TMP,

		/**
		 * The location of the log directory.
		 */
		LOG,

		/**
		 * The location of the library directory for Java dependencies.
		 */
		JLIB,

		/**
		 * The location of the library directory for native dependencies.
		 */
		NLIB,

		/**
		 * The location of the payload archive.
		 */
		GEN;
	}

	private static Map<EnvPath, Path> envpaths;

	/**
	 * Get an environment path.
	 * 
	 * @param path The path type
	 * @return The corresponding {@link Path}
	 */
	public static Path get(EnvPath path) {
		return envpaths.get(path);
	}

	/**
	 * Load the filesystem environment.
	 * 
	 * @param paths
	 * @return Whether the environment is set up
	 */
	public static boolean load(EnvPath... paths) {
		if (envpaths != null)
			throw new IllegalStateException();

		// Run environment checks
		if (JAR != null && (!Files.exists(JAR) || Files.isDirectory(JAR)))
			throw new RuntimeException("Failed to locate instance jar.");
		if (BASE == null || !Files.exists(BASE) || !Files.isDirectory(BASE))
			throw new RuntimeException("Failed to locate base directory.");

		envpaths = Arrays.stream(paths).collect(Collectors.toUnmodifiableMap(p -> p, p -> discover(p)));

		for (Path p : envpaths.values())
			if (p == null)
				throw new RuntimeException();
		for (Path p : envpaths.values())
			if (!Files.exists(p))
				return false;
		return true;
	}

	/**
	 * Set up the filesystem environment. This method is idempotent.
	 */
	public static void setup() {
		try {
			for (Path p : envpaths.values()) {
				if (!Files.exists(p))
					Files.createDirectories(p);
				if (!Files.isDirectory(p))
					throw new RuntimeException("Expected directory: " + p);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Locate the instance jar.
	 * 
	 * @return A {@link Path} representing the main jar file
	 */
	private static Path discoverJar() {
		try {
			Path jar = Paths.get(MainDispatch.getMain().getProtectionDomain().getCodeSource().getLocation().toURI());
			if (Files.isDirectory(jar))
				return null;
			return jar;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Locate the base directory.
	 * 
	 * @return A {@link File} representing the base directory or {@code null} if an
	 *         error occurred
	 */
	private static Path discoverBase() {
		if (JAR == null)
			// Unit test mode engaged
			try {
				return Files.createTempDirectory("unit_environment_");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		return JAR.getParent();
	}

	/**
	 * Locate a subdirectory of the {@link #BASE} directory. This method resolves
	 * symbolic links.
	 * 
	 * @param sub The desired subdirectory
	 * @return A {@link Path} representing the subdirectory or {@code null} if an
	 *         error occurred
	 */
	private static Path discover(EnvPath sub) {
		try {
			return BASE.resolve(sub.name()).toFile().getCanonicalFile().toPath();
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Print the environment for debugging.
	 */
	public static void print() {
		System.out.println("\t=== ENVIRONMENT ===");
		System.out.println("JVM TIMESTAMP: " + JVM_TIMESTAMP.toString());
		System.out.println("BASE: " + BASE);
		for (EnvPath p : envpaths.keySet())
			System.out.println(p + ": " + envpaths.get(p));
	}

	private Environment() {
	}
}
