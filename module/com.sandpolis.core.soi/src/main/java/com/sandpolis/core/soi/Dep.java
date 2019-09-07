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
package com.sandpolis.core.soi;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sandpolis.core.soi.Dependency.SO_DependencyMatrix;
import com.sandpolis.core.soi.Dependency.SO_DependencyMatrix.Artifact;

/**
 * This class acts like a wrapper for {@link Artifact} and adds easy access to
 * child dependencies.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class Dep {

	private Artifact artifact;

	private List<Dep> children;

	public Dep(SO_DependencyMatrix matrix, Artifact artifact) {
		Objects.requireNonNull(matrix);
		this.artifact = Objects.requireNonNull(artifact);

		this.children = artifact.getDependencyList().stream().map(id -> new Dep(matrix, matrix.getArtifact(id)))
				.collect(Collectors.toList());
	}

	public Artifact getArtifact() {
		return artifact;
	}

	public String getCoordinates() {
		return artifact.getCoordinates();
	}

	public String getVersion() {
		// TODO
		return null;
	}

	public String getArtifactId() {
		// TODO
		return null;
	}

	public String getGroupId() {
		// TODO
		return null;
	}

	public long getSize() {
		return artifact.getSize();
	}

	/**
	 * Get the {@link Dep}'s direct dependencies.
	 *
	 * @return A stream of direct dependencies
	 */
	public Stream<Dep> getDependencies() {
		return children.stream();
	}

	/**
	 * Get the {@link Dep}'s transitive dependencies.
	 *
	 * @return A stream of transitive dependencies
	 */
	public Stream<Dep> getAllDependencies() {
		return Stream.concat(getDependencies(), getDependencies().flatMap(dep -> dep.getDependencies())).distinct();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Dep)
			return getCoordinates().equals(((Dep) obj).getCoordinates());

		return false;
	}
}
