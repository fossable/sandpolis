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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.attributes.Attribute;

import com.sandpolis.core.proto.soi.Dependency.SO_DependencyMatrix;
import com.sandpolis.core.proto.soi.Dependency.SO_DependencyMatrix.Artifact;

/**
 * This class collects Gradle dependencies and produces a
 * {@link SO_DependencyMatrix} which can be used in the SOI task.
 * 
 * @author cilki
 */
public class DependencyProcessor {

	private List<Artifact.Builder> matrix = new ArrayList<>();

	/**
	 * Add a new dependency relationship to the processor.
	 * 
	 * @param instance   The instance artifact
	 * @param dependency The artifact's dependency
	 */
	public void add(String instance, ExternalModuleDependency dependency) {
		add(Artifact.newBuilder().setCoordinates(":" + instance + ":"), dependency);
	}

	/**
	 * Add a new dependency relationship to the processor.
	 * 
	 * @param parent     The parent dependency
	 * @param dependency The parent's dependency
	 */
	public void add(ExternalModuleDependency parent, ExternalModuleDependency dependency) {
		add(getArtifact(parent), dependency);
	}

	/**
	 * Add a new dependency relationship to the processor.
	 * 
	 * @param parent     The parent artifact
	 * @param dependency The artifact's dependency
	 */
	public synchronized void add(Artifact.Builder parent, ExternalModuleDependency dependency) {
		System.out.println("Adding dependency: " + dependency.getName());
		if (!matrix.stream().anyMatch(a -> a.getCoordinates().equals(parent.getCoordinates())))
			matrix.add(parent);

		Artifact.Builder artifact = getArtifact(dependency);

		parent.addDependency(matrix.size());
		matrix.add(artifact);

		// TODO get child dependencies recursively
		// add(artifact, dependency.getDependencies())
	}

	/**
	 * Build the contents of the {@link DependencyProcessor} into a
	 * {@link SO_DependencyMatrix}.
	 * 
	 * @return A new {@link SO_DependencyMatrix}
	 */
	public SO_DependencyMatrix build() {
		return SO_DependencyMatrix.newBuilder()
				.addAllArtifact(matrix.stream().map(a -> a.build()).collect(Collectors.toList())).build();
	}

	/**
	 * Build a new artifact representing the given dependency.
	 * 
	 * @param dependency The dependency
	 * @return An artifact representing the dependency
	 */
	private static Artifact.Builder getArtifact(ExternalModuleDependency dependency) {
		Artifact.Builder dep = Artifact.newBuilder().setCoordinates(getCoordinates(dependency));
		dep.setFeature(dependency.getAttributes().getAttribute(Attribute.of("feature", String.class)));
		dep.setPlatform(dependency.getAttributes().getAttribute(Attribute.of("platform", String.class)));
		return dep;
	}

	/**
	 * Get a dependency's coordinates in standard Gradle form.
	 * 
	 * @param dependency The dependency
	 * @return The dependency's coordinates
	 */
	private static String getCoordinates(ExternalModuleDependency dependency) {
		return String.format("%s:%s:%s", dependency.getGroup(), dependency.getName(), dependency.getVersion());
	}

}
