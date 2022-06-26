//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.agentbuilder.generator;

import static org.s7s.core.instance.plugin.PluginStore.PluginStore;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cilki.zipset.ZipSet;
import org.s7s.core.clientserver.Messages.RQ_BuildAgent.GeneratorOptions;
import org.s7s.core.foundation.S7SFile;
import org.s7s.core.instance.Group.AgentConfig;
import org.s7s.core.instance.InstanceContext;
import org.s7s.core.instance.plugin.Plugin;
import org.s7s.core.instance.state.InstanceOids.ProfileOid.PluginOid;
import org.s7s.core.server.group.Group;

/**
 * This generator builds a {@code org.s7s.agent.vanilla} agent.
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

	protected Properties buildDistagentConfig() throws IOException {
		// TODO
		return null;
	}

	@Override
	public GeneratedAgent run(Group group) throws Exception {
		log.debug("Computing artifact");

		Path agent = S7SFile.of(InstanceContext.PATH_LIB.get()).findModule("org.s7s.agent.vanilla").orElseThrow();

		Map<String, byte[]> executables = new HashMap<>();

		ZipSet output = new ZipSet(agent);

		// Add agent configuration
		output.add("soi/agent.bin", config.toByteArray());

//		Properties so_build = JarUtil.getResource(agent, "build.properties", in -> {
//			var props = new Properties();
//			props.load(in);
//			return props;
//		});
//
//		// Add dependencies
//		for (var dependency : so_build.getProperty("build.dependencies").split(",")) {
//			// TODO
//		}

		// Add plugin binaries
		for (var plugin : PluginStore.values().stream()
				.filter(plugin -> config.getPluginList().contains(plugin.get(PluginOid.PACKAGE_ID).asString()))
				.toArray(Plugin[]::new)) {
			// TODO
		}

		return new GeneratedAgent(group, executables);
	}
}
