//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.server.agentbuilder;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.clientserver.msg.MsgAgentbuilder.DeploymentOptions;
import com.sandpolis.core.clientserver.msg.MsgAgentbuilder.GeneratorOptions;
import com.sandpolis.core.clientserver.msg.MsgAgentbuilder.PackagerOptions;
import com.sandpolis.core.foundation.Result.Outcome;
import com.sandpolis.core.instance.Group.AgentConfig;
import com.sandpolis.core.server.agentbuilder.deployer.AgentDeployer;
import com.sandpolis.core.server.agentbuilder.deployer.SshDeployer;
import com.sandpolis.core.server.agentbuilder.generator.AgentGenerator;
import com.sandpolis.core.server.agentbuilder.generator.MicroGenerator;
import com.sandpolis.core.server.agentbuilder.generator.VanillaGenerator;
import com.sandpolis.core.server.agentbuilder.packager.AgentPackager;

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

	public CompletionStage<Outcome> start() {
		if (started)
			throw new IllegalStateException("The generator has already been started");

		started = true;
		return CompletableFuture.supplyAsync(() -> {
			var outcome = Outcome.newBuilder();

			try {
				outcome.setTime(System.currentTimeMillis());

				// Run generator
				var generatedAgent = loadGenerator().run(null);

				// Run packager
				var packagedAgent = loadPackager().run(generatedAgent);

				// Run deployer
				loadDeployer().run(packagedAgent);

			} catch (Exception e) {
				log.error("Generation failed", e);
				outcome.setResult(false);
			} finally {
				outcome.setTime(System.currentTimeMillis() - outcome.getTime());

			}
			return outcome.build();
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
