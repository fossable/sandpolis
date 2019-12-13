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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import mslinks.ShellLink;

import static java.nio.file.attribute.PosixFilePermission.*;

/**
 * @author cilki
 * @since 5.1.2
 */
public final class InstallUtil {

	private static final Logger log = LoggerFactory.getLogger(InstallUtil.class);

	public static Set<String> computeDependencies(Dependency.SO_DependencyMatrix matrix, String coordinate) {
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
	private static void computeDependencies(Dependency.SO_DependencyMatrix matrix, Set<String> dependencies,
			String coordinate) {
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

	public static Path installLinuxDesktopEntry(Path executable, String coordinate, Path bin, String name)
			throws IOException {
		for (String dest : Main.EXT_LINUX_DESKTOP.split(";")) {
			Path destination = Paths.get(dest);
			if (Files.exists(destination) && !Files.isWritable(destination))
				continue;
			try {
				Files.createDirectories(destination);
			} catch (IOException e) {
				continue;
			}

			destination = destination.resolve(coordinate.split(":")[1] + ".desktop");

			if (Files.exists(destination))
				Files.delete(destination);
			Files.writeString(destination,
					String.join("\n",
							List.of("[Desktop Entry]", "Version=1.1", "Type=Application", "Terminal=false",
									"Categories=Network;Utility;RemoteAccess;Security;", "Name=" + name,
									"Exec=\"" + bin + "\" %f"))
							+ "\n");
			log.debug("Installed desktop entry to: {}", destination);
			return destination;
		}

		return null;
	}

	public static Path installWindowsDesktopShortcut(Path executable, String coordinate) throws IOException, InterruptedException {
		for (String dest : Main.EXT_WINDOWS_DESKTOP.split(";")) {
			Path destination = Paths.get(dest);
			if (Files.exists(destination) && !Files.isWritable(destination))
				continue;
			destination = destination.resolve(coordinate.split(":")[1]);

			ShellLink.createLink(executable.toString()).saveTo(destination.toString());
			log.debug("Installed desktop shortcut to: {}", destination);
			return destination;
		}

		return null;
	}

	public static Path installWindowsStartMenuEntry(Path executable, String coordinate) throws IOException, InterruptedException {
		for (String dest : Main.EXT_WINDOWS_START.split(";")) {
			Path destination = Paths.get(dest);
			if (!Files.isWritable(destination))
				continue;
			destination = destination.resolve(coordinate.split(":")[1]);

			ShellLink.createLink(executable.toString()).saveTo(destination.toString());
			log.debug("Installed start menu entry to: {}", destination);
			return destination;
		}

		return null;
	}

	public static Path installLinuxBinaries(Path executable, String coordinate) throws IOException {
		for (String dest : Main.EXT_LINUX_BIN.split(";")) {
			Path destination = Paths.get(dest);
			if (!Files.isWritable(destination))
				continue;
			destination = destination.resolve(coordinate.split(":")[1]);
			coordinate = coordinate.split(":")[1].replaceAll("-", ".");

			Files.writeString(destination,
					String.format("#!/bin/sh\nexec /usr/bin/java --module-path \"%s\" -m com.%s/com.%s.Main \"%%@\"",
							executable.getParent(), coordinate, coordinate));
			Files.setPosixFilePermissions(destination, Set.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE, GROUP_READ,
					GROUP_EXECUTE, OTHERS_READ, OTHERS_EXECUTE));
			log.debug("Installed binaries to: {}", destination);
			return destination;
		}

		return null;
	}

	private InstallUtil() {
	}
}
