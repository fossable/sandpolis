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

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.protobuf.ByteString;
import com.google.protobuf.UnsafeByteOperations;
import com.sandpolis.core.clientserver.msg.MsgGroup.RS_GenerateArtifact;
import com.sandpolis.core.instance.Group.AgentConfig;

/**
 * {@link Generator} is a base class for all artifact generators. There are
 * currently two generators: {@link ArtifactGeneratorVanilla} and
 * {@link ArtifactGeneratorMicro}.
 *
 * @since 4.0.0
 */
public abstract class Generator implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(Generator.class);

	private RS_GenerateArtifact result;

	private boolean started;

	/**
	 * The group's agent configuration.
	 */
	protected final AgentConfig config;

	protected final Packager packager;

	protected Generator(AgentConfig config, Packager packager) {
		this.config = Objects.requireNonNull(config);
		this.packager = Objects.requireNonNull(packager);
	}

	/**
	 * Get the generation report.
	 *
	 * @return The completed generation response
	 */
	public RS_GenerateArtifact getResult() {
		return result;
	}

	@Override
	public void run() {
		if (started)
			throw new IllegalStateException("The generator has already been started");

		started = true;

		var result = RS_GenerateArtifact.newBuilder();

		try {
			result.setTimestamp(System.currentTimeMillis());
			byte[] binary = generate();

			// Compute metadata
			result.setOutputSize(binary.length);
			result.setOutputSha256(ByteSource.wrap(binary).hash(Hashing.sha256()).toString());
			result.setOutputSha512(ByteSource.wrap(binary).hash(Hashing.sha512()).toString());
			result.setOutput(UnsafeByteOperations.unsafeWrap(binary));

//			// Find latest number
//			int seq = Files.list(Environment.GEN.path()).mapToInt(path -> {
//				return Integer.parseInt(getNameWithoutExtension(path.getFileName().toString()));
//			}).sorted().findFirst().orElse(-1) + 1;
//
//			// Write to archive directory
//			Path archive = Environment.GEN.path().resolve(String.format("%d%s", seq, archiveExtension));
//			log.debug("Writing archive: {} ({})", archive, TextUtil.formatByteCount(result.length));
//			Files.write(archive, result);
		} catch (Exception e) {
			log.error("Generation failed", e);
			result.setResult(false);
		} finally {
			// TODO
			System.currentTimeMillis();
			this.result = result.build();
		}
	}

	/**
	 * Performs the generation synchronously.
	 */
	protected abstract byte[] generate() throws Exception;
}
