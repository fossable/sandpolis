//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.foundation;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderNotFoundException;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Resources;

public record S7SJarFile(Path file) {

	private static final Logger log = LoggerFactory.getLogger(S7SJarFile.class);

	public static S7SJarFile of(Path file) {
		return new S7SJarFile(file);
	}

	public static S7SJarFile of(File file) {
		return of(file.toPath());
	}

	public static S7SJarFile of(String path) {
		return of(Paths.get(path));
	}

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
	public Attributes getManifest() throws IOException {

		try (JarFile jar = new JarFile(file.toFile(), false)) {
			if (jar.getManifest() == null)
				throw new NoSuchFileException("Manifest not found");

			return jar.getManifest().getMainAttributes();
		}
	}

	/**
	 * Retrieve the value of a manifest attribute from the given jar.
	 *
	 * @param attribute The attribute to query
	 * @return The attribute's value
	 * @throws IOException
	 */
	public Optional<String> getManifestValue(String attribute) throws IOException {
		checkNotNull(attribute);

		try {
			return Optional.ofNullable(getManifest().getValue(attribute));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	/**
	 * Read the given resource if available.
	 *
	 * @param resource The resource path
	 * @param parser   The parsing function
	 * @return The parsed object
	 * @throws IOException
	 */
	public <T> Optional<T> getResource(String resource, ResourceParser<T> parser) throws IOException {
		checkNotNull(resource);
		checkNotNull(parser);

		if (!resource.startsWith("/"))
			resource = "/" + resource;

		// Attempt to use the newer filesystem API first
		try (FileSystem zip = FileSystems.newFileSystem(file, (ClassLoader) null)) {
			try (var in = Files.newInputStream(zip.getPath(resource))) {
				try {
					return Optional.ofNullable(parser.parse(in));
				} catch (Exception e) {
					log.error("Failed to parse resource", e);
					return Optional.empty();
				}
			}
		} catch (ProviderNotFoundException e) {
			// ZipFile fallback
			try (ZipFile zip = new ZipFile(file.toFile())) {
				try (var in = zip.getInputStream(zip.getEntry(resource))) {
					try {
						return Optional.ofNullable(parser.parse(in));
					} catch (Exception e1) {
						log.error("Failed to parse resource", e1);
						return Optional.empty();
					}
				}
			}
		}
	}

	/**
	 * Read the given resource if available.
	 *
	 * @param resource The resource path
	 * @return The resource bytes
	 * @throws IOException
	 */
	public Optional<byte[]> getResource(String resource) throws IOException {
		return getResource(resource, in -> in.readAllBytes());
	}

	/**
	 * Calculate the size of a resource by (probably) reading it entirely.
	 *
	 * @param resource Absolute location of target resource within the given jar
	 * @return The size of the target resource in bytes
	 * @throws IOException
	 */
	public long getResourceSize(String resource) throws IOException {
		checkNotNull(resource);

		URL url = getResourceUrl(resource);
		if (url == null)
			throw new IOException();

		return Resources.asByteSource(url).size();
	}

	/**
	 * Get a {@link URL} representing some entry of a jar file. The file and entry
	 * must both exist.
	 *
	 * @param resource Absolute location of target resource within the given jar
	 * @return A URL representing the resource or {@code null} if not found
	 * @throws IOException
	 */
	public URL getResourceUrl(String resource) throws IOException {
		checkNotNull(resource);

		if (!resource.startsWith("/"))
			resource = "/" + resource;

		if (!resourceExists(resource))
			return null;

		return new URL(String.format("jar:file:%s!%s", file.toAbsolutePath().toString(), resource));
	}

	/**
	 * Check if a resource exists.
	 *
	 * @param resource Absolute location of a target resource
	 * @return Whether the target resource exists
	 * @throws IOException
	 */
	public boolean resourceExists(String resource) throws IOException {
		checkNotNull(resource);

		if (!resource.startsWith("/"))
			resource = "/" + resource;

		try (FileSystem fs = FileSystems.newFileSystem(file, (ClassLoader) null)) {
			return Files.exists(fs.getPath(resource));
		} catch (ProviderNotFoundException e) {
			throw new IOException("Illegal file type");
		}
	}

}
