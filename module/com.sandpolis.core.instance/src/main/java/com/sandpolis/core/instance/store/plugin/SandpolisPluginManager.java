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

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.pf4j.AbstractPluginManager;
import org.pf4j.DefaultExtensionFactory;
import org.pf4j.DefaultExtensionFinder;
import org.pf4j.DefaultPluginFactory;
import org.pf4j.DefaultPluginStatusProvider;
import org.pf4j.DefaultVersionManager;
import org.pf4j.ExtensionFactory;
import org.pf4j.ExtensionFinder;
import org.pf4j.JarPluginRepository;
import org.pf4j.ManifestPluginDescriptorFinder;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginDescriptorFinder;
import org.pf4j.PluginFactory;
import org.pf4j.PluginLoader;
import org.pf4j.PluginRepository;
import org.pf4j.PluginStatusProvider;
import org.pf4j.VersionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cilki.compact.CompactClassLoader;
import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.proto.util.Platform.InstanceFlavor;

public final class SandpolisPluginManager extends AbstractPluginManager {

	private static final Logger log = LoggerFactory.getLogger(SandpolisPluginManager.class);

	private final String componentPath;

	public SandpolisPluginManager(Instance instance, InstanceFlavor flavor) {
		componentPath = String.format("%s/%s.jar", instance.toString().toLowerCase(), flavor.toString().toLowerCase());
	}

	@Override
	protected PluginRepository createPluginRepository() {
		return new JarPluginRepository(getPluginsRoot());
	}

	@Override
	protected PluginFactory createPluginFactory() {
		return new DefaultPluginFactory();
	}

	@Override
	protected ExtensionFactory createExtensionFactory() {
		return new DefaultExtensionFactory();
	}

	@Override
	protected PluginDescriptorFinder createPluginDescriptorFinder() {
		return new ManifestPluginDescriptorFinder() {
			@Override
			protected Manifest readManifest(Path pluginPath) {
				try (var in = new ZipInputStream(Files.newInputStream(pluginPath))) {
					ZipEntry entry;
					while ((entry = in.getNextEntry()) != null) {
						if (entry.getName().equals(componentPath)) {
							try (var component = new ZipInputStream(in)) {
								while ((entry = component.getNextEntry()) != null) {
									if (entry.getName().equals("META-INF/MANIFEST.MF")) {
										return new Manifest(component);
									}
								}
							}
							log.warn("Missing component manifest");
							return super.readManifest(pluginPath);
						}
					}
				} catch (IOException e) {
					log.warn("Failed to read plugin archive", e);
				}
				return super.readManifest(pluginPath);
			}
		};
	}

	@Override
	protected ExtensionFinder createExtensionFinder() {
		DefaultExtensionFinder extensionFinder = new DefaultExtensionFinder(this);
		addPluginStateListener(extensionFinder);

		return extensionFinder;
	}

	@Override
	protected PluginStatusProvider createPluginStatusProvider() {
		return new DefaultPluginStatusProvider(getPluginsRoot());
	}

	@Override
	protected VersionManager createVersionManager() {
		return new DefaultVersionManager();
	}

	@Override
	protected PluginLoader createPluginLoader() {
		return new PluginLoader() {
			@Override
			public boolean isApplicable(Path pluginPath) {
				return Files.exists(pluginPath);
				// TODO check entries?
			}

			@Override
			public ClassLoader loadPlugin(Path pluginPath, PluginDescriptor pluginDescriptor) {
				log.trace("Building plugin classloader for plugin archive: {}", pluginPath);

				CompactClassLoader ccl = new CompactClassLoader(Thread.currentThread().getContextClassLoader());
				try {
					// Add common classes
					ccl.add(pluginPath.toUri().toURL(), false);

					// Add instance specific classes if it exists
					if (ccl.getResource(componentPath) != null) {
						ccl.add(new URL(String.format("file:%s!/%s", pluginPath, componentPath)), false);

						// TODO component specific dependencies from matrix.bin
					}
				} catch (IOException e) {
					log.error("Failed to load plugin", e);
				}

				return ccl;
			}
		};
	}
}
