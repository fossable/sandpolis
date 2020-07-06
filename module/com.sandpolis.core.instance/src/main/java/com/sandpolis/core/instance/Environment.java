//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
package com.sandpolis.core.instance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.Date;

import org.slf4j.Logger;

import com.sandpolis.core.foundation.util.TextUtil;

/**
 * This class contains important information about the runtime file hierarchy.
 *
 * @author cilki
 * @since 4.0.0
 */
public enum Environment {

	/**
	 * The main instance jar file.
	 */
	JAR(discoverJar()),

	/**
	 * The library directory for Java modules.
	 */
	LIB(JAR.path() == null ? null : JAR.path().getParent()),

	/**
	 * The log directory.
	 */
	LOG(LIB.path() == null ? null : LIB.path().resolveSibling("log")),

	/**
	 * The plugin directory.
	 */
	PLUGIN(LIB.path() == null ? null : LIB.path().resolveSibling("plugin")),

	/**
	 * The database data directory.
	 */
	DB(LIB.path() == null ? null : LIB.path().resolveSibling("db")),

	/**
	 * The payload archive.
	 */
	GEN(LIB.path() == null ? null : LIB.path().resolveSibling("gen")),

	/**
	 * The temporary directory.
	 */
	TMP(Paths.get(System.getProperty("java.io.tmpdir")));

	/**
	 * The absolute {@link Path} of the environment path.
	 */
	private Path path;

	/**
	 * Build a {@link EnvPath} without a default.
	 */
	private Environment() {
	}

	/**
	 * @param def The default path
	 */
	private Environment(Path def) {
		this.path = def.toAbsolutePath();
	}

	/**
	 * Get the path.
	 *
	 * @return The path or {@code null} if none
	 */
	public Path path() {
		return path;
	}

	/**
	 * Set the path unless {@code null}.
	 *
	 * @param path The new path or {@code null}
	 * @return {@code this}
	 */
	public Environment set(String path) {
		if (path != null)
			this.path = Paths.get(path).toAbsolutePath();
		return this;
	}

	/**
	 * Require that the environment path be readable at runtime.
	 *
	 * @return {@code this}
	 * @throws IOException
	 */
	public Environment requireReadable() throws IOException {
		if (!Files.exists(path))
			Files.createDirectories(path);

		if (!Files.isReadable(path))
			throw new IOException("Unreadable directory");

		return this;
	}

	/**
	 * Require that the environment path be readable and writable at runtime.
	 *
	 * @return {@code this}
	 * @throws IOException
	 */
	public Environment requireWritable() throws IOException {
		if (!Files.exists(path))
			Files.createDirectories(path);

		if (!Files.isReadable(path))
			throw new IOException("Unreadable directory");

		if (!Files.isWritable(path))
			throw new IOException("Unwritable directory");

		return this;
	}

	/**
	 * Locate the instance jar by querying the {@link ProtectionDomain} of the
	 * instance class.
	 *
	 * @return A {@link Path} to the main jar file or {@code null}
	 */
	private static Path discoverJar() {
		if (MainDispatch.getMain() == null)
			// Called before dispatch
			return null;

		try {
			return Paths.get(MainDispatch.getMain().getProtectionDomain().getCodeSource().getLocation().toURI());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Print environment details on startup.
	 *
	 * @param log  The output log
	 * @param name The instance name
	 */
	public static void printEnvironment(Logger log, String name) {
		log.info("Launching {} ({})", TextUtil.rainbowText(name), Core.SO_BUILD.getVersion());
		log.debug("Build Environment:");
		log.debug("  Timestamp: {}", new Date(Core.SO_BUILD.getTime()));
		log.debug("   Platform: {}", Core.SO_BUILD.getPlatform());
		log.debug("     Gradle: {}", Core.SO_BUILD.getGradleVersion());
		log.debug("       Java: {}", Core.SO_BUILD.getJavaVersion());
		log.debug("Runtime Environment:");
		log.debug("  Timestamp: {}", new Date());
		log.debug("   Platform: {} ({})", System.getProperty("os.name"), System.getProperty("os.arch"));
		log.debug("       Java: {} ({})", System.getProperty("java.version"), System.getProperty("java.vendor"));
		log.debug("  Libraries: {}", LIB.path);
	}
}
