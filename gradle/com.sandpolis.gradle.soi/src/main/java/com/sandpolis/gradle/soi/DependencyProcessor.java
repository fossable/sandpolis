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
package com.sandpolis.gradle.soi;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;

import com.sandpolis.core.foundation.soi.Dependency.SO_DependencyMatrix;
import com.sandpolis.core.foundation.soi.Dependency.SO_DependencyMatrix.Artifact;

/**
 * This class accepts Gradle dependencies incrementally and produces a
 * {@link SO_DependencyMatrix}.<br>
 * <br>
 * Note: this class is NOT thread-safe. Concurrent operations will produce an
 * invalid {@link SO_DependencyMatrix} result.
 *
 * @author cilki
 */
public final class DependencyProcessor {

	/**
	 * The list artifacts that will be built into a {@link SO_DependencyMatrix}.
	 */
	private List<Artifact.Builder> artifacts;

	/**
	 * Build a new {@link DependencyProcessor} for the given {@link Project}.
	 *
	 * @param project The root project
	 */
	public DependencyProcessor(Project project) {
		this.artifacts = new ArrayList<>();

		add(Objects.requireNonNull(project));
	}

	/**
	 * Build the contents of the {@link DependencyProcessor} into a
	 * {@link SO_DependencyMatrix}.
	 *
	 * @return A new {@link SO_DependencyMatrix}
	 */
	public SO_DependencyMatrix build() {
		var matrix = SO_DependencyMatrix.newBuilder();
		artifacts.stream().forEachOrdered(matrix::addArtifact);
		return matrix.build();
	}

	/**
	 * Add a new {@link Project} to the processor.
	 *
	 * @param project The new project
	 */
	public void add(Project project) {
		Objects.requireNonNull(project);

		if (getArtifact(project).isPresent())
			throw new IllegalArgumentException("Project already included: " + project.getName());

		// Add the artifact
		var artifact = buildArtifact(project);
		addArtifact(artifact);

		// Add the project's direct dependencies
		project.getConfigurations().getAsMap().get("runtimeClasspath").getResolvedConfiguration()
				.getFirstLevelModuleDependencies().stream()
				.filter(dependency -> dependency.getModuleArtifacts().size() == 1)
				.forEach(dependency -> add(artifact, dependency));
	}

	/**
	 * Add a new dependency relationship to the processor.
	 *
	 * @param parent     The id of the parent artifact
	 * @param dependency A dependency of the parent
	 */
	private void add(Artifact.Builder parent, ResolvedDependency dependency) {
		Objects.requireNonNull(dependency);

		getArtifact(dependency).ifPresentOrElse(artifact -> {
			int id = artifacts.indexOf(artifact);
			if (!parent.getDependencyList().contains(id))
				parent.addDependency(id);
		}, () -> {
			dependency.getModuleArtifacts().forEach(resolvedArtifact -> {
				var artifact = buildArtifact(getCoordinates(dependency), resolvedArtifact);

				// Add to matrix and parent artifact's dependency list
				parent.addDependency(addArtifact(artifact));

				// Recursively add child dependencies
				dependency.getChildren().forEach(child -> add(artifact, child));
			});
		});
	}

	/**
	 * Add an artifact to the in-progress matrix.
	 *
	 * @param artifact The artifact to add
	 * @return The artifact's new id
	 */
	private int addArtifact(Artifact.Builder artifact) {
		Objects.requireNonNull(artifact);

		if (artifacts.contains(artifact))
			throw new IllegalArgumentException("Matrix already contains artifact: " + artifact);

		artifacts.add(artifact);
		return artifacts.indexOf(artifact);
	}

	/**
	 * Build a new {@link Artifact.Builder} for the given {@link Project}.
	 *
	 * @param project The project to include
	 * @return A new artifact
	 */
	private Artifact.Builder buildArtifact(Project project) {
		return Artifact.newBuilder().setCoordinates(getCoordinates(project));
	}

	/**
	 * Build a {@link Artifact.Builder} for the given {@link File}.
	 *
	 * @param coordinates      The artifact's coordinates
	 * @param resolvedArtifact The artifact to include
	 * @return A new artifact
	 */
	private Artifact.Builder buildArtifact(String coordinates, ResolvedArtifact resolvedArtifact) {
		Objects.requireNonNull(coordinates);
		Objects.requireNonNull(resolvedArtifact);

		var artifact = Artifact.newBuilder().setCoordinates(coordinates).setSize(resolvedArtifact.getFile().length());
		if (resolvedArtifact.getClassifier() != null)
			artifact.setCoordinates(artifact.getCoordinates() + ":" + resolvedArtifact.getClassifier());

//		for (var nativeEntry : natives.entrySet()) {
//			if (artifact.getCoordinates().contains(":" + nativeEntry.getKey() + ":")) {
//				for (var platformEntry : nativeEntry.getValue().entrySet()) {
//					for (var archEntry : platformEntry.getValue().entrySet()) {
//						var component = NativeComponent.newBuilder().setPath(archEntry.getValue())
//								.setArchitecture(archEntry.getKey()).setPlatform(platformEntry.getKey().toUpperCase());
//
//						try {
//							// Get component size
//							component.setSize(Resources
//									.asByteSource(
//											new URL("jar:file:" + file.getAbsolutePath() + "!" + component.getPath()))
//									.size());
//						} catch (IOException e) {
//							throw new RuntimeException("Failed to read component size", e);
//						}
//
//						artifact.addNativeComponent(component);
//					}
//				}
//				break;
//			}
//		}
		return artifact;
	}

	/**
	 * Find an existing artifact for a project.
	 *
	 * @param project The project
	 * @return The artifact
	 */
	private Optional<Artifact.Builder> getArtifact(Project project) {
		Objects.requireNonNull(project);

		return getArtifact(getCoordinates(project));
	}

	/**
	 * Find an existing artifact for a dependency.
	 *
	 * @param dependency The dependency
	 * @return The artifact
	 */
	private Optional<Artifact.Builder> getArtifact(ResolvedDependency dependency) {
		Objects.requireNonNull(dependency);

		return getArtifact(getCoordinates(dependency));
	}

	/**
	 * Find an existing artifact in {@link #matrix} by its coordinates.
	 *
	 * @param coordinates The artifact's coordinates
	 * @return The artifact
	 */
	private Optional<Artifact.Builder> getArtifact(String coordinates) {
		Objects.requireNonNull(coordinates);

		return artifacts.stream().filter(a -> a.getCoordinates().equals(coordinates)).findAny();
	}

	/**
	 * Get a project's coordinates in standard Gradle form.
	 *
	 * @param project The project
	 * @return The project's coordinates
	 */
	private static String getCoordinates(Project project) {
		Objects.requireNonNull(project);

		return String.format("com.sandpolis:%s:%s", project.getName().replace("com.", "").replaceAll("\\.", "-"),
				project.getVersion());
	}

	/**
	 * Get a dependency's coordinates in standard Gradle form.
	 *
	 * @param dependency The dependency
	 * @return The dependency's coordinates
	 */
	private static String getCoordinates(ResolvedDependency dependency) {
		Objects.requireNonNull(dependency);

		String artifactId = dependency.getModuleGroup().equals("com.sandpolis")
				? dependency.getModuleName().replace("com.", "").replaceAll("\\.", "-")
				: dependency.getModuleName();

		return String.format("%s:%s:%s", dependency.getModuleGroup(), artifactId, dependency.getModuleVersion());
	}

}
