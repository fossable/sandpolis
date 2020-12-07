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

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import com.github.cilki.zipset.ZipSet;
import com.github.cilki.zipset.ZipSet.EntryPath;
import com.sandpolis.core.foundation.util.ArtifactUtil;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.server.generator.Packager;
import com.sandpolis.core.server.group.Group;

/**
 * {@link JarPackager} produces a runnable jar artifact.
 *
 * @since 5.0.0
 */
public class JarPackager implements Packager {

	@Override
	public byte[] generate(Group group) throws Exception {
		Path agent = Environment.LIB.path().resolve("sandpolis-agent-mega-" + Core.SO_BUILD.getVersion() + ".jar");

		ZipSet output = null;
//		if (config.getMemory()) {
//			output = new ZipSet(agent);
//
//			// Add agent configuration
//			output.add("soi/agent.bin", config.toByteArray());
//
//			for (String gav : getDependencies()) {
//				String filename = String.format("%s-%s.jar", gav.split(":")[1], gav.split(":")[2]);
//
//				// TODO merge
//			}
//		} else {
//			Properties cfg = buildInstallerConfig();
//			cfg.setProperty("screen.session", "com.sandpolis.agent.vanilla");
//
//			output = new ZipSet(readArtifactBinary());
//
//			// Add installer configuration
//			try (var out = new ByteArrayOutputStream()) {
//				cfg.store(out, null);
//
//				output.add("config.properties", out.toByteArray());
//			}
//
//			if (!config.getDownloader()) {
//				for (String dependency : getDependencies()) {
//					Path source = ArtifactUtil.getArtifactFile(Environment.LIB.path(), dependency);
//
//					if (Files.exists(source)) {
//						// Add library
//						output.add("lib/" + source.getFileName(), source);
//					} else {
//						throw new FileNotFoundException(source.toString());
//					}
//				}
//			}
//
//			// Add agent configuration
//			output.add(EntryPath.get("lib/" + agent.getFileName(), "soi/agent.bin"), config.toByteArray());
//		}

		return output.build();
	}
}
