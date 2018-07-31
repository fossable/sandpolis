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
import java.util.Date;

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
	private Environment() {
	}

	/**
	 * The initialization timestamp of the process.
	 */
	public static final Date JVM_TIMESTAMP = new Date(ManagementFactory.getRuntimeMXBean().getStartTime());

	/**
	 * The location of the main instance jar. May be {@code null} if test mode is
	 * enabled.
	 */
	public static final Path JAR = discoverJar();

	/**
	 * The location of the base directory.
	 */
	public static final Path BASE = discoverBase();

	/**
	 * The location of the database directory.
	 */
	public static final Path DB = discover("db");

	/**
	 * The location of the temporary directory.
	 */
	public static final Path TMP = discover("tmp");

	/**
	 * The location of the log directory.
	 */
	public static final Path LOG = discover("log");

	/**
	 * The location of the library directory for Java dependencies.
	 */
	public static final Path JLIB = discover("jlib");

	/**
	 * The location of the library directory for native dependencies.
	 */
	public static final Path NLIB = discover("nlib");

	/**
	 * The location of the payload archive.
	 */
	public static final Path GEN_OUTPUT = discover("payload");

	/**
	 * Check that the runtime environment is properly setup.
	 * 
	 * @throws RuntimeException If the environment is misconfigured
	 */
	public static void check() {
		// Run environment checks
		if (JAR != null && (!Files.exists(JAR) || Files.isDirectory(JAR)))
			throw new RuntimeException("Failed to locate instance jar.");

		if (BASE == null || !Files.exists(BASE) || !Files.isDirectory(BASE))
			throw new RuntimeException("Failed to locate base directory.");

		try {
			if (!Files.exists(DB))
				Files.createDirectories(DB);
			if (!Files.exists(TMP))
				Files.createDirectories(TMP);
			if (!Files.exists(LOG))
				Files.createDirectories(LOG);
			if (!Files.exists(JLIB))
				Files.createDirectories(JLIB);
			if (!Files.exists(NLIB))
				Files.createDirectories(NLIB);
			if (!Files.exists(GEN_OUTPUT))
				Files.createDirectories(GEN_OUTPUT);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Print the environment for debugging.
	 */
	public static void print() {
		System.out.println("\t=== ENVIRONMENT ===");
		System.out.println("JVM TIMESTAMP: " + JVM_TIMESTAMP.toString());
		System.out.println("BASE: " + BASE);
		System.out.println("DB: " + DB);
		System.out.println("TMP: " + TMP);
		System.out.println("LOG: " + LOG);
		System.out.println("JLIB: " + JLIB);
		System.out.println("GEN_OUTPUT: " + GEN_OUTPUT);
	}

	/**
	 * Locate the instance jar.
	 * 
	 * @return A {@link File} representing the main jar file or {@code null} if an
	 *         error occurred
	 */
	private static Path discoverJar() {
		try {
			Path jar = Paths.get(MainDispatch.getMain().getProtectionDomain().getCodeSource().getLocation().toURI());
			if (Files.isDirectory(jar))
				return null;
			return jar;
		} catch (Exception e) {
			return null;
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
	 * @return A {@link File} representing the subdirectory or {@code null} if an
	 *         error occurred
	 */
	private static Path discover(String sub) {
		try {
			return BASE.resolve(sub).toRealPath();
		} catch (IOException | NullPointerException e) {
			return null;
		}
	}
}
