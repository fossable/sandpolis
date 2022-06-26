//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.instance.plugin;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import org.s7s.core.foundation.S7SCertificate;
import org.s7s.core.foundation.S7SJarFile;
import org.s7s.core.instance.InstanceContext;
import org.s7s.core.foundation.Instance.InstanceFlavor;
import org.s7s.core.foundation.Instance.InstanceType;
import org.s7s.core.instance.plugin.PluginStore.PluginStoreConfig;
import org.s7s.core.instance.state.InstanceOids.ProfileOid.PluginOid;
import org.s7s.core.instance.state.st.STDocument;
import org.s7s.core.instance.store.ConfigurableStore;
import org.s7s.core.instance.store.STCollectionStore;

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
 * @since 5.0.0
 */
public final class PluginStore extends STCollectionStore<Plugin> implements ConfigurableStore<PluginStoreConfig> {

	public static final record PluginLoadedEvent(Plugin plugin) {
	}

	public static final class PluginStoreConfig {

		public STDocument collection;

		public Function<X509Certificate, Boolean> verifier;

		private PluginStoreConfig(Consumer<PluginStoreConfig> configurator) {
			configurator.accept(this);
		}
	}

	public static final record PluginUnloadedEvent(Plugin plugin) {
	}

	private static final Logger log = LoggerFactory.getLogger(PluginStore.class);

	public static final PluginStore PluginStore = new PluginStore();

	/**
	 * The default certificate verifier which allows all plugins.
	 */
	private static Function<X509Certificate, Boolean> verifier = c -> true;

	private PluginStore() {
		super(log, Plugin::new);
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
		for (InstanceType instance : InstanceType.values())
			for (InstanceFlavor sub : InstanceFlavor.values())
				if (getPluginComponentUrl(plugin, instance, sub) != null)
					types.add(sub);

		return types;
	}

	public Optional<Plugin> getByPackageId(String packageId) {
		return values().stream().filter(plugin -> plugin.get(PluginOid.PACKAGE_ID).asString().equals(packageId))
				.findAny();
	}

	public Stream<Plugin> getLoadedPlugins() {
		return values().stream().filter(plugin -> plugin.get(PluginOid.LOADED).asBoolean(false)
				&& plugin.get(PluginOid.ENABLED).asBoolean(true));
	}

	public <E> Stream<E> getHandles(Class<E> type) {
		return getLoadedPlugins().map(plugin -> plugin.getHandle(type).orElse(null)).filter(Objects::nonNull);
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
	public ByteSource getPluginComponent(Plugin plugin, InstanceType instance, InstanceFlavor sub) {
		URL url = getPluginComponentUrl(plugin, instance, sub);

		return url != null ? Resources.asByteSource(url) : null;
	}

	/**
	 * Get a {@link URL} representing a component of the given plugin.
	 *
	 * @param plugin   The target plugin
	 * @param instance The instance type
	 * @param sub      The instance subtype
	 * @return A url for the component or {@code null} if not found
	 */
	public URL getPluginComponentUrl(Plugin plugin, InstanceType instance, InstanceFlavor sub) {
		checkNotNull(plugin);
		checkNotNull(instance);
		checkNotNull(sub);

		return null;
	}

	@Override
	public void init(Consumer<PluginStoreConfig> configurator) {
		var config = new PluginStoreConfig(configurator);

		setDocument(config.collection);
	}

	/**
	 * Install a plugin.
	 *
	 * @param path The plugin's filesystem artifact
	 */
	public synchronized void installPlugin(Path path) {
		log.debug("Installing plugin: {}", path.toString());

		try {
			Plugin plugin = create(p -> {
			});
			plugin.install(path, true);
		} catch (IOException e) {
			log.error("Failed to install plugin", e);
		}
	}

	/**
	 * Load a plugin. This method verifies the plugin artifact's hash.
	 *
	 * @param plugin The plugin to load
	 */
	private void loadPlugin(Plugin plugin) {
		checkState(!plugin.get(PluginOid.LOADED).asBoolean(false));

		log.debug("Loading plugin: {} ({})", plugin.get(PluginOid.NAME).asString(),
				plugin.get(PluginOid.PACKAGE_ID).asString());

		// Verify hash
		try {
			if (!plugin.checkHash()) {
				log.error("The stored plugin hash does not match the artifact's hash");
				return;
			}
		} catch (IOException e) {
			log.error("Failed to hash plugin", e);
			return;
		}

		// Verify certificate
		try {
			if (!verifier.apply(S7SCertificate.of(plugin.get(PluginOid.CERTIFICATE).asString()).certificate())) {
				log.error("Failed to verify plugin certificate");
				return;
			}
		} catch (CertificateException e) {
			log.error("Failed to load plugin certificate", e);
			return;
		}

		try {
			plugin.load();
		} catch (IOException e) {
			log.error("Failed to load plugin", e);
			return;
		}
		post(new PluginLoadedEvent(plugin));
	}

	/**
	 * Load all installed plugins that are enabled and currently unloaded.
	 */
	public void loadPlugins() {
		values().stream()

				// Enabled plugins only
				.filter(plugin -> plugin.get(PluginOid.ENABLED).asBoolean())

				// Skip loaded plugins
				.filter(plugin -> !plugin.get(PluginOid.LOADED).asBoolean(false))

				// Load each plugin
				.forEach(PluginStore::loadPlugin);
	}

	/**
	 * Scan the plugin directory for uninstalled core plugins and install them.
	 *
	 * @throws IOException If a filesystem error occurs
	 */
	public void scanPluginDirectory() throws IOException {
		// TODO will install an arbitrary version if there's more than one
		Files.list(InstanceContext.PATH_PLUGIN.get())

				// Skip non-plugins
				.filter(path -> path.getFileName().toString().endsWith(".jar"))

				// Skip installed plugins
				.filter(path -> {
					try (Stream<Plugin> stream = values().stream()) {
						// Read plugin id
						String id = S7SJarFile.of(path).getManifestValue("Plugin-Id").orElse(null);

						return stream.noneMatch(plugin -> plugin.get(PluginOid.PACKAGE_ID).asString().equals(id));
					} catch (IOException e) {
						return false;
					}
				}).forEach(PluginStore::installPlugin);

	}
}
