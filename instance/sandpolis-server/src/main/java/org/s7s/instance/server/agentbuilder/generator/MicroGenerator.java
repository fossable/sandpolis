//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.agentbuilder.generator;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.s7s.core.clientserver.Messages.RQ_BuildAgent.GeneratorOptions;
import org.s7s.core.foundation.S7SFile;
import org.s7s.core.foundation.S7SJarFile;
import org.s7s.core.instance.Group.AgentConfig;
import org.s7s.core.instance.InstanceContext;
import org.s7s.core.server.group.Group;

/**
 * Generates a <b>org.s7s.agent.micro</b> stub.
 *
 * @since 6.1.0
 */
public class MicroGenerator implements AgentGenerator {

	private static final Logger log = LoggerFactory.getLogger(MicroGenerator.class);

	private GeneratorOptions options;

	private AgentConfig config;

	public MicroGenerator(GeneratorOptions options, AgentConfig config) {
		this.options = Objects.requireNonNull(options);
		this.config = Objects.requireNonNull(config);
	}

	@Override
	public GeneratedAgent run(Group group) throws Exception {

		Path micro = S7SFile.of(InstanceContext.PATH_LIB.get()).findModule("org.s7s.agent.micro").get();

		try (var out = new ByteArrayOutputStream()) {

			// Write the executable
			out.write(S7SJarFile.of(micro).getResource("agent-linux-x86_64", InputStream::readAllBytes).get());

			// Write config
			byte[] config_bytes = config.toByteArray();
			out.write(config_bytes);

			// Write config size
			out.write(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(config_bytes.length).array());

			// Write config path
			String config_path = "agent.config";
			out.write(config_path.getBytes());

			// Write path length
			out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(config_path.getBytes().length)
					.array());

			return new GeneratedAgent(group, Map.of("sandpolis-micro", out.toByteArray()));
		}
	}
}
