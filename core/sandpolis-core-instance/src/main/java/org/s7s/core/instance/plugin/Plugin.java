//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.plugin;

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
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.common.io.MoreFiles;
import com.google.common.io.Resources;
import org.s7s.core.foundation.S7SJarFile;
import org.s7s.core.instance.Entrypoint;
import org.s7s.core.instance.InstanceContext;
import org.s7s.core.foundation.Instance.InstanceFlavor;
import org.s7s.core.foundation.Instance.InstanceType;
import org.s7s.core.instance.state.InstanceOids.ProfileOid.PluginOid;
import org.s7s.core.instance.state.st.STDocument;
import org.s7s.core.instance.state.vst.AbstractSTDomainObject;

/**
 * Represents a Sandpolis plugin installed in the {@code PLUGIN} directory.
 *
 * @since 5.0.0
 */
public class Plugin extends AbstractSTDomainObject {

	private ClassLoader classloader;

	private SandpolisPlugin handle;

	Plugin(STDocument document) {
		super(document);
	}

	public void install(Path path, boolean enabled) throws IOException {
		if (get(PluginOid.HASH).isPresent())
			throw new IllegalStateException();

		var jar = S7SJarFile.of(path);

		var manifest = jar.getManifest();
		set(PluginOid.PACKAGE_ID, manifest.getValue("Plugin-Id"));
		set(PluginOid.COORDINATES, manifest.getValue("Plugin-Coordinate"));
		set(PluginOid.NAME, manifest.getValue("Plugin-Name"));
		set(PluginOid.DESCRIPTION, manifest.getValue("Description"));
		set(PluginOid.CERTIFICATE, manifest.getValue("Plugin-Cert"));

		// Install core
		Path core = getComponent(null, null);
		if (!exists(core))
			createDirectories(core.getParent());
		Resources.asByteSource(jar.getResourceUrl("core.jar")).copyTo(Files.asByteSink(core.toFile()));

		// Install components
		for (InstanceType instance : InstanceType.values()) {
			for (InstanceFlavor flavor : InstanceFlavor.values()) {
				String internalPath = String.format("%s/%s.jar", instance.toString().toLowerCase(),
						flavor.toString().toLowerCase());
				if (jar.resourceExists(internalPath)) {
					Path component = getComponent(instance, flavor);
					if (!exists(component))
						createDirectories(component.getParent());

					Resources.asByteSource(jar.getResourceUrl(internalPath))
							.copyTo(Files.asByteSink(component.toFile()));
				}
			}
		}

		set(PluginOid.ENABLED, enabled);
		set(PluginOid.HASH, computeHash());
	}

	public String getVersion() {
		String[] gav = get(PluginOid.COORDINATES).asString().split(":");
		if (gav.length == 3)
			return gav[2];
		return null;
	}

	public ClassLoader getClassloader() {
		return classloader;
	}

	public <E> Optional<E> getHandle(Class<E> type) {
		return (handle != null && type.isAssignableFrom(handle.getClass())) ? Optional.of((E) handle)
				: Optional.empty();
	}

	/**
	 * Recompute the plugin hash and compare it to the saved hash.
	 *
	 * @return Whether the current hash matches the saved hash
	 * @throws IOException
	 */
	public boolean checkHash() throws IOException {
		return Arrays.equals(computeHash(), get(PluginOid.HASH).asBytes());
	}

	/**
	 * Load an installed plugin.
	 *
	 * @throws IOException
	 */
	void load() throws IOException {
		checkState(!get(PluginOid.LOADED).asBoolean(false));

		Path component = getComponent(Entrypoint.data().instance(), Entrypoint.data().flavor());

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
					.startsWith(String.format("%s.%s.%s", get(PluginOid.PACKAGE_ID),
							Entrypoint.data().instance().toString().toLowerCase(),
							Entrypoint.data().flavor().toString().toLowerCase()));
		}).map(ServiceLoader.Provider::get).findFirst().orElse(null);

		if (handle != null)
			handle.loaded();

		set(PluginOid.LOADED, true);
	}

	void unload() {
		checkState(!get(PluginOid.LOADED).asBoolean(false));

		if (handle != null)
			handle.unloaded();

		set(PluginOid.LOADED, false);
	}

	/**
	 * Search for all plugin components in the plugin directory.
	 *
	 * @return The component paths in a deterministic order
	 */
	public Stream<Path> components() {
		List<Path> components = new ArrayList<>();
		for (InstanceType instance : InstanceType.values()) {
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
	public Path getComponent(InstanceType instance, InstanceFlavor flavor) {
		if (instance == null && flavor == null) {
			return InstanceContext.PATH_PLUGIN.get().resolve(get(PluginOid.PACKAGE_ID).asString()).resolve(getVersion())
					.resolve("core.jar");
		} else {
			Objects.requireNonNull(instance);
			Objects.requireNonNull(flavor);

			return InstanceContext.PATH_PLUGIN.get().resolve(get(PluginOid.PACKAGE_ID).asString()).resolve(getVersion())
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
				ByteSource.wrap(get(PluginOid.COORDINATES).asBytes()),
				// Get component artifacts
				ByteSource.concat(components().map(MoreFiles::asByteSource).collect(Collectors.toList())))
				// Perform hash function
				.hash(Hashing.sha256()).asBytes();
	}

}
