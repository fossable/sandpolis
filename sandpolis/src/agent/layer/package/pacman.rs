//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.integration.pacman;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.s7s.core.foundation.S7SFile;
import org.s7s.core.foundation.S7SProcess;
import org.s7s.core.foundation.S7SSystem;

/**
 * Integration with Arch Linux's Pacman package management utility.
 *
 * @author cilki
 *
 */
public record Pacman(Path executable) {

	private static final Logger log = LoggerFactory.getLogger(Pacman.class);

	public static boolean isAvailable() {
		switch (S7SSystem.OS_TYPE) {
		case LINUX:
			return S7SFile.which("pacman").isPresent();
		default:
			return false;
		}
	}

	public static Pacman load() {
		if (!isAvailable()) {
			throw new IllegalStateException();
		}

		return new Pacman(S7SFile.which("pacman").get().path());
	}

	public String getManagerVersion() {
		final var versionRegex = Pattern.compile("Pacman v(.*) -");

		return S7SProcess.exec("pacman", "-V").stdoutLines()
				// Pull version out of the output
				.map(versionRegex::matcher).filter(Matcher::matches).findFirst().get().group(1);
	}

	public S7SProcess clean() {
		log.debug("Cleaning package cache");

		return S7SProcess.exec("pacman", "-Sc", "--noconfirm");
	}

	public List<String> getOutdated() {
		log.debug("Querying for outdated packages");

		return S7SProcess.exec("pacman", "-Suq", "--print-format", "%n").stdoutLines().collect(Collectors.toList());
	}

	public S7SProcess install(String... packages) {
		log.debug("Installing {} packages", packages.length);

		return S7SProcess.exec("pacman", "-S", "--noconfirm", Arrays.stream(packages).collect(Collectors.joining(" ")));
	}

	public S7SProcess refresh() throws Exception {
		log.debug("Refreshing package database");

		return S7SProcess.exec("pacman", "-Sy");
	}

	public S7SProcess remove(String... packages) {
		log.debug("Removing {} packages", packages.length);

		return S7SProcess.exec("pacman", "-R", "--noconfirm", Arrays.stream(packages).collect(Collectors.joining(" ")));
	}

	public S7SProcess upgrade(String... packages) {
		log.debug("Upgrading {} packages", packages.length);

		return S7SProcess.exec("pacman", "-S", "--noconfirm", Arrays.stream(packages).collect(Collectors.joining(" ")));
	}
}

public record PackageData( //
		String name, //
		String version, //
		String base, //
		String desc, //
		String url, //
		String arch, //
		long builddate, //
		long installdate, //
		String packager, //
		long size, //
		String license, //
		String validation, //
		List<String> depends, //
		Map<String, String> optdepends, //
		List<String> files) {

	public static PackageData of(Path directory) throws IOException {

		int index;

		String name;
		String version;
		String base;
		String desc;
		String url;
		String arch;
		long builddate;
		long installdate;
		String packager;
		long size;
		String license;
		String validation;
		List<String> depends = new ArrayList<>();
		Map<String, String> optdepends = new HashMap<>();
		List<String> files = new ArrayList<>();

		// Read "desc"
		var descLines = Files.readAllLines(directory.resolve("desc"));

		index = descLines.indexOf("%NAME%");
		name = descLines.get(index + 1);

		index = descLines.indexOf("%VERSION%");
		version = descLines.get(index + 1);

		index = descLines.indexOf("%BASE%");
		base = descLines.get(index + 1);

		index = descLines.indexOf("%DESC%");
		desc = descLines.get(index + 1);

		index = descLines.indexOf("%URL%");
		url = descLines.get(index + 1);

		index = descLines.indexOf("%ARCH%");
		arch = descLines.get(index + 1);

		index = descLines.indexOf("%BUILDDATE%");
		builddate = Long.parseLong(descLines.get(index + 1));

		index = descLines.indexOf("%INSTALLDATE%");
		installdate = Long.parseLong(descLines.get(index + 1));

		index = descLines.indexOf("%PACKAGER%");
		packager = descLines.get(index + 1);

		index = descLines.indexOf("%SIZE%");
		size = Long.parseLong(descLines.get(index + 1));

		index = descLines.indexOf("%LICENSE%");
		license = descLines.get(index + 1);

		index = descLines.indexOf("%VALIDATION%");
		validation = descLines.get(index + 1);

		index = descLines.indexOf("%DEPENDS%");
		while (!descLines.get(++index).isBlank()) {
			depends.add(descLines.get(index));
		}

		index = descLines.indexOf("%OPTDEPENDS%");
		while (!descLines.get(++index).isBlank()) {
			var components = descLines.get(index).split(":");
			optdepends.put(components[0], components[1]);
		}

		// Read "files"
		var filesLines = Files.readAllLines(directory.resolve("files"));
		index = filesLines.indexOf("%FILES%");
		while (!filesLines.get(++index).isBlank()) {
			// Ignore directories
			if (!filesLines.get(index).endsWith("/")) {
				files.add(filesLines.get(index));
			}
		}

		return new PackageData(name, version, base, desc, url, arch, builddate, installdate, packager, size, license,
				validation, Collections.unmodifiableList(depends), Collections.unmodifiableMap(optdepends),
				Collections.unmodifiableList(files));
	}
}
