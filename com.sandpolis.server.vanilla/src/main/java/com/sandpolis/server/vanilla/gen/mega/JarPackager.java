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
package com.sandpolis.server.vanilla.gen.mega;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.stream.Collectors;

import com.github.cilki.zipset.ZipSet;
import com.github.cilki.zipset.ZipSet.EntryPath;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.util.ArtifactUtil;
import com.sandpolis.server.vanilla.gen.MegaGen;

import com.sandpolis.core.proto.util.Platform.OsType;
import com.sandpolis.core.proto.util.Generator.GenConfig;

/**
 * This generator produces a runnable jar file.
 *
 * @author cilki
 * @since 5.0.0
 */
public class JarPackager extends MegaGen {
	public JarPackager(GenConfig config) {
		super(config);
	}

	@Override
	protected void generate() throws Exception {
		Path client = Environment.LIB.path().resolve("sandpolis-client-mega-" + Core.SO_BUILD.getVersion() + ".jar");

		ZipSet output;
		if (config.getMega().getMemory()) {
			output = new ZipSet(client);

			// Add client configuration
			output.add("soi/client.bin", config.getMega().toByteArray());

			for (String gav : getDependencies()) {
				String filename = String.format("%s-%s.jar", gav.split(":")[1], gav.split(":")[2]);

				// TODO merge
			}
		} else {
			Properties cfg = new Properties();
			cfg.put("modules", getDependencies().stream().collect(Collectors.joining(" ")));
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

			Path installer = Environment.LIB.path()
					.resolve("sandpolis-client-installer-" + Core.SO_BUILD.getVersion() + ".jar");

			output = new ZipSet(installer);

			// Add installer configuration
			try (var out = new ByteArrayOutputStream()) {
				cfg.store(out, null);

				output.add("config.properties", out.toByteArray());
			}

			// Add client
			output.add(EntryPath.get("lib/" + installer.getFileName()), client);

			// Add client configuration
			output.add(EntryPath.get("lib/" + installer.getFileName(), "soi/client.bin"),
					config.getMega().toByteArray());

			if (config.getMega().getDownloader()) {

			} else {
				for (String dependency : getDependencies()) {
					Path source = ArtifactUtil.getArtifactFile(Environment.LIB.path(), dependency);

					if (Files.exists(source)) {
						// Add library
						output.add("lib/" + source.getFileName(), source);
					} else {
						// TODO
					}
				}
			}
		}

		output.build(Environment.GEN.path().resolve("0.jar"));
	}
}
