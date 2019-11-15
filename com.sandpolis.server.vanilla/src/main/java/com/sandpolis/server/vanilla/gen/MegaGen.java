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
package com.sandpolis.server.vanilla.gen;

import com.github.cilki.zipset.ZipSet;
import com.github.cilki.zipset.ZipSet.EntryPath;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.store.plugin.Plugin;
import com.sandpolis.core.proto.util.Generator.FeatureSet;
import com.sandpolis.core.proto.util.Generator.GenConfig;
import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.core.proto.util.Platform.InstanceFlavor;
import com.sandpolis.core.proto.util.Platform.OsType;
import com.sandpolis.core.soi.SoiUtil;
import com.sandpolis.core.util.ArtifactUtil;
import com.sandpolis.server.vanilla.gen.mega.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.sandpolis.core.instance.store.plugin.PluginStore.PluginStore;
import static com.sandpolis.core.util.ArtifactUtil.ParsedCoordinate.fromCoordinate;

/**
 * This generator builds a {@code com.sandpolis.client.mega} client.
 *
 * @author cilki
 * @since 2.0.0
 */
public abstract class MegaGen extends Generator {

	private static final Logger log = LoggerFactory.getLogger(MegaGen.class);

	private String artifact;

	protected MegaGen(GenConfig config, String archiveExtension, String artifact) {
		super(config, archiveExtension);
		this.artifact = Objects.requireNonNull(artifact);
	}

	protected MegaGen(GenConfig config, String archiveExtension) {
		super(config, archiveExtension);
	}

	public static MegaGen build(GenConfig config) {
		switch (config.getFormat()) {
		case BAT:
			return new BatPackager(config);
		case ELF:
			return new ElfPackager(config);
		case EXE:
			return new ExePackager(config);
		case JAR:
			return new JarPackager(config);
		case PY:
			return new PyPackager(config);
		case QR:
			return new QrPackager(config);
		case RB:
			return new RbPackager(config);
		case SH:
			return new ShPackager(config);
		case URL:
			return new UrlPackager(config);
		default:
			throw new IllegalArgumentException();
		}
	}

	protected Properties buildInstallerConfig() throws IOException {
		Properties cfg = new Properties();

		// Set module dependencies
		cfg.put("modules", getDependencies().stream().collect(Collectors.joining(" ")));

		// Set installation paths
		for (var entry : config.getMega().getExecution().getInstallPathMap().entrySet()) {
			switch (entry.getKey()) {
			case OsType.AIX_VALUE:
				cfg.put("path.aix", entry.getValue());
				break;
			case OsType.FREEBSD_VALUE:
				cfg.put("path.freebsd", entry.getValue());
				break;
			case OsType.LINUX_VALUE:
				cfg.put("path.linux", entry.getValue());
				break;
			case OsType.MACOS_VALUE:
				cfg.put("path.mac", entry.getValue());
				break;
			case OsType.SOLARIS_VALUE:
				cfg.put("path.solaris", entry.getValue());
				break;
			case OsType.WINDOWS_VALUE:
				cfg.put("path.windows", entry.getValue());
				break;
			}
		}

		return cfg;
	}

	protected String readArtifactString() throws IOException {
		try (var in = MegaGen.class.getResourceAsStream(artifact)) {
			if (in == null)
				throw new IOException("Missing resource: " + artifact);

			return CharStreams.toString(new InputStreamReader(in, Charsets.UTF_8));
		}
	}

	protected byte[] readArtifactBinary() throws IOException {
		try (var in = MegaGen.class.getResourceAsStream(artifact)) {
			if (in == null)
				throw new IOException("Missing resource: " + artifact);

			return in.readAllBytes();
		}
	}

	protected List<String> getDependencies() throws IOException {
		Path client = Environment.LIB.path().resolve("sandpolis-client-mega-" + Core.SO_BUILD.getVersion() + ".jar");

		return SoiUtil.getMatrix(client).getAllDependenciesInclude()
				.map(artifact -> artifact.getArtifact().getCoordinates()).collect(Collectors.toList());
	}

	protected Object test() throws Exception {
		log.debug("Computing MEGA payload");

		Path client = Environment.LIB.path().resolve("sandpolis-client-mega-" + Core.SO_BUILD.getVersion() + ".jar");

		ZipSet output = new ZipSet(client);
		FeatureSet features = config.getMega().getFeatures();

		// Add client configuration
		output.add("soi/client.bin", config.getMega().toByteArray());

		// Add client dependencies
		SoiUtil.getMatrix(client).getAllDependencies().forEach(artifact -> {
			Path source = ArtifactUtil.getArtifactFile(Environment.LIB.path(), artifact.getArtifact().getCoordinates());

			// Add library
			output.add("lib/" + source.getFileName(), source);

			// Strip native dependencies if possible
			artifact.getArtifact().getNativeComponentList().stream()
					// Filter out unnecessary platform-specific libraries
					.filter(component -> !features.getSupportedOsList()
							.contains(OsType.valueOf(component.getPlatform())))
					.filter(component -> !features.getSupportedArchList().contains(component.getArchitecture()))
					.forEach(component -> {
						output.sub(EntryPath.get("lib/" + source.getFileName(), component.getPath()));
					});

		});

		// Add plugin binaries
		if (!config.getMega().getDownloader()) {
			for (var plugin : PluginStore.stream().filter(plugin -> features.getPluginList().contains(plugin.getId()))
					.toArray(Plugin[]::new)) {
				ZipSet pluginArchive = new ZipSet();

				// Add core component
				Path core = plugin.getComponent(null, null);
				pluginArchive.add("core.jar", core);
				SoiUtil.getMatrix(core).getAllDependencies().forEach(dep -> {
					output.add("lib/" + fromCoordinate(dep.getCoordinates()).filename,
							ArtifactUtil.getArtifactFile(Environment.LIB.path(), dep.getCoordinates()));
				});

				// Add mega component
				Path mega = plugin.getComponent(Instance.CLIENT, InstanceFlavor.MEGA);
				pluginArchive.add("client/mega.jar", mega);
				SoiUtil.getMatrix(mega).getAllDependencies().forEach(dep -> {
					output.add("lib/" + fromCoordinate(dep.getCoordinates()).filename,
							ArtifactUtil.getArtifactFile(Environment.LIB.path(), dep.getCoordinates()));
				});

				output.add(EntryPath.get("lib/" + plugin.getId() + ".jar"), pluginArchive);
			}
		}

		return output.build();
	}

}
