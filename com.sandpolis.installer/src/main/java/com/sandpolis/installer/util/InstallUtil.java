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

import com.sandpolis.core.soi.Dependency;
import com.sandpolis.core.soi.Dependency.SO_DependencyMatrix.Artifact;
import com.sandpolis.installer.Main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author cilki
 * @since 5.1.2
 */
public final class InstallUtil {

	public static Set<String> computeDependencies(Dependency.SO_DependencyMatrix matrix, String coordinate) {
		Set<String> dependencies = new HashSet<>();
		computeDependencies(matrix, dependencies, coordinate);
		return dependencies;
	}

	/**
	 * Gather all dependencies of the artifact corresponding to the given
	 * coordinate.
	 *
	 * @param matrix       The dependency matrix
	 * @param dependencies The dependency set
	 * @param coordinate   The coordinate
	 */
	private static void computeDependencies(Dependency.SO_DependencyMatrix matrix, Set<String> dependencies,
			String coordinate) {
		if (dependencies.contains(coordinate))
			return;

		dependencies.add(coordinate);

		matrix.getArtifactList().stream()
				// Find the artifact in the matrix and iterate over its dependencies
				.filter(a -> a.getCoordinates().equals(coordinate)).findFirst().get().getDependencyList().stream()
				.map(matrix.getArtifactList()::get).map(Artifact::getCoordinates).map(InstallUtil::processJavaFx)
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

	public static void installLinuxDesktopEntry(String coordinate, Path executable, String name) throws IOException {
		Path destination = Paths.get("/usr/share/applications");
		if (!Files.isWritable(destination)) {
			destination = Paths.get(System.getProperty("user.home"), ".local/share/applications");
		}

		installLinuxDesktopEntry(destination, executable, coordinate, name);
	}

	public static void installLinuxDesktopEntry(Path destination, Path executable, String coordinate, String name)
			throws IOException {
		Files.createDirectories(destination);
		destination = destination.resolve(coordinate.split(":")[1] + ".desktop");

		if (Files.exists(destination))
			Files.delete(destination);
		Files.writeString(destination,
				String.join("\n",
						List.of("[Desktop Entry]", "Version=1.1", "Type=Application", "Terminal=false",
								"Categories=Network;Utility;RemoteAccess;Security;", "Name=" + name,
								"Exec=\"" + executable.toString() + "\" %f"))
						+ "\n");
	}

	public static void installWindowsDesktopShortcut() throws IOException, InterruptedException {
		Path destination = Paths.get(System.getProperty("user.home"), "Desktop");

		Runtime.getRuntime().exec("mklink \"" + destination.toString() + "\"").waitFor();
	}

	public static void installWindowsStartMenuEntry(String coordinate) throws IOException, InterruptedException {
		Path destination = Paths.get("C:/ProgramData/Microsoft/Windows/Start Menu/Programs");
		if (!Files.isWritable(destination)) {
			destination = Paths.get(System.getProperty("user.home"), "AppData/Microsoft/Windows/Start Menu/Programs");
		}
		destination = destination.resolve("Sandpolis/" + coordinate.split(":")[1]);
		Files.createDirectories(destination);

		Runtime.getRuntime().exec("mklink \"" + destination.toString() + "\"").waitFor();
	}

	public static void installLinuxBinaries() throws IOException {
		Path destination = Paths.get("/usr/share/applications");
		if (!Files.isWritable(destination)) {
			destination = Paths.get(System.getProperty("user.home"), ".local/share/applications");
		}

		// TODO
		Files.writeString(destination,
				"#!/bin/bash\n" + "exec /usr/bin/java --module-path " + "" + " -m " + "" + " \"%@\"");
	}

	private InstallUtil() {
	}
}
