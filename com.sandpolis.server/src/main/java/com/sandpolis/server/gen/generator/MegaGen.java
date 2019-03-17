/******************************************************************************
 *                                                                            *
 *                    Copyright 2015 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.server.gen.generator;

import static com.sandpolis.core.instance.Environment.EnvPath.LIB;
import static com.sandpolis.core.instance.store.artifact.ArtifactUtil.ParsedCoordinate.fromArtifact;
import static com.sandpolis.core.instance.store.artifact.ArtifactUtil.ParsedCoordinate.fromCoordinate;

import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cilki.zipset.ZipSet;
import com.github.cilki.zipset.ZipSet.EntryPath;
import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.store.artifact.ArtifactUtil;
import com.sandpolis.core.proto.util.Generator.FeatureSet;
import com.sandpolis.core.proto.util.Generator.GenConfig;
import com.sandpolis.core.proto.util.Platform.Architecture;
import com.sandpolis.core.proto.util.Platform.OsType;
import com.sandpolis.core.soi.SoiUtil;
import com.sandpolis.server.gen.FileGenerator;

/**
 * This generator builds a MEGA client.
 *
 * @author cilki
 * @since 2.0.0
 */
public class MegaGen extends FileGenerator {

	private static final Logger log = LoggerFactory.getLogger(MegaGen.class);

	/**
	 * Dependencies that are required for the client to run and therefore should
	 * always be included in the output.
	 */
	private static final List<String> required = List.of("com.sandpolis.core.soi", "com.sandpolis.core.instance");

	public MegaGen(GenConfig config) {
		super(config);
	}

	@Override
	protected Object run() throws Exception {

		log.debug("Computing MEGA payload");

		ZipSet output = new ZipSet(Environment.get(LIB).resolve("com.sandpolis.client.mega-standalone.jar"));
		FeatureSet features = config.getMega().getFeatures();

		// Add client configuration
		output.add("/main/main.jar!/soi/client.bin", config.getMega().toByteArray());

		// Add client dependencies
		SoiUtil.getMatrix(Environment.get(LIB).resolve("com.sandpolis.client.mega.jar")).getAllDependencies()
				// Skip unnecessary dependencies if allowed
				.filter(artifact -> !config.getMega().getDownloader() || required.contains(artifact.getArtifactId()))
				.forEach(artifact -> {
					Path source = ArtifactUtil.getArtifactFile(artifact.getArtifact());

					// Add library
					output.add("lib/" + source.getFileName(), source);

					// Skip native dependencies if possible
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
				Path bin = ArtifactUtil.getArtifactFile(plugin);
				output.add("lib/" + fromCoordinate(plugin).filename, bin);

				// Add plugin dependencies
				SoiUtil.readMatrix(bin).getArtifactList().stream().forEach(dep -> {
					output.add("lib/" + fromArtifact(dep).filename, ArtifactUtil.getArtifactFile(dep));
				});
			}
		}

		return output.build();
	}

}
