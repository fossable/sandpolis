/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.core.instance.store.plugin;

import static com.sandpolis.core.instance.Environment.EnvPath.JLIB;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.MoreFiles;
import com.google.common.io.Resources;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.Store.ManualInitializer;
import com.sandpolis.core.instance.storage.StoreProvider;
import com.sandpolis.core.instance.storage.StoreProviderFactory;
import com.sandpolis.core.instance.storage.database.Database;
import com.sandpolis.core.proto.net.MCPlugin.PluginDescriptor;
import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.proto.util.Platform.InstanceFlavor;
import com.sandpolis.core.util.CertUtil;
import com.sandpolis.core.util.JarUtil;

/**
 * The {@link PluginStore} manages plugins.
 * 
 * @author cilki
 * @since 5.0.0
 */
@ManualInitializer
public final class PluginStore {

	private static final Logger log = LoggerFactory.getLogger(PluginStore.class);

	private static StoreProvider<Plugin> provider;

	/**
	 * The PF4J plugin manager.
	 */
	private static PluginManager manager;

	/**
	 * The default certificate verifier.
	 */
	private static Function<X509Certificate, Boolean> verifier = c -> true;

	public static void init(StoreProvider<Plugin> provider) {
		PluginStore.provider = Objects.requireNonNull(provider);
		manager = new DefaultPluginManager();
	}

	public static void load(Database main) {
		Objects.requireNonNull(main);

		init(StoreProviderFactory.database(Plugin.class, main));
	}

	/**
	 * Set the certificate verifier.
	 * 
	 * @param verifier A new certificate verifier
	 */
	public static void setCertVerifier(Function<X509Certificate, Boolean> verifier) {
		PluginStore.verifier = Objects.requireNonNull(verifier);
	}

	/**
	 * Get a list of all plugins in descriptor form.
	 * 
	 * @return The descriptor list
	 */
	public static List<PluginDescriptor> getPluginDescriptors() {
		try (Stream<Plugin> stream = provider.stream()) {
			return stream.map(plugin -> plugin.toDescriptor()).collect(Collectors.toList());
		}
	}

	/**
	 * Get a list of all plugins.
	 * 
	 * @return The plugin list
	 */
	public static List<Plugin> getPlugins() {
		try (Stream<Plugin> stream = provider.stream()) {
			return stream.collect(Collectors.toList());
		}
	}

	/**
	 * Get a plugin by ID.
	 * 
	 * @param id The plugin ID
	 * @return The plugin or {@code null} if not found
	 */
	public static Plugin getPlugin(String id) {
		try (Stream<Plugin> stream = provider.stream()) {
			return stream.filter(plugin -> plugin.getId().equals(id)).findAny().get();
		}
	}

	/**
	 * Get a component of a plugin archive.
	 * 
	 * @param plugin   The plugin
	 * @param instance The instance type of the component
	 * @param sub      The subtype of the component
	 * @return The component as a {@link ByteSource}
	 */
	public static ByteSource getPluginComponent(Plugin plugin, Instance instance, InstanceFlavor sub) {
		return Resources.asByteSource(manager.getPlugin(plugin.getName()).getPluginClassLoader()
				.getResource(String.format("/%s/%s.jar", instance.name().toLowerCase(), sub.name().toLowerCase())));
	}

	/**
	 * Get a plugin's filesystem artifact.
	 * 
	 * @param plugin The plugin
	 * @return The plugin artifact
	 */
	public static Path getArtifact(Plugin plugin) {
		return Environment.get(JLIB).resolve(plugin.getId() + ".jar");
	}

	/**
	 * Scan the plugin directory for uninstalled core plugins.
	 * 
	 * @throws IOException
	 */
	public static void scanPluginDirectory() throws IOException {
		Files.list(Environment.get(JLIB))
				// Core plugins only
				.filter(path -> Core.SO_BUILD.getPluginList().stream()
						.anyMatch(name -> path.getFileName().toString().startsWith(name)))
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
	public static void loadPlugins() {
		try (Stream<Plugin> stream = provider.stream()) {
			stream.filter(plugin -> plugin.isEnabled()).forEach(PluginStore::loadPlugin);
		}
	}

	/**
	 * Install a plugin.
	 * 
	 * @param path The plugin's filesystem artifact
	 */
	private static synchronized void installPlugin(Path path) {

		try {
			byte[] hash = hashPlugin(path);

			var manifest = JarUtil.getManifest(path.toFile());
			String id = manifest.getValue("Plugin-Id");
			String name = manifest.getValue("Plugin-Name");
			String version = manifest.getValue("Plugin-Version");
			String description = manifest.getValue("Description");

			// TODO validate info
			log.debug("Installing plugin: {}", path.toString());

			provider.add(new Plugin(id, name, version, description, true, hash));
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
	private static void loadPlugin(Plugin plugin) {
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
			var cert = CertUtil.parse(JarUtil.getManifestValue("Plugin-Cert", path));
			if (!verifier.apply(cert))
				throw new CertificateException("Certificate verification failed");
		} catch (CertificateException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}

		log.debug("Loading plugin: {}", plugin.getName());
		manager.loadPlugin(path);
	}

	/**
	 * Hash a plugin's filesystem artifact.
	 * 
	 * @param path The plugin artifact
	 * @return The file hash
	 * @throws IOException
	 */
	private static byte[] hashPlugin(Path path) throws IOException {
		return MoreFiles.asByteSource(path).hash(Hashing.sha256()).asBytes();
	}

	private PluginStore() {
	}
}
