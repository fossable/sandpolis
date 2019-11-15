/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.core.instance.store.plugin;

import static com.google.common.base.Preconditions.checkState;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.list;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
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
import com.sandpolis.core.instance.plugin.SandpolisPlugin;
import com.sandpolis.core.instance.util.InstanceUtil;
import com.sandpolis.core.proto.net.MsgPlugin.PluginDescriptor;
import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.proto.util.Platform.InstanceFlavor;
import com.sandpolis.core.util.JarUtil;

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
		Resources.asByteSource(JarUtil.getResourceUrl(path, "core.jar"))
				.copyTo(Files.asByteSink(Environment.PLUGIN.path().resolve(getArtifactName(null, null)).toFile()));

		// Install components
		InstanceUtil.iterate((instance, flavor) -> {
			try {
				String component = String.format("%s/%s.jar", instance.toString().toLowerCase(),
						flavor.toString().toLowerCase());
				if (JarUtil.resourceExists(path, component)) {

					Resources.asByteSource(JarUtil.getResourceUrl(path, component)).copyTo(Files
							.asByteSink(Environment.PLUGIN.path().resolve(getArtifactName(instance, flavor)).toFile()));
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

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
		return coordinate.split(":")[2];
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

		Path component = Environment.PLUGIN.path().resolve(getArtifactName(Core.INSTANCE, Core.FLAVOR));

		// Build new classloader
		if (exists(component)) {
			classloader = new URLClassLoader(
					new URL[] { Environment.PLUGIN.path().resolve(getArtifactName(null, null)).toUri().toURL(),
							component.toUri().toURL() });
		} else {
			classloader = new URLClassLoader(
					new URL[] { Environment.PLUGIN.path().resolve(getArtifactName(null, null)).toUri().toURL() });
		}

		// Load plugin class if available
		handle = ServiceLoader.load(SandpolisPlugin.class, classloader).stream().filter(prov -> {
			// Restrict to services in the plugin component
			return prov.type().getName()
					.startsWith(String.format("%s.%s.%s", id, Core.INSTANCE.toString().toLowerCase(),
							Core.FLAVOR.toString().toLowerCase()));
		}).map(prov -> prov.get()).findFirst().orElse(null);

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
	 * @throws IOException
	 */
	Stream<Path> findComponents() throws IOException {
		return list(Environment.PLUGIN.path()).filter(path -> path.getFileName().toString().startsWith(id));
	}

	public Path getComponent(Instance instance, InstanceFlavor flavor) {
		return Environment.PLUGIN.path().resolve(getArtifactName(instance, flavor));
	}

	private String getArtifactName(Instance instance, InstanceFlavor flavor) {
		if (instance == null && flavor == null) {
			return String.format("%s-%s.jar", id, getVersion());
		} else {
			return String.format("%s:%s:%s-%s.jar", id, instance.toString().toLowerCase(),
					flavor.toString().toLowerCase(), getVersion());
		}
	}

	/**
	 * Hash the plugin components.
	 *
	 * @return
	 * @throws IOException
	 */
	private byte[] hash() throws IOException {
		return ByteSource
				.concat(findComponents().map(path -> MoreFiles.asByteSource(path)).collect(Collectors.toList()))
				.hash(Hashing.sha256()).asBytes();
	}

}
