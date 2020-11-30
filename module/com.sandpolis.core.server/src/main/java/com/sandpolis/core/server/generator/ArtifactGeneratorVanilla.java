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
package com.sandpolis.core.server.generator;

import static com.sandpolis.core.instance.plugin.PluginStore.PluginStore;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cilki.zipset.ZipSet;
import com.github.cilki.zipset.ZipSet.EntryPath;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.sandpolis.core.foundation.Platform.OsType;
import com.sandpolis.core.foundation.soi.SoiUtil;
import com.sandpolis.core.foundation.util.ArtifactUtil;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.Group.AgentConfig;
import com.sandpolis.core.instance.Metatypes.InstanceFlavor;
import com.sandpolis.core.instance.Metatypes.InstanceType;
import com.sandpolis.core.instance.plugin.Plugin;

/**
 * This generator builds a {@code com.sandpolis.agent.vanilla} agent.
 *
 * @since 2.0.0
 */
public class ArtifactGeneratorVanilla extends Generator {

	private static final Logger log = LoggerFactory.getLogger(ArtifactGeneratorVanilla.class);

	private String artifact;

	public ArtifactGeneratorVanilla(AgentConfig config, Packager packager) {
		super(config, packager);
	}

	protected Properties buildInstallerConfig() throws IOException {
		Properties cfg = new Properties();

		// Set module dependencies
		cfg.put("modules", getDependencies().stream().collect(Collectors.joining(" ")));

		// Set installation paths
		for (var entry : config.getInstallPathMap().entrySet()) {
			switch (entry.getKey()) {
			case OsType.AIX_VALUE:
				cfg.put("path.aix", entry.getValue());
				break;
			case OsType.BSD_VALUE:
				cfg.put("path.bsd", entry.getValue());
				break;
			case OsType.LINUX_VALUE:
				cfg.put("path.linux", entry.getValue());
				break;
			case OsType.DARWIN_VALUE:
				cfg.put("path.darwin", entry.getValue());
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
		try (var in = ArtifactGeneratorVanilla.class.getResourceAsStream(artifact)) {
			if (in == null)
				throw new IOException("Missing resource: " + artifact);

			return CharStreams.toString(new InputStreamReader(in, Charsets.UTF_8));
		}
	}

	protected byte[] readArtifactBinary() throws IOException {
		try (var in = ArtifactGeneratorVanilla.class.getResourceAsStream(artifact)) {
			if (in == null)
				throw new IOException("Missing resource: " + artifact);

			return in.readAllBytes();
		}
	}

	protected List<String> getDependencies() throws IOException {
		Path agent = Environment.LIB.path().resolve("sandpolis-agent-mega-" + Core.SO_BUILD.getVersion() + ".jar");

		return SoiUtil.getMatrix(agent).getAllDependenciesInclude()
				.map(artifact -> artifact.getArtifact().getCoordinates()).collect(Collectors.toList());
	}

	@Override
	protected byte[] generate() throws Exception {
		log.debug("Computing artifact");

		Path agent = Environment.LIB.path().resolve("sandpolis-agent-mega-" + Core.SO_BUILD.getVersion() + ".jar");

		ZipSet output = new ZipSet(agent);

		// Add agent configuration
		output.add("soi/agent.bin", config.toByteArray());

		// Add agent dependencies
		SoiUtil.getMatrix(agent).getAllDependencies().forEach(artifact -> {
			Path source = ArtifactUtil.getArtifactFile(Environment.LIB.path(), artifact.getArtifact().getCoordinates());

			// Add library
			output.add("lib/" + source.getFileName(), source);

			// Strip native dependencies if possible
			artifact.getArtifact().getNativeComponentList().stream()
					// Filter out unnecessary platform-specific libraries
					.filter(component -> !config.getSupportedOsList().contains(OsType.valueOf(component.getPlatform())))
					.filter(component -> !config.getSupportedArchList().contains(component.getArchitecture())).forEach(
							component -> output.sub(EntryPath.get("lib/" + source.getFileName(), component.getPath())));

		});

		// Add plugin binaries
		if (true) {
			for (var plugin : PluginStore.values().stream()
					.filter(plugin -> config.getPluginList().contains(plugin.getPackageId())).toArray(Plugin[]::new)) {
				ZipSet pluginArchive = new ZipSet();

				// Add core component
				Path core = plugin.getComponent(null, null);
				pluginArchive.add("core.jar", core);
				SoiUtil.getMatrix(core).getAllDependencies().forEach(dep -> {
					output.add("lib/" + ArtifactUtil.ParsedCoordinate.fromCoordinate(dep.getCoordinates()).filename,
							ArtifactUtil.getArtifactFile(Environment.LIB.path(), dep.getCoordinates()));
				});

				// Add vanilla component
				Path mega = plugin.getComponent(InstanceType.AGENT, InstanceFlavor.VANILLA);
				pluginArchive.add("agent/vanilla.jar", mega);
				SoiUtil.getMatrix(mega).getAllDependencies().forEach(dep -> {
					output.add("lib/" + ArtifactUtil.ParsedCoordinate.fromCoordinate(dep.getCoordinates()).filename,
							ArtifactUtil.getArtifactFile(Environment.LIB.path(), dep.getCoordinates()));
				});

				output.add(EntryPath.get("lib/" + plugin.getPackageId() + ".jar"), pluginArchive);
			}
		}

		return output.build();
	}
}
