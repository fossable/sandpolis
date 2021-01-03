//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.instance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.Date;

import org.slf4j.Logger;

import com.sandpolis.core.foundation.Config;
import com.sandpolis.core.foundation.Config.ConfigProperty;
import com.sandpolis.core.foundation.util.TextUtil;

/**
 * {@link Environment} contains nformation about the runtime file hierarchy.
 *
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
	LIB(discoverPath(Config.PATH_LIB, JAR.path() == null ? null : JAR.path().getParent())),

	/**
	 * The configuration directory.
	 */
	CFG(discoverPath(Config.PATH_CFG, LIB.path() == null ? null : LIB.path().resolveSibling("config"))),

	/**
	 * The database data directory.
	 */
	DATA(discoverPath(Config.PATH_DATA, LIB.path() == null ? null : LIB.path().resolveSibling("data"))),

	/**
	 * The log directory.
	 */
	LOG(discoverPath(Config.PATH_LOG, LIB.path() == null ? null : LIB.path().resolveSibling("log"))),

	/**
	 * The plugin directory.
	 */
	PLUGIN(discoverPath(Config.PATH_PLUGIN, LIB.path() == null ? null : LIB.path().resolveSibling("plugin"))),

	/**
	 * The temporary directory.
	 */
	TMP(discoverPath(Config.PATH_TMP, System.getProperty("java.io.tmpdir")));

	public static void clearEnvironment() {
		LIB.set(null);
		LOG.set(null);
		PLUGIN.set(null);
		DATA.set(null);
		CFG.set(null);
		TMP.set(null);
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

	private static Path discoverPath(Object... candidates) {
		for (Object candidate : candidates) {
			if (candidate == null) {
				continue;
			}

			if (candidate instanceof ConfigProperty property) {
				if (property.defined()) {
					return Paths.get(property.value().get().toString());
				}
			} else if (candidate instanceof String path) {
				return Paths.get(path);
			} else if (candidate instanceof Path path) {
				return path;
			}
		}
		return null;
	}

	/**
	 * Print environment details on startup.
	 *
	 * @param log  The output log
	 * @param name The instance name
	 */
	public static void printEnvironment(Logger log, String name) {
		log.info("Launching {} ({})", TextUtil.rainbowText(name),
				Core.SO_BUILD.getProperty("instance.version", "?.?.?"));
		log.debug("Core version: {}", Core.SO_BUILD.getProperty("core.version", "?.?.?"));
		log.debug("  Build timestamp: {}", new Date(Long.parseLong(Core.SO_BUILD.getProperty("build.timestamp", "0"))));
		log.debug("Runtime timestamp: {}", new Date());
		log.debug("   Build platform: {}", Core.SO_BUILD.getProperty("build.platform", "Unknown"));
		log.debug(" Runtime platform: {} ({})", System.getProperty("os.name"), System.getProperty("os.arch"));
		log.debug("        Build JVM: {}", Core.SO_BUILD.getProperty("build.java.version", "Unknown"));
		log.debug("      Runtime JVM: {} ({})", System.getProperty("java.version"), System.getProperty("java.vendor"));
		log.debug("         JAR path: {}", JAR.path);
		log.debug("         LIB path: {}", LIB.path);
		log.debug("         LOG path: {}", LOG.path);
		log.debug("      PLUGIN path: {}", PLUGIN.path);
		log.debug("        DATA path: {}", DATA.path);
		log.debug("         CFG path: {}", CFG.path);
		log.debug("         TMP path: {}", TMP.path);
	}

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
}
