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
package com.sandpolis.installer.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.foundation.soi.Dependency.SO_DependencyMatrix;
import com.sandpolis.core.foundation.soi.Dependency.SO_DependencyMatrix.Artifact;
import com.sandpolis.core.foundation.util.JarUtil;
import com.sandpolis.installer.Main;

/**
 * @author cilki
 * @since 5.1.2
 */
public final class InstallUtil {

	private static final Logger log = LoggerFactory.getLogger(InstallUtil.class);

	public static Set<String> computeDependencies(SO_DependencyMatrix matrix, String coordinate) {
		Set<String> dependencies = new HashSet<>();
		computeDependencies(matrix, dependencies, coordinate);
		return dependencies.stream().map(InstallUtil::processJavaFx).collect(Collectors.toSet());
	}

	/**
	 * Gather all dependencies of the artifact corresponding to the given
	 * coordinate.
	 *
	 * @param matrix       The dependency matrix
	 * @param dependencies The dependency set
	 * @param coordinate   The coordinate
	 */
	private static void computeDependencies(SO_DependencyMatrix matrix, Set<String> dependencies, String coordinate) {
		if (dependencies.contains(coordinate))
			return;

		dependencies.add(coordinate);

		matrix.getArtifactList().stream()
				// Find the artifact in the matrix and iterate over its dependencies
				.filter(a -> a.getCoordinates().equals(coordinate)).findFirst().get().getDependencyList().stream()
				.map(matrix.getArtifactList()::get).map(Artifact::getCoordinates)
				.forEach(c -> computeDependencies(matrix, dependencies, c));

	}

	/**
	 * Translate all JavaFX platform classifiers to the current platform type.
	 *
	 * @param coordinate The input coordinate
	 * @return The output coordinate
	 */
	private static String processJavaFx(String coordinate) {
		if (coordinate.startsWith("org.openjfx:javafx-")
				&& (coordinate.endsWith(":linux") || coordinate.endsWith(":mac") || coordinate.endsWith(":win"))) {
			coordinate = coordinate.substring(0, coordinate.lastIndexOf(':'));

			// Set platform classifier
			if (Main.IS_WINDOWS)
				coordinate += ":win";
			else if (Main.IS_LINUX)
				coordinate += ":linux";
			else if (Main.IS_MAC)
				coordinate += ":mac";
			else
				throw new RuntimeException();
		}
		return coordinate;
	}

	/**
	 * Install an icon from the given instance jar.
	 *
	 * @param instance The instance jar
	 * @param icon     The location of the icon to install within the instance jar
	 * @param output   The new icon location
	 * @return {@code output}
	 * @throws IOException
	 */
	public static Path installIcon(Path instance, String icon, Path output) throws IOException {
		var url = JarUtil.getResourceUrl(instance, icon);
		if (url != null) {
			try (var in = url.openStream()) {
				Files.copy(in, output);
			}
		}
		return output;
	}

	/**
	 * Execute a system command.
	 *
	 * @param cmd The command to execute
	 * @return A new process
	 * @throws IOException
	 */
	public static Process exec(String cmd) throws IOException {
		log.debug("Executing: {}", cmd);
		return Runtime.getRuntime().exec(cmd);
	}

	private InstallUtil() {
	}
}
