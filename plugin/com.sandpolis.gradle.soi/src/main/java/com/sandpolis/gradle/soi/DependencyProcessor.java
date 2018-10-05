/******************************************************************************
 *                                                                            *
 *                    Copyright 2017 Subterranean Security                    *
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
package com.sandpolis.gradle.soi;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolvedDependency;

import com.google.common.io.Resources;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.sandpolis.core.proto.soi.Dependency.SO_DependencyMatrix;
import com.sandpolis.core.proto.soi.Dependency.SO_DependencyMatrix.Artifact;
import com.sandpolis.core.proto.soi.Dependency.SO_DependencyMatrix.Artifact.NativeComponent;
import com.sandpolis.core.proto.util.Generator.Feature;
import com.sandpolis.core.proto.util.Platform.Architecture;
import com.sandpolis.core.proto.util.Platform.OsType;

/**
 * This class collects Gradle dependencies and produces a
 * {@link SO_DependencyMatrix} which can be used in the SOI task.
 * 
 * @author cilki
 */
public class DependencyProcessor {

	/**
	 * A list of mutable {@link Artifact}s.
	 */
	private List<Artifact.Builder> artifacts = new ArrayList<>();

	/**
	 * A mapping from objects in {@link #artifacts} to file artifacts.
	 */
	private Map<Artifact.Builder, File> files = new HashMap<>();

	/**
	 * The root project.
	 */
	private Project root;

	public DependencyProcessor(Project root) {
		this.root = root;
	}

	/**
	 * Add a new dependency relationship to the processor.
	 * 
	 * @param instance   The instance artifact name
	 * @param dependency The artifact's dependency
	 */
	public synchronized void add(String instance, ResolvedDependency dependency) {
		add(Artifact.newBuilder().setCoordinates(":" + instance + ":"), dependency);
	}

	/**
	 * Add a new dependency relationship to the processor.
	 * 
	 * @param parent     The parent artifact
	 * @param dependency The artifact's dependency
	 */
	private void add(Artifact.Builder parent, ResolvedDependency dependency) {
		if (!artifacts.stream().anyMatch(a -> a.getCoordinates().equals(parent.getCoordinates())))
			artifacts.add(parent);

		Artifact.Builder artifact = getArtifact(dependency);
		if (!artifacts.stream().anyMatch(a -> a.getCoordinates().equals(artifact.getCoordinates()))) {
			artifacts.add(artifact);
		}

		for (int i = 0; i < artifacts.size(); i++) {
			if (artifacts.get(i).getCoordinates().equals(artifact.getCoordinates())) {
				parent.addDependency(i);
				break;
			}
		}

		// Recursively add child dependencies
		for (ResolvedDependency child : dependency.getChildren()) {
			add(artifact, child);
		}
	}

	/**
	 * Build the contents of the {@link DependencyProcessor} into a
	 * {@link SO_DependencyMatrix}.
	 * 
	 * @return A new {@link SO_DependencyMatrix}
	 */
	@SuppressWarnings("unchecked")
	public SO_DependencyMatrix build() {
		SO_DependencyMatrix.Builder deps = SO_DependencyMatrix.newBuilder();

		// Read feature extension from root project
		Map<String, List<String>> features = (Map<String, List<String>>) root.getExtensions().getExtraProperties()
				.get("feature_matrix");

		// Flatten each feature into the mutable artifact's list
		for (String feature : features.keySet()) {
			for (Artifact.Builder artifact : artifacts) {
				for (String dependency : features.get(feature)) {
					if (artifact.getCoordinates().equals(dependency)) {
						artifact.addFeature(Feature.valueOf(feature.toUpperCase()));
					}
				}
			}
		}

		// Read natives extension from root project
		Map<String, Map<String, Map<String, String>>> natives = (Map<String, Map<String, Map<String, String>>>) root
				.getExtensions().getExtraProperties().get("native_matrix");

		// Flatten each native component into the mutable artifact's list
		for (String dependency : natives.keySet()) {
			for (Artifact.Builder artifact : artifacts) {
				if (artifact.getCoordinates().contains(":" + dependency.replaceAll("_", "-") + ":")) {
					for (String platform : natives.get(dependency).keySet()) {
						for (String architecture : natives.get(dependency).get(platform).keySet()) {

							EnumValueDescriptor arch = Architecture.getDescriptor().findValueByName(architecture);
							EnumValueDescriptor os = OsType.getDescriptor().findValueByName(platform.toUpperCase());

							NativeComponent.Builder component = NativeComponent.newBuilder()
									.setPath(natives.get(dependency).get(platform).get(architecture));

							if (arch != null)
								component.setArchitecture(Architecture.valueOf(arch));
							else
								throw new RuntimeException("Missing: Architecture." + architecture);

							if (os != null)
								component.setPlatform(OsType.valueOf(os));
							else
								throw new RuntimeException("Missing: OsType." + platform.toUpperCase());

							try {
								// Get component size
								component.setSize(Resources.asByteSource(new URL("jar:file:"
										+ files.get(artifact).getAbsolutePath() + "!" + component.getPath())).size());
							} catch (IOException e) {
								throw new RuntimeException(e);
							}

							artifact.addNativeComponent(component);
						}
					}
				}
			}
		}

		return deps.addAllArtifact(artifacts.stream().map(a -> a.build()).collect(Collectors.toList())).build();
	}

	/**
	 * Build a new artifact representing the given dependency.
	 * 
	 * @param dependency The dependency
	 * @return An artifact representing the dependency
	 */
	private Artifact.Builder getArtifact(ResolvedDependency dependency) {
		if (dependency == null)
			throw new IllegalArgumentException();

		File resource = dependency.getModuleArtifacts().iterator().next().getFile();

		Artifact.Builder artifact = Artifact.newBuilder().setCoordinates(getCoordinates(dependency))
				.setSize(resource.length());

		files.put(artifact, resource);
		return artifact;
	}

	/**
	 * Get a dependency's coordinates in standard Gradle form.
	 * 
	 * @param dependency The dependency
	 * @return The dependency's coordinates
	 */
	private static String getCoordinates(ResolvedDependency dependency) {
		if (dependency == null)
			throw new IllegalArgumentException();

		if (dependency.getModuleGroup().contains("Sandpolis"))
			return String.format(":%s:", dependency.getModuleName());

		return String.format("%s:%s:%s", dependency.getModuleGroup(), dependency.getModuleName(),
				dependency.getModuleVersion());
	}

}
