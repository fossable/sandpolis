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

import static com.sandpolis.core.instance.Environment.EnvPath.JLIB;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ByteSource;
import org.zeroturnaround.zip.FileSource;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipUtil;

import com.sandpolis.core.instance.Environment;
import com.sandpolis.core.instance.store.artifact.ArtifactStore;
import com.sandpolis.core.proto.util.Generator.Feature;
import com.sandpolis.core.proto.util.Generator.FeatureSet;
import com.sandpolis.core.proto.util.Generator.GenConfig;
import com.sandpolis.core.proto.util.Platform.Instance;
import com.sandpolis.server.gen.FileGenerator;

/**
 * This generator builds a MEGA client.
 *
 * @author cilki
 * @since 2.0.0
 */
public class MegaGen extends FileGenerator {

	private static final Logger log = LoggerFactory.getLogger(MegaGen.class);

	public MegaGen(GenConfig config) {
		super(config);
	}

	@Override
	protected Object run() throws Exception {
		File client = Environment.get(JLIB).resolve("com.sandpolis.client.mega.jar").toFile();
		FeatureSet features = config.getMega().getFeatures();

		// A list of entries to be injected
		List<ZipEntrySource> sources = new ArrayList<>();

		// Add client config
		sources.add(new ByteSource("client.bin", config.getMega().toByteArray()));

		// Filter libraries according to features and target platforms
		sources.addAll(ArtifactStore.getDependencies(Instance.CLIENT)
				// Filter by feature
				.filter(artifact -> {
					for (Feature feature : artifact.getFeatureList()) {
						if (!features.getFeatureList().contains(feature)) {
							return false;
						}
					}
					return true;
				}).map(artifact -> {
					File source = ArtifactStore.getArtifactFile(artifact);
					if (artifact.getNativeComponentCount() != 0) {

						// Collect unnecessary internal native components
						String[] paths = artifact.getNativeComponentList().stream()
								.filter(component -> !features.getSupportedOsList().contains(component.getPlatform()))
								.filter(component -> !features.getSupportedArchList()
										.contains(component.getArchitecture()))
								.map(component -> component.getPath()).toArray(String[]::new);

						// TODO use removeEntries(File, String[], OutputStream) rather than copying to a
						// temporary file (if that method is ever added to zt-zip).
						File staging = null;
						try {
							staging = Files.createTempFile(null, null).toFile();
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
						ZipUtil.removeEntries(source, paths, staging);

						return new FileSource("lib/" + source.getName(), staging);
					}
					return new FileSource("lib/" + source.getName(), source);

				}).collect(Collectors.toList()));

		// Inject artifacts
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			ZipUtil.addEntries(client, sources.stream().toArray(ZipEntrySource[]::new), out);
			return out.toByteArray();
		}
	}

}
