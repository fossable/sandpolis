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
package com.sandpolis.core.foundation.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.util.Optional;
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
	 * @return The attribute's value
	 * @throws IOException
	 */
	public static Optional<String> getManifestValue(Path file, String attribute) throws IOException {
		checkNotNull(file);
		checkNotNull(attribute);

		return getManifestValue(file.toFile(), attribute);
	}

	/**
	 * Retrieve the value of a manifest attribute from the given jar.
	 *
	 * @param file      The target jar file
	 * @param attribute The attribute to query
	 * @return The attribute's value
	 * @throws IOException
	 */
	public static Optional<String> getManifestValue(File file, String attribute) throws IOException {
		checkNotNull(file);
		checkNotNull(attribute);

		try {
			return Optional.ofNullable(getManifest(file).getValue(attribute));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	/**
	 * Retrieve the attribute map from the manifest of the given jar.
	 *
	 * @param file The target jar file
	 * @return The jar's manifest attributes
	 * @throws IOException
	 */
	public static Attributes getManifest(Path file) throws IOException {
		return getManifest(file.toFile());
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
	public static long getResourceSize(Path file, String resource) throws IOException {
		checkNotNull(resource);
		checkNotNull(file);

		URL url = getResourceUrl(file, resource);
		if (url == null)
			throw new IOException();

		return Resources.asByteSource(url).size();
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
	 * Get a {@link URL} representing some entry of a jar file. The file and entry
	 * must both exist.
	 *
	 * @param file     The target jar file
	 * @param resource Absolute location of target resource within the given jar
	 * @return A URL representing the resource or {@code null} if not found
	 * @throws IOException
	 */
	public static URL getResourceUrl(Path file, String resource) throws IOException {
		checkNotNull(resource);
		checkNotNull(file);

		if (!resource.startsWith("/"))
			resource = "/" + resource;

		if (!resourceExists(file, resource))
			return null;

		return new URL(String.format("jar:file:%s!%s", file.toAbsolutePath().toString(), resource));
	}

	/**
	 * Check if a resource exists within the given jar file.
	 *
	 * @param file     The target jar file
	 * @param resource Absolute location of target resource within the given jar
	 * @return Whether the target resource exists
	 * @throws IOException
	 */
	public static boolean resourceExists(Path file, String resource) throws IOException {
		checkNotNull(resource);
		checkNotNull(file);

		if (!Files.exists(file))
			throw new FileNotFoundException();

		if (!resource.startsWith("/"))
			resource = "/" + resource;

		try (FileSystem fs = FileSystems.newFileSystem(file, (ClassLoader) null)) {
			return Files.exists(fs.getPath(resource));
		} catch (ProviderNotFoundException e) {
			throw new IOException("Illegal file type");
		}
	}

	private JarUtil() {
	}
}
