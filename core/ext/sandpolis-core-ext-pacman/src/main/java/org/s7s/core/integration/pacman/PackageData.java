//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.integration.pacman;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
