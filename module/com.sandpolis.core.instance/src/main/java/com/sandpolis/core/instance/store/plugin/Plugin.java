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
package com.sandpolis.core.instance.store.plugin;

import static com.google.common.base.Preconditions.checkState;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.exists;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.common.io.MoreFiles;
import com.google.common.io.Resources;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.DocumentBindings;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.plugin.SandpolisPlugin;
import com.sandpolis.core.util.JarUtil;
import com.sandpolis.core.util.Platform.Instance;
import com.sandpolis.core.util.Platform.InstanceFlavor;

/**
 * Represents a Sandpolis plugin installed in the {@code PLUGIN} directory.
 *
 * @author cilki
 * @since 5.0.0
 */
public class Plugin extends DocumentBindings.Profile.Instance.Plugin {

	private ClassLoader classloader;

	private SandpolisPlugin handle;

	public Plugin() {
		super(null);
	}

	public void install(Path path, boolean enabled) throws IOException {
		if (hash().isPresent())
			throw new IllegalStateException();

		var manifest = JarUtil.getManifest(path.toFile());
		packageId().set(manifest.getValue("Plugin-Id"));
		coordinates().set(manifest.getValue("Plugin-Coordinate"));
		name().set(manifest.getValue("Plugin-Name"));
		description().set(manifest.getValue("Description"));
		certificate().set(manifest.getValue("Plugin-Cert"));

		// Install core
		Path core = getComponent(null, null);
		if (!exists(core))
			createDirectories(core.getParent());
		Resources.asByteSource(JarUtil.getResourceUrl(path, "core.jar")).copyTo(Files.asByteSink(core.toFile()));

		// Install components
		for (Instance instance : Instance.values()) {
			for (InstanceFlavor flavor : InstanceFlavor.values()) {
				String internalPath = String.format("%s/%s.jar", instance.toString().toLowerCase(),
						flavor.toString().toLowerCase());
				if (JarUtil.resourceExists(path, internalPath)) {
					Path component = getComponent(instance, flavor);
					if (!exists(component))
						createDirectories(component.getParent());

					Resources.asByteSource(JarUtil.getResourceUrl(path, internalPath))
							.copyTo(Files.asByteSink(component.toFile()));
				}
			}
		}

		enabled().set(enabled);
		hash().set(computeHash());
	}

	public String getVersion() {
		String[] gav = getCoordinates().split(":");
		if (gav.length == 3)
			return gav[2];
		return null;
	}

	public ClassLoader getClassloader() {
		return classloader;
	}

	public <E> Stream<E> getExtensions(Class<E> extension) {
		return (handle != null && extension.isAssignableFrom(handle.getClass())) ? Stream.of((E) handle)
				: Stream.empty();
	}

	/**
	 * Recompute the plugin hash and compare it to the saved hash.
	 * 
	 * @return Whether the current hash matches the saved hash
	 * @throws IOException
	 */
	public boolean checkHash() throws IOException {
		return Arrays.equals(computeHash(), getHash());
	}

	/**
	 * Load an installed plugin.
	 * 
	 * @throws IOException
	 */
	void load() throws IOException {
		checkState(!isLoaded());

		Path component = getComponent(Core.INSTANCE, Core.FLAVOR);

		// Build new classloader
		if (exists(component)) {
			classloader = new URLClassLoader(
					new URL[] { getComponent(null, null).toUri().toURL(), component.toUri().toURL() });
		} else {
			classloader = new URLClassLoader(new URL[] { getComponent(null, null).toUri().toURL() });
		}

		// Load plugin class if available
		handle = ServiceLoader.load(SandpolisPlugin.class, classloader).stream().filter(prov -> {
			// Restrict to services in the plugin component
			return prov.type().getName()
					.startsWith(String.format("%s.%s.%s", getPackageId(), Core.INSTANCE.toString().toLowerCase(),
							Core.FLAVOR.toString().toLowerCase()));
		}).map(ServiceLoader.Provider::get).findFirst().orElse(null);

		if (handle != null)
			handle.loaded();

		loaded().set(true);
	}

	void unload() {
		checkState(isLoaded());

		if (handle != null)
			handle.unloaded();

		loaded().set(false);
	}

	/**
	 * Search for all plugin components in the plugin directory.
	 *
	 * @return The component paths in a deterministic order
	 */
	public Stream<Path> components() {
		List<Path> components = new ArrayList<>();
		for (Instance instance : Instance.values()) {
			for (InstanceFlavor flavor : InstanceFlavor.values()) {
				Path component = getComponent(instance, flavor);
				if (exists(component))
					components.add(component);
			}
		}
		return components.stream();
	}

	/**
	 * Get a path to the filesystem artifact identified by the given instance type.
	 * 
	 * @param instance The instance type
	 * @param flavor   The instance subtype
	 * @return The artifact path
	 */
	public Path getComponent(Instance instance, InstanceFlavor flavor) {
		if (instance == null && flavor == null) {
			return Environment.PLUGIN.path().resolve(getPackageId()).resolve(getVersion()).resolve("core.jar");
		} else {
			Objects.requireNonNull(instance);
			Objects.requireNonNull(flavor);

			return Environment.PLUGIN.path().resolve(getPackageId()).resolve(getVersion())
					.resolve(instance.toString().toLowerCase()).resolve(flavor.toString().toLowerCase() + ".jar");
		}
	}

	/**
	 * Calculate a hash unique to this plugin and version.
	 * 
	 * <p>
	 * Internally, this hash is composed of the plugin's coordinates and installed
	 * filesystem artifacts.
	 *
	 * @return The plugin hash
	 * @throws IOException If the plugin's filesystem artifacts could not be read
	 */
	private byte[] computeHash() throws IOException {
		return ByteSource.concat(
				// Get coordinates
				ByteSource.wrap(coordinates().get().getBytes()),
				// Get component artifacts
				ByteSource.concat(components().map(MoreFiles::asByteSource).collect(Collectors.toList())))
				// Perform hash function
				.hash(Hashing.sha256()).asBytes();
	}

}
