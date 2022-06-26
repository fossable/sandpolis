//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.integration.apt;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.s7s.core.foundation.S7SProcess;

/**
 * Integration with Debian's Advanced Package Tool (APT).
 */
public record Apt(Path executable) {

	public String getVersion() {
		return S7SProcess.exec(executable.toString(), "--version").stdout();
	}

	public void clean() throws Exception {
		// TODO Auto-generated method stub

	}

	public List<String> getInstalled() throws Exception {
		return S7SProcess.exec("apt", "list", "--installed").stdoutLines()
				// Each line is a package
				.map(line -> line.split("\\s+"))
				// Protect against any invalid lines
				.filter(pkg -> pkg.length >= 3)
				// Get package name
				.map(pkg -> pkg[0].substring(0, pkg[0].indexOf('/')))
				// Create list
				.collect(Collectors.toList());
	}

	public Package getMetadata(String name) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public List<String> getOutdated() throws Exception {
		return S7SProcess.exec("apt", "list", "--upgradable").stdoutLines()
				// Each line is a package
				.map(line -> line.split("\\s+"))
				// Protect against any invalid lines
				.filter(pkg -> pkg.length >= 3)
				// Get package name
				.map(pkg -> pkg[0].substring(0, pkg[0].indexOf('/')))
				// Create list
				.collect(Collectors.toList());
	}

	public S7SProcess install(String... packages) {
		return S7SProcess.exec("apt", "-y", "install", Arrays.stream(packages).collect(Collectors.joining(" ")));
	}

	public S7SProcess refresh() {
		return S7SProcess.exec("apt", "update");
	}

	public void remove(String... packages) throws Exception {
		// TODO Auto-generated method stub

	}

	public S7SProcess upgrade(String... packages) {
		return S7SProcess.exec("apt", "-y", "upgrade", Arrays.stream(packages).collect(Collectors.joining(" ")));
	}

}
