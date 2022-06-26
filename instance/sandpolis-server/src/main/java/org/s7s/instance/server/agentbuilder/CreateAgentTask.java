//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.agentbuilder;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.s7s.core.clientserver.Messages.RQ_BuildAgent.DeploymentOptions;
import org.s7s.core.clientserver.Messages.RQ_BuildAgent.GeneratorOptions;
import org.s7s.core.clientserver.Messages.RQ_BuildAgent.PackagerOptions;
import org.s7s.core.protocol.Group.RS_BuildAgent;
import org.s7s.core.instance.Group.AgentConfig;
import org.s7s.core.server.agentbuilder.deployer.AgentDeployer;
import org.s7s.core.server.agentbuilder.deployer.SshDeployer;
import org.s7s.core.server.agentbuilder.generator.AgentGenerator;
import org.s7s.core.server.agentbuilder.generator.MicroGenerator;
import org.s7s.core.server.agentbuilder.generator.VanillaGenerator;
import org.s7s.core.server.agentbuilder.packager.AgentPackager;

public class CreateAgentTask {

	private static final Logger log = LoggerFactory.getLogger(CreateAgentTask.class);

	private boolean started;

	/**
	 * The group's agent configuration.
	 */
	private final AgentConfig config;

	private final GeneratorOptions generatorOptions;
	private final PackagerOptions packagerOptions;
	private final DeploymentOptions deploymentOptions;

	public CreateAgentTask(AgentConfig config, GeneratorOptions generatorOptions, PackagerOptions packagerOptions,
			DeploymentOptions deploymentOptions) {
		this.config = Objects.requireNonNull(config);

		this.generatorOptions = generatorOptions;
		this.packagerOptions = packagerOptions;
		this.deploymentOptions = deploymentOptions;
	}

	public CompletionStage<RS_BuildAgent> start() {
		if (started)
			throw new IllegalStateException("The generator has already been started");

		started = true;
		return CompletableFuture.supplyAsync(() -> {

			try {

				// Run generator
				var generatedAgent = loadGenerator().run(null);

				// Run packager
				var packagedAgent = loadPackager().run(generatedAgent);

				// Run deployer
				loadDeployer().run(packagedAgent);

			} catch (Exception e) {
				log.error("Generation failed", e);
				return RS_BuildAgent.BUILD_AGENT_FAILED;
			}
			return RS_BuildAgent.BUILD_AGENT_OK;
		});
	}

	private AgentGenerator loadGenerator() {
		switch (generatorOptions.getPayload()) {
		case "vanilla":
			return new VanillaGenerator(generatorOptions, config);
		case "micro":
			return new MicroGenerator(generatorOptions, config);
		default:
			throw new RuntimeException();
		}
	}

	private AgentPackager loadPackager() {
		switch (packagerOptions.getFormat()) {

		default:
			throw new RuntimeException();
		}
	}

	private AgentDeployer loadDeployer() {
		switch ("ssh") {
		case "ssh":
			return new SshDeployer(deploymentOptions);
		default:
			throw new RuntimeException();
		}
	}
}
