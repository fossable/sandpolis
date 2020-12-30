//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.foundation.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.zip.ZipFile;

import com.google.common.io.Resources;

/**
 * Utilities for working with jar files and their resources.
 *
 * @since 4.0.0
 */
public final class JarUtil {

	public interface ResourceParser<T> {
		public T parse(InputStream in) throws Exception;
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
	 * Read the given resource from the given zip.
	 *
	 * @param file     A zip containing the entry
	 * @param resource The resource path
	 * @param parser   The parsing function
	 * @return The parsed object
	 * @throws Exception If the file does not exist, have the required entry, could
	 *                   not be read, or could not be parsed
	 */
	public static <T> T getResource(Path file, String resource, ResourceParser<T> parser) throws Exception {
		checkNotNull(resource);
		checkNotNull(file);
		checkNotNull(parser);

		if (!resource.startsWith("/"))
			resource = "/" + resource;

		// Attempt to use the newer filesystem API first
		try (FileSystem zip = FileSystems.newFileSystem(file, (ClassLoader) null)) {
			try (var in = Files.newInputStream(zip.getPath(resource))) {
				return parser.parse(in);
			}
		} catch (ProviderNotFoundException e) {
			// ZipFile fallback
			try (ZipFile zip = new ZipFile(file.toFile())) {
				try (var in = zip.getInputStream(zip.getEntry(resource))) {
					return parser.parse(in);
				}
			}
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

		URL url = getResourceUrl(file, resource);
		if (url == null)
			throw new IOException();

		return Resources.asByteSource(url).size();
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
