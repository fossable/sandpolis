//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.server.agentbuilder.generator;

import static com.sandpolis.core.instance.plugin.PluginStore.PluginStore;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cilki.zipset.ZipSet;
import com.sandpolis.core.clientserver.msg.MsgAgentbuilder.GeneratorOptions;
import com.sandpolis.core.foundation.Platform.OsType;
import com.sandpolis.core.foundation.util.FileUtil;
import com.sandpolis.core.foundation.util.JarUtil;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.Group.AgentConfig;
import com.sandpolis.core.instance.plugin.Plugin;
import com.sandpolis.core.server.group.Group;

/**
 * This generator builds a {@code com.sandpolis.agent.vanilla} agent.
 *
 * @since 2.0.0
 */
public class VanillaGenerator implements AgentGenerator {

	private static final Logger log = LoggerFactory.getLogger(VanillaGenerator.class);

	private GeneratorOptions options;

	private AgentConfig config;

	public VanillaGenerator(GeneratorOptions options, AgentConfig config) {
		this.options = Objects.requireNonNull(options);
		this.config = Objects.requireNonNull(config);
	}

	protected Properties buildInstallerConfig() throws IOException {
		Properties cfg = new Properties();

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

	@Override
	public GeneratedAgent run(Group group) throws Exception {
		log.debug("Computing artifact");

		Path agent = FileUtil.findModule(Environment.LIB.path(), "com.sandpolis.agent.vanilla").orElseThrow();

		Map<String, byte[]> executables = new HashMap<>();

		ZipSet output = new ZipSet(agent);

		// Add agent configuration
		output.add("soi/agent.bin", config.toByteArray());

		Properties so_build = JarUtil.getResource(agent, "build.properties", in -> {
			var props = new Properties();
			props.load(in);
			return props;
		});

		// Add dependencies
		for (var dependency : so_build.getProperty("build.dependencies").split(",")) {
			// TODO
		}

		// Add plugin binaries
		for (var plugin : PluginStore.values().stream()
				.filter(plugin -> config.getPluginList().contains(plugin.getPackageId())).toArray(Plugin[]::new)) {
			// TODO
		}

		return new GeneratedAgent(group, executables);
	}
}
