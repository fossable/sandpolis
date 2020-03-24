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
package com.sandpolis.plugin.upgrade.client.mega.library;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.util.SystemUtil;
import com.sandpolis.core.util.TextUtil;
import com.sandpolis.plugin.upgrade.MsgUpgrade.Package;
import com.sandpolis.plugin.upgrade.client.mega.PackageManager;

/**
 * Integration with Arch Linux's Pacman package management utility.
 *
 * @author cilki
 *
 */
public class Pacman extends PackageManager {

	private static final Logger log = LoggerFactory.getLogger(Pacman.class);

	@Override
	public String getManagerVersion() throws Exception {
		final var versionRegex = Pattern.compile("Pacman v(.*) -");

		return SystemUtil.exec("pacman", "-V").lines()
				// Pull version out of the output
				.map(versionRegex::matcher).filter(Matcher::matches).findFirst().get().group(1);
	}

	@Override
	public Path getManagerLocation() throws Exception {
		return Paths.get(SystemUtil.exec("which", "pacman").string());
	}

	@Override
	public void clean() throws Exception {
		log.debug("Cleaning package cache");

		SystemUtil.execp("pacman", "-Sc", "--noconfirm").complete(80);
	}

	@Override
	public List<Package> getInstalled() throws Exception {
		log.debug("Querying for local packages");

		return SystemUtil.exec("pacman", "-Q").lines()
				// Each line is a package
				.map(line -> line.split("\\s+"))
				// Protect against invalid lines
				.filter(pkg -> pkg.length == 2)
				// Build new Package object
				.map(pkg -> Package.newBuilder().setName(pkg[0]).setVersion(pkg[1]).build())
				// Collect into list
				.collect(Collectors.toList());
	}

	@Override
	public Package getMetadata(String name) throws Exception {
		Package.Builder p = Package.newBuilder().setName(name);

		SystemUtil.exec("pacman", "-Ql", name).lines()
				// Each line is a file or directory
				.map(line -> line.split("\\s+"))
				// Protect against any invalid lines
				.filter(pkg -> pkg.length == 2).filter(pkg -> pkg[0].equals(name))
				// Append file
				.forEach(pkg -> p.addFile(pkg[1]));

		SystemUtil.exec("pacman", "-Qi", name).lines()
				// Each line is a property
				.map(line -> line.split(":"))
				// Protect against invalid lines
				.filter(a -> a.length == 2)
				// Gather metadata
				.forEach(a -> {
					switch (a[0].trim()) {
					case "Description" -> p.setDescription(a[1]);
					case "Architecture" -> p.setArchitecture(a[1]);
					case "URL" -> p.setUpstreamUrl(a[1]);
					case "Licenses" -> Arrays.stream(a[1].split("\\s+")).forEach(p::addLicense);
					case "Depends On" -> Arrays.stream(a[1].split("\\s+")).forEach(p::addDependency);
					case "Installed Size" -> p.setLocalSize(TextUtil.unformatByteCount(a[1]));
					case "Install Reason" -> p.setExplicit("Explicitly installed".equals(a[1]));
					}
				});

		return p.build();
	}

	@Override
	public List<Package> getOutdated() throws Exception {
		log.debug("Querying for outdated packages");

		return SystemUtil.exec("pacman", "-Suq", "--print-format", "%n %v %s %l %r").lines()
				// Each line is a package
				.map(line -> line.split("\\s+"))
				// Protect against invalid lines
				.filter(pkg -> pkg.length == 5)
				// Build new Package object
				.map(pkg -> Package.newBuilder().setName(pkg[0]).setVersion(pkg[1])
						.setRemoteSize(Long.parseLong(pkg[2])).setRemoteLocation(pkg[3]).setRepository(pkg[4]).build())
				// Collect into list
				.collect(Collectors.toList());
	}

	@Override
	public void install(List<String> packages) throws Exception {
		log.debug("Installing {} packages", packages.size());

		SystemUtil.execp("pacman", "-S", "--noconfirm", packages.stream().collect(Collectors.joining(" ")))
				.complete(600);
	}

	@Override
	public void refresh() throws Exception {
		log.debug("Refreshing package database");

		SystemUtil.execp("pacman", "-Sy").complete(80);
	}

	@Override
	public void remove(List<String> packages) throws Exception {
		log.debug("Removing {} packages", packages.size());

		SystemUtil.execp("pacman", "-R", "--noconfirm", packages.stream().collect(Collectors.joining(" ")))
				.complete(600);
	}

	@Override
	public void upgrade(List<String> packages) throws Exception {
		log.debug("Upgrading {} packages", packages.size());

		SystemUtil.execp("pacman", "-S", "--noconfirm", packages.stream().collect(Collectors.joining(" ")))
				.complete(600);
	}
}
