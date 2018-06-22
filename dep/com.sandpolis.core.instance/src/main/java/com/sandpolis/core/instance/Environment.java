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
import java.net.URISyntaxException;
import java.util.Date;

/**
 * This class contains static information about the runtime environment. Only
 * standard Java classes are used so that this class can be loaded at any time.
 * 
 * Any symbolic links will be resolved before being stored.
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
	 * The location of the main instance jar.
	 */
	public static final File JAR = discoverJar();

	/**
	 * The location of the base directory.
	 */
	public static final File BASE = discoverBase();

	/**
	 * The location of the database directory.
	 */
	public static final File DB = discover("db");

	/**
	 * The location of the temporary directory.
	 */
	public static final File TMP = discover("tmp");

	/**
	 * The location of the log directory.
	 */
	public static final File LOG = discover("log");

	/**
	 * The location of the library directory for Java dependencies.
	 */
	public static final File JLIB = discover("jlib");

	/**
	 * The location of the library directory for native dependencies.
	 */
	public static final File NLIB = discover("nlib");

	/**
	 * Check that the runtime environment is properly setup.
	 * 
	 * @throws RuntimeException
	 *             If the environment is misconfigured
	 */
	public static void check() {
		// Run environment checks
		if (JAR == null || !JAR.exists() || !JAR.isFile())
			throw new RuntimeException("Failed to locate instance jar.");

		if (BASE == null || !BASE.exists() || !BASE.isDirectory())
			throw new RuntimeException("Failed to locate base directory.");

		if (!DB.exists())
			DB.mkdirs();
		if (!TMP.exists())
			TMP.mkdirs();
		if (!LOG.exists())
			LOG.mkdirs();
		if (!JLIB.exists())
			JLIB.mkdirs();
		if (!NLIB.exists())
			NLIB.mkdirs();
	}

	/**
	 * Locate the instance jar.
	 * 
	 * @return A {@link File} representing the main jar file or {@code null} if an
	 *         error occurred
	 */
	private static File discoverJar() {
		try {
			return new File(MainDispatch.getMain().getProtectionDomain().getCodeSource().getLocation().toURI());
		} catch (URISyntaxException e) {
			return null;
		}
	}

	/**
	 * Locate the base directory.
	 * 
	 * @return A {@link File} representing the base directory or {@code null} if an
	 *         error occurred
	 */
	private static File discoverBase() {
		if (JAR == null)
			return null;
		return JAR.getParentFile();
	}

	/**
	 * Locate a subdirectory of the {@link #BASE} directory. This method resolves
	 * symbolic links.
	 * 
	 * @param sub
	 *            The desired subdirectory
	 * @return A {@link File} representing the subdirectory or {@code null} if an
	 *         error occurred
	 */
	private static File discover(String sub) {
		try {
			return new File(BASE.getAbsolutePath() + File.separator + sub).getCanonicalFile();
		} catch (IOException | NullPointerException e) {
			return null;
		}
	}

}
