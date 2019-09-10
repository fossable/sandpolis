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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sandpolis.core.instance.Environment.EnvPath.LIB;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.pf4j.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.MoreFiles;
import com.google.common.io.Resources;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.storage.MemoryMapStoreProvider;
import com.sandpolis.core.instance.storage.database.Database;
import com.sandpolis.core.instance.store.MapStore;
import com.sandpolis.core.instance.store.StoreBase.StoreConfig;
import com.sandpolis.core.instance.store.plugin.PluginStore.PluginStoreConfig;
import com.sandpolis.core.proto.net.MCPlugin.PluginDescriptor;
import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.proto.util.Platform.InstanceFlavor;
import com.sandpolis.core.util.CertUtil;
import com.sandpolis.core.util.JarUtil;

/**
 * The {@link PluginStore} manages plugins.<br>
 * <br>
 * Plugins can be in one of 4 states:
 * <ul>
 * <li>DOWNLOADED: The plugin exists on the filesystem, but the
 * {@link PluginStore} does not know about it. The plugin does not have a
 * {@link Plugin} instance.</li>
 * <li>INSTALLED: The {@link PluginStore} has a {@link Plugin} instance for the
 * plugin, but no extensions points have been loaded.</li>
 * <li>DISABLED: The plugin is INSTALLED, but cannot be loaded until it's
 * enabled.</li>
 * <li>LOADED: The plugin is INSTALLED and all extension points have been
 * loaded.</li>
 * </ul>
 *
 * @author cilki
 * @since 5.0.0
 */
public final class PluginStore extends MapStore<String, Plugin, PluginStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(PluginStore.class);

	public PluginStore() {
		super(log);
	}

	/**
	 * The PF4J plugin manager.
	 */
	private static PluginManager manager = new SandpolisPluginManager();

	/**
	 * The default certificate verifier which allows all plugins.
	 */
	private static Function<X509Certificate, Boolean> verifier = c -> true;

	/**
	 * Get the plugins in descriptor form.
	 *
	 * @return A new plugin descriptor stream
	 */
	public Stream<PluginDescriptor> getPluginDescriptors() {
		return provider.stream().map(plugin -> plugin.toDescriptor());
	}

	/**
	 * Get a plugin by id.
	 *
	 * @param id The plugin id
	 * @return The plugin
	 */
	public Optional<Plugin> getPlugin(String id) {
		checkNotNull(id);

		return provider.stream().filter(plugin -> plugin.getId().equals(id)).findAny();
	}

	/**
	 * Get a component of a plugin archive.
	 *
	 * @param plugin   The plugin
	 * @param instance The instance type of the component
	 * @param sub      The subtype of the component
	 * @return The component as a {@link ByteSource} or {@code null} if the
	 *         component does not exist
	 */
	public ByteSource getPluginComponent(Plugin plugin, Instance instance, InstanceFlavor sub) {
		URL url = getPluginComponentUrl(plugin, instance, sub);

		return url != null ? Resources.asByteSource(url) : null;
	}

	/**
	 * Find all components that the given plugin contains.
	 *
	 * @param plugin The plugin to search
	 * @return A list of components that were found in the plugin
	 */
	public List<InstanceFlavor> findComponentTypes(Plugin plugin) {
		checkNotNull(plugin);

		List<InstanceFlavor> types = new LinkedList<>();

		// TODO don't check invalid combinations
		for (Instance instance : Instance.values())
			for (InstanceFlavor sub : InstanceFlavor.values())
				if (getPluginComponentUrl(plugin, instance, sub) != null)
					types.add(sub);

		return types;
	}

	/**
	 * Get a {@link URL} representing a component of the given plugin.
	 *
	 * @param plugin   The target plugin
	 * @param instance The instance type
	 * @param sub      The instance subtype
	 * @return A url for the component or {@code null} if not found
	 */
	public URL getPluginComponentUrl(Plugin plugin, Instance instance, InstanceFlavor sub) {
		checkNotNull(plugin);
		checkNotNull(instance);
		checkNotNull(sub);

		try {
			return JarUtil.getResourceUrl(getArtifact(plugin),
					String.format("%s/%s.jar", instance.name().toLowerCase(), sub.name().toLowerCase()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get a plugin's filesystem artifact.
	 *
	 * @param plugin The plugin
	 * @return The plugin artifact
	 */
	public Path getArtifact(Plugin plugin) {
		checkNotNull(plugin);

		Path path;
		if (manager.getPlugin(plugin.getId()) != null)
			// First class approach
			path = manager.getPlugin(plugin.getId()).getPluginPath();
		else
			// Fallback approach
			path = Environment.get(LIB).resolve(plugin.getId() + ".jar");

		if (!Files.exists(path))
			log.warn("Missing filesystem artifact for plugin: {}", plugin.getId());

		return path;
	}

	/**
	 * Scan the plugin directory for uninstalled core plugins and install them.
	 *
	 * @throws IOException If a filesystem error occurs
	 */
	public void scanPluginDirectory() throws IOException {
		// TODO will install an arbitrary version if there's more than one
		Files.list(Environment.get(LIB))
				// Core plugins only
				.filter(path -> path.getFileName().toString().startsWith("sandpolis-plugin-"))
				// Skip installed plugins
				.filter(path -> {
					try (Stream<Plugin> stream = provider.stream()) {
						return stream.noneMatch(plugin -> path.getFileName().toString().startsWith(plugin.getName()));
					}
				}).forEach(PluginStore::installPlugin);

	}

	/**
	 * Load all enabled plugins.
	 */
	public void loadPlugins() {
		provider.stream().filter(plugin -> plugin.isEnabled()).forEach(PluginStore::loadPlugin);
	}

	/**
	 * Install a plugin.
	 *
	 * @param path The plugin's filesystem artifact
	 */
	public synchronized void installPlugin(Path path) {
		// TODO check state

		try {
			byte[] hash = hashPlugin(path);

			var manifest = JarUtil.getManifest(path.toFile());
			String id = manifest.getValue("Plugin-Id");
			String coordinate = manifest.getValue("Plugin-Coordinate");
			String name = manifest.getValue("Plugin-Name");
			String version = manifest.getValue("Plugin-Version");
			String description = manifest.getValue("Description");

			// TODO validate info
			log.debug("Installing plugin: {}", path.toString());

			Plugin plugin = new Plugin(id, coordinate, name, version, description, true, hash);
			provider.add(plugin);
			manager.loadPlugin(path);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Load a plugin. This method verifies the plugin artifact's hash.
	 *
	 * @param plugin The plugin to load
	 */
	private void loadPlugin(Plugin plugin) {
		// TODO check state

		// Locate plugin
		Path path = getArtifact(plugin);

		// Verify hash
		try {
			if (!Arrays.equals(hashPlugin(path), plugin.getHash()))
				throw new RuntimeException("The stored plugin hash does not match the artifact's hash");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// Verify certificate
		try {
			var cert = CertUtil.parseCert(JarUtil.getManifestValue(path, "Plugin-Cert").get());
			if (!verifier.apply(cert))
				throw new CertificateException("Certificate verification failed");
		} catch (CertificateException | IOException e) {
			log.error("Failed to load plugin", e);
			return;
		}

		log.debug("Loading plugin: {}", plugin.getName());
		manager.startPlugin(plugin.getId());
		// TODO load extensions
		// manager.getExtensions(SandpolisPlugin.class,
		// plugin.getId()).stream().forEach(SandpolisPlugin::load);
	}

	/**
	 * Hash a plugin's filesystem artifact.
	 *
	 * @param path The plugin artifact
	 * @return The file hash
	 * @throws IOException
	 */
	private byte[] hashPlugin(Path path) throws IOException {
		return MoreFiles.asByteSource(path).hash(Hashing.sha256()).asBytes();
	}

	@Override
	public PluginStore init(Consumer<PluginStoreConfig> configurator) {
		var config = new PluginStoreConfig();
		configurator.accept(config);

		return (PluginStore) super.init(null);
	}

	public final class PluginStoreConfig extends StoreConfig {

		public Function<X509Certificate, Boolean> verifier;

		@Override
		public void ephemeral() {
			provider = new MemoryMapStoreProvider<>(Plugin.class, Plugin::getId);
		}

		@Override
		public void persistent(Database database) {
			provider = database.getConnection().provider(Plugin.class, "id");
		}
	}

	public static final PluginStore PluginStore = new PluginStore();
}
