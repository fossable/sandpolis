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
import com.sandpolis.core.util.JarUtil;
import com.sandpolis.installer.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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

	public static Path installLinuxDesktopEntry(Path bin, Path icon, String coordinate, String name)
			throws IOException {
		for (Path destination : Main.EXT_LINUX_DESKTOP.evaluateWritable()) {
			if (!Files.exists(destination))
				continue;
			destination = destination.resolve(coordinate.split(":")[1] + ".desktop");

			Files.writeString(destination,
					String.join("\n",
							List.of("[Desktop Entry]", "Version=1.1", "Type=Application", "Terminal=false",
									"Categories=Network;Utility;RemoteAccess;Security;", "Name=" + name,
									"Icon=\"" + icon + "\"", "Exec=\"" + bin + "\" %f"))
							+ "\n");
			log.debug("Installed desktop entry to: {}", destination);
			return destination;
		}

		return null;
	}

	public static Path installWindowsDesktopShortcut(Path bin, Path icon, String name) throws IOException {
		for (Path destination : Main.EXT_WINDOWS_DESKTOP.evaluateWritable()) {
			if (!Files.exists(destination))
				continue;
			destination = destination.resolve(name + ".lnk");

			ShellLink.createLink(bin.toString()).setName(name).setIconLocation(icon.toString())
					.saveTo(destination.toString());
			log.debug("Installed desktop shortcut to: {}", destination);
			return destination;
		}

		return null;
	}

	public static Path installWindowsStartMenuEntry(Path bin, Path icon, String name) throws IOException {
		for (Path destination : Main.EXT_WINDOWS_START.evaluateWritable()) {
			if (!Files.exists(destination))
				continue;
			destination = destination.resolve(name + ".lnk");

			ShellLink.createLink(bin.toString()).setName(name).setIconLocation(icon.toString())
					.saveTo(destination.toString());
			log.debug("Installed start menu entry to: {}", destination);
			return destination;
		}

		return null;
	}

	public static Path installLinuxBinaries(Path instance, String coordinate) throws IOException {
		for (Path destination : Main.EXT_LINUX_BIN.evaluateWritable()) {
			if (!Files.exists(destination))
				continue;
			destination = destination.resolve(coordinate.split(":")[1]);
			coordinate = coordinate.split(":")[1].replaceAll("-", ".");

			Files.writeString(destination,
					String.format("#!/bin/sh\nexec /usr/bin/java --module-path \"%s\" -m com.%s/com.%s.Main \"%%@\"\n",
							instance.getParent(), coordinate, coordinate));
			Files.setPosixFilePermissions(destination, Set.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE, GROUP_READ,
					GROUP_EXECUTE, OTHERS_READ, OTHERS_EXECUTE));
			log.debug("Installed binaries to: {}", destination);
			return destination;
		}

		return null;
	}

	public static Path installWindowsBinaries(Path instance, String coordinate) throws IOException {
		for (Path destination : Main.EXT_WINDOWS_BIN.evaluateWritable()) {
			if (!Files.exists(destination))
				continue;
			destination = destination.resolve(coordinate.split(":")[1] + ".bat");
			coordinate = coordinate.split(":")[1].replaceAll("-", ".");

			Files.writeString(destination,
					String.format("@echo off%nstart javaw --module-path \"%s\" -m com.%s/com.%s.Main",
							instance.getParent(), coordinate, coordinate));
			log.debug("Installed binaries to: {}", destination);
			return destination;
		}
		return null;
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

	private InstallUtil() {
	}

	/**
	 * Represents a {@link Path} on the system which may receive installation
	 * artifacts.
	 */
	public static final class InstallPath {

		private List<Object> candidates;

		/**
		 * Get the highest precedence {@link Path}.
		 *
		 * @return
		 */
		public Optional<Path> evaluate() {
			return candidates.stream().map(this::convert).filter(Objects::nonNull).findFirst();
		}

		/**
		 * Get a list of path candidates that are writable.
		 *
		 * @return A list of writable path candidates
		 */
		public List<Path> evaluateWritable() {
			return candidates.stream().map(this::convert).filter(Objects::nonNull).filter(this::isWritable)
					.collect(Collectors.toList());
		}

		private Path convert(Object candidate) {
			if (candidate instanceof String)
				candidate = Paths.get((String) candidate);

			if (candidate instanceof InstallPath)
				candidate = ((InstallPath) candidate).evaluate().orElse(null);

			if (candidate instanceof Path)
				return (Path) candidate;

			return null;
		}

		/**
		 * Determine whether a {@link Path} is writable, or if it does not exist, some
		 * parent path is writable.
		 *
		 * @param path The input path
		 * @return Whether the path is writable
		 */
		private boolean isWritable(Path path) {
			if (Files.exists(path))
				return Files.isWritable(path);
			if (path.equals(path.getParent()))
				// Reached the root path
				return false;
			return isWritable(path.getParent());
		}

		private InstallPath() {
		}

		public static InstallPath of(Object... candidates) {
			InstallPath path = new InstallPath();
			path.candidates = Arrays.stream(candidates).filter(Objects::nonNull)
					.collect(Collectors.toUnmodifiableList());
			return path;
		}
	}
}
