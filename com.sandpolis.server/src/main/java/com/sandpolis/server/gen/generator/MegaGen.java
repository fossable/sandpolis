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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

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

		// Make convenient references for common objects
		FeatureSet features = config.getMega().getFeatures();

		// Compute libraries for injection according to the configuration
		List<ZipEntrySource> entries = ArtifactStore.getDependencies(Instance.CLIENT)
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

						// Find unnecessary internal native components
						String[] paths = artifact.getNativeComponentList().stream()
								.filter(component -> !features.getSupportedOsList().contains(component.getPlatform()))
								.filter(component -> !features.getSupportedArchList()
										.contains(component.getArchitecture()))
								.map(component -> component.getPath()).toArray(String[]::new);

						// Omit the components
						try (var out = new ByteArrayOutputStream()) {
							ZipUtil.removeEntries(source, paths, out);
							return new ByteSource("lib/" + source.getName(), out.toByteArray());
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
					return new FileSource("lib/" + source.getName(), source);

				}).collect(Collectors.toList());

		File client = Environment.get(JLIB).resolve("com.sandpolis.client.mega-standalone.jar").toFile();

		try (var zip = new ZipFile(client);
				var main_in = zip.getInputStream(zip.getEntry("main/main.jar"));
				var main_out = new ByteArrayOutputStream()) {
			// Add client config
			ZipUtil.addEntries(main_in,
					new ZipEntrySource[] { new ByteSource("soi/client.bin", config.getMega().toByteArray()) },
					main_out);

			entries.add(new ByteSource("main/main.jar", main_out.toByteArray()));
		}

		////////////////////////////////////////////////////////
		// TODO remove when zt-zip adds a stream-based method //
		byte[] temp;
		try (var out = new ByteArrayOutputStream()) {
			ZipUtil.removeEntries(client, new String[] { "main/main.jar" }, out);
			temp = out.toByteArray();
		}
		////////////////////////////////////////////////////////

		// Inject artifacts
		try (var out = new ByteArrayOutputStream(); var in = new ByteArrayInputStream(temp)) {
			ZipUtil.addEntries(in, entries.stream().toArray(ZipEntrySource[]::new), out);
			return out.toByteArray();
		}
	}

}
