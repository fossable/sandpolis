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
package com.sandpolis.server.vanilla.gen.generator;

import static com.sandpolis.core.instance.Environment.EnvPath.LIB;
import static com.sandpolis.core.util.ArtifactUtil.ParsedCoordinate.fromCoordinate;

import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cilki.zipset.ZipSet;
import com.github.cilki.zipset.ZipSet.EntryPath;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.proto.util.Generator.FeatureSet;
import com.sandpolis.core.proto.util.Generator.GenConfig;
import com.sandpolis.core.proto.util.Platform.Architecture;
import com.sandpolis.core.proto.util.Platform.OsType;
import com.sandpolis.core.soi.SoiUtil;
import com.sandpolis.core.util.ArtifactUtil;
import com.sandpolis.server.vanilla.gen.FileGenerator;

/**
 * This generator builds a {@code com.sandpolis.client.mega} client.
 *
 * @author cilki
 * @since 2.0.0
 */
public class MegaGen extends FileGenerator {

	private static final Logger log = LoggerFactory.getLogger(MegaGen.class);

	/**
	 * Dependencies that must be packaged under every circumstance.
	 */
	private static final List<String> include = List.of("sandpolis-core-soi", "sandpolis-core-instance");

	/**
	 * Dependencies that should not be packaged under any circumstance.
	 */
	private static final List<String> exclude = List.of("compact-classloader");

	public MegaGen(GenConfig config) {
		super(config);
	}

	@Override
	protected Object run() throws Exception {
		log.debug("Computing MEGA payload");

		Path client = Environment.get(LIB).resolve("sandpolis-client-mega-" + Core.SO_BUILD.getVersion() + ".jar");

		ZipSet output = new ZipSet(client);
		FeatureSet features = config.getMega().getFeatures();

		// Add client configuration
		output.add("soi/client.bin", config.getMega().toByteArray());

		// Add client dependencies
		SoiUtil.getMatrix(client).getAllDependencies()
				// Skip unnecessary dependencies if allowed
				.filter(artifact -> !config.getMega().getDownloader() || include.contains(artifact.getArtifactId()))
				.filter(artifact -> !exclude.contains(artifact.getArtifactId())) //
				.forEach(artifact -> {
					Path source = ArtifactUtil.getArtifactFile(Environment.get(LIB),
							artifact.getArtifact().getCoordinates());

					// Add library
					output.add("lib/" + source.getFileName(), source);

					// Strip native dependencies if possible
					artifact.getArtifact().getNativeComponentList().stream()
							// Filter out unnecessary platform-specific libraries
							.filter(component -> !features.getSupportedOsList()
									.contains(OsType.valueOf(component.getPlatform())))
							.filter(component -> !features.getSupportedArchList()
									.contains(Architecture.valueOf(component.getArchitecture())))
							.forEach(component -> {
								output.sub(EntryPath.get("lib/" + source.getFileName(), component.getPath()));
							});

				});

		// Add plugin binaries
		if (!config.getMega().getDownloader()) {
			for (String plugin : features.getPluginList()) {
				Path bin = ArtifactUtil.getArtifactFile(Environment.get(LIB), ":" + plugin + ":5.1.0");
				output.add("lib/" + fromCoordinate(":" + plugin + ":5.1.0").filename, bin);

				// Add plugin dependencies
				SoiUtil.readMatrix(bin).getArtifactList().stream().forEach(dep -> {
					output.add("lib/" + fromCoordinate(dep.getCoordinates()).filename,
							ArtifactUtil.getArtifactFile(Environment.get(LIB), dep.getCoordinates()));
				});

				// Remove unnecessary plugin components
				// TODO
			}
		}

		return output.build();
	}

}
