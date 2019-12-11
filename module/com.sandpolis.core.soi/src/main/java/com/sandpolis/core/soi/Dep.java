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

	public Stream<Dep> getAllDependenciesInclude() {
		return Stream.concat(Stream.of(this), getAllDependencies());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Dep)
			return getCoordinates().equals(((Dep) obj).getCoordinates());

		return false;
	}

	@Override
	public int hashCode() {
		return getCoordinates().hashCode();
	}
}
