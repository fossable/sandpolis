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
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.common.io.MoreFiles;
import com.google.common.io.Resources;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.Plugin.PluginDescriptor;
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
@Entity
public final class Plugin {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int db_id;

	/**
	 * The plugin's package identifier.
	 */
	@Column(nullable = false, unique = true)
	private String id;

	/**
	 * The plugin's Maven central coordinates in G:A:V format.
	 */
	@Column(nullable = false, unique = true)
	private String coordinate;

	/**
	 * The plugin's user-friendly name.
	 */
	@Column(nullable = false)
	private String name;

	/**
	 * The plugin's user-friendly description.
	 */
	@Column(nullable = true)
	private String description;

	/**
	 * Whether the plugin can be loaded.
	 */
	@Column(nullable = false)
	private boolean enabled;

	/**
	 * The plugin artifact's hash.
	 */
	@Column(nullable = false)
	private byte[] hash;

	/**
	 * The plugin's certificate.
	 */
	@Column(nullable = false, length = 2048)
	private String certificate;

	/**
	 * A classloader for the plugin.
	 */
	@Transient
	private ClassLoader classloader;

	/**
	 * The plugin handle if available.
	 */
	@Transient
	private SandpolisPlugin handle;

	/**
	 * Whether the plugin is currently loaded.
	 */
	@Transient
	private boolean loaded;

	public Plugin(Path path, boolean enabled) throws IOException {

		var manifest = JarUtil.getManifest(path.toFile());
		id = manifest.getValue("Plugin-Id");
		coordinate = manifest.getValue("Plugin-Coordinate");
		name = manifest.getValue("Plugin-Name");
		description = manifest.getValue("Description");
		certificate = manifest.getValue("Plugin-Cert");

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

		this.enabled = enabled;
		this.hash = hash();
	}

	Plugin() {
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public byte[] getHash() {
		return hash;
	}

	public String getCertificate() {
		return certificate;
	}

	public boolean isLoaded() {
		return loaded;
	}

	public String getVersion() {
		String[] gav = coordinate.split(":");
		if (gav.length == 3)
			return gav[2];
		return null;
	}

	public String getCoordinate() {
		return coordinate;
	}

	public ClassLoader getClassloader() {
		return classloader;
	}

	/**
	 * Build a {@link PluginDescriptor} from the object.
	 *
	 * @return A new {@link PluginDescriptor}
	 */
	public PluginDescriptor toDescriptor() {
		var plugin = PluginDescriptor.newBuilder().setId(getId()).setCoordinate(coordinate).setName(getName())
				.setEnabled(isEnabled());

		if (description != null)
			plugin.setDescription(getDescription());
		return plugin.build();
	}

	public <E> Stream<E> getExtensions(Class<E> extension) {
		if (handle != null && extension.isAssignableFrom(handle.getClass()))
			return Stream.of((E) handle);
		else
			return Stream.empty();
	}

	public boolean checkHash() throws IOException {
		return Arrays.equals(hash(), hash);
	}

	void load() throws IOException {
		checkState(!loaded);

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
					.startsWith(String.format("%s.%s.%s", id, Core.INSTANCE.toString().toLowerCase(),
							Core.FLAVOR.toString().toLowerCase()));
		}).map(ServiceLoader.Provider::get).findFirst().orElse(null);

		if (handle != null)
			handle.loaded();
		loaded = true;
	}

	void unload() {
		checkState(loaded);

		if (handle != null)
			handle.unloaded();

		loaded = false;
	}

	/**
	 * Search for all plugin components in the {@code PLUGIN} directory.
	 *
	 * @return The component paths
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

	public Path getComponent(Instance instance, InstanceFlavor flavor) {
		if (instance == null && flavor == null) {
			return Environment.PLUGIN.path().resolve(getId()).resolve(getVersion()).resolve("core.jar");
		} else {
			return Environment.PLUGIN.path().resolve(getId()).resolve(getVersion())
					.resolve(instance.toString().toLowerCase()).resolve(flavor.toString().toLowerCase() + ".jar");
		}
	}

	/**
	 * Produce a hash unique to this plugin and version.
	 *
	 * @return The plugin hash
	 * @throws IOException If the plugin's filesystem artifacts could not be read
	 */
	private byte[] hash() throws IOException {
		return ByteSource.concat(components().map(MoreFiles::asByteSource).collect(Collectors.toList()))
				.hash(Hashing.sha256()).asBytes();
	}

}
