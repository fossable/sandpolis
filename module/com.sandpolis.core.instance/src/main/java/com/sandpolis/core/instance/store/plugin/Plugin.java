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

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

import com.github.cilki.compact.CompactClassLoader;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.plugin.SandpolisPlugin;
import com.sandpolis.core.proto.net.MsgPlugin.PluginDescriptor;
import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.proto.util.Platform.InstanceFlavor;
import com.sandpolis.core.util.JarUtil;

/**
 * Represents a Sandpolis plugin.
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
	 * The plugin's filesystem path.
	 */
	@Column(nullable = false)
	private String path;

	/**
	 * A classloader for the plugin.
	 */
	@Transient
	private CompactClassLoader classloader;

	/**
	 * The plugin handle if available.
	 */
	@Transient
	private SandpolisPlugin handle;

	@Transient
	private boolean loaded;

	public Plugin(Path path, byte[] hash, boolean enabled) throws IOException {
		this.enabled = enabled;
		this.hash = hash;
		this.path = path.toAbsolutePath().toString();

		var manifest = JarUtil.getManifest(path.toFile());
		id = manifest.getValue("Plugin-Id");
		coordinate = manifest.getValue("Plugin-Coordinate");
		name = manifest.getValue("Plugin-Name");
		description = manifest.getValue("Description");
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

	public Path getPath() {
		return Paths.get(path);
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

	public CompactClassLoader getClassloader() {
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

	void load() throws IOException {
		checkState(!loaded);

		// Build new classloader
		classloader = new CompactClassLoader(Thread.currentThread().getContextClassLoader());

		// Add common classes
		classloader.add(getPath().toUri().toURL(), false);

		// Add instance specific classes if it exists
		String componentPath = String.format("%s/%s.jar", Core.INSTANCE.toString().toLowerCase(),
				Core.FLAVOR.toString().toLowerCase());
		if (classloader.getResource(componentPath) != null) {
			classloader.add(new URL(String.format("file:%s!/%s", getPath(), componentPath)), false);

			// TODO component specific dependencies from matrix.bin
		}

		// Load plugin class if available
		try {
			handle = (SandpolisPlugin) classloader.loadClass(getClassName(Core.INSTANCE, Core.FLAVOR)).getConstructor()
					.newInstance();
			handle.loaded();
		} catch (ClassNotFoundException e) {
			// Do nothing
		} catch (Exception e) {
			throw new IOException(e);
		}

		// Add to application classloader
		CompactClassLoader system = (CompactClassLoader) ClassLoader.getSystemClassLoader();
		system.add(classloader);

		loaded = true;
	}

	void unload() {
		checkState(loaded);

		if (handle != null)
			handle.unloaded();

		loaded = false;
	}

	public <E> Stream<E> getExtensions(Class<E> extension) {
		if (extension.isAssignableFrom(handle.getClass()))
			return Stream.of((E) handle);
		else
			return Stream.empty();
	}

	private String getClassName(Instance instance, InstanceFlavor flavor) {
		String lastId = getId().substring(getId().lastIndexOf('.') + 1);
		return String.format("%s.%s.%s.%sPlugin", getId(), instance.toString().toLowerCase(),
				flavor.toString().toLowerCase(),
				lastId.substring(0, 1).toUpperCase() + lastId.substring(1).toLowerCase());
	}

}
