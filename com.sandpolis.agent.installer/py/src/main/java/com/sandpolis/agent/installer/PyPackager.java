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
package com.sandpolis.agent.installer;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.sandpolis.core.instance.Generator.GenConfig;
import com.sandpolis.core.server.generator.MegaGen;

/**
 * This generator produces a Python script.
 *
 * @author cilki
 * @since 5.0.0
 */
public class PyPackager implements Packager {
	public PyPackager(GenConfig config) {
		super(config, ".py", "/lib/sandpolis-client-installer.py");
	}

	@Override
	protected byte[] generate() throws Exception {
		Map<String, String> cfg = new HashMap<>();

		String stub = readArtifactString();
		stub.replaceFirst("# PLACEHOLDER",
				cfg.entrySet().stream()
						.map(entry -> String.format("config['%s'] = '%s'%n", entry.getKey(), entry.getValue()))
						.collect(Collectors.joining()));

		return stub.getBytes();
	}

}