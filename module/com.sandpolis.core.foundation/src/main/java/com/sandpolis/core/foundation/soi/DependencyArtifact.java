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
package com.sandpolis.core.foundation.soi;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sandpolis.core.foundation.soi.Dependency.SO_DependencyMatrix;
import com.sandpolis.core.foundation.soi.Dependency.SO_DependencyMatrix.Artifact;

/**
 * This class is a wrapper for an {@link Artifact} and provides easy access to
 * transitive dependencies.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class DependencyArtifact {

	private final Artifact artifact;

	private final List<DependencyArtifact> children;

	public DependencyArtifact(SO_DependencyMatrix matrix, Artifact artifact) {
		Objects.requireNonNull(matrix);
		this.artifact = Objects.requireNonNull(artifact);

		this.children = artifact.getDependencyList().stream()
				.map(id -> new DependencyArtifact(matrix, matrix.getArtifact(id))).collect(Collectors.toList());
	}

	public Artifact getArtifact() {
		return artifact;
	}

	public String getCoordinates() {
		return artifact.getCoordinates();
	}

	public String getVersion() {
		return artifact.getCoordinates().split(":")[2];
	}

	public String getArtifactId() {
		return artifact.getCoordinates().split(":")[1];
	}

	public String getGroupId() {
		return artifact.getCoordinates().split(":")[0];
	}

	public long getSize() {
		return artifact.getSize();
	}

	/**
	 * Get the {@link DependencyArtifact}'s direct dependencies.
	 *
	 * @return A stream of direct dependencies
	 */
	public Stream<DependencyArtifact> getDependencies() {
		return children.stream();
	}

	/**
	 * Get the {@link DependencyArtifact}'s transitive dependencies.
	 *
	 * @return A stream of transitive dependencies
	 */
	public Stream<DependencyArtifact> getAllDependencies() {
		return Stream.concat(getDependencies(), getDependencies().flatMap(dep -> dep.getDependencies())).distinct();
	}

	public Stream<DependencyArtifact> getAllDependenciesInclude() {
		return Stream.concat(Stream.of(this), getAllDependencies());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DependencyArtifact)
			return getCoordinates().equals(((DependencyArtifact) obj).getCoordinates());

		return false;
	}

	@Override
	public int hashCode() {
		return getCoordinates().hashCode();
	}
}
