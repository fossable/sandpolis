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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.util.SystemUtil;
import com.sandpolis.plugin.upgrade.client.mega.PackageManager;
import com.sandpolis.plugin.upgrade.net.MsgUpgrade.Package;

/**
 * Integration with Arch Linux's Pacman package management utility.
 * 
 * @author cilki
 *
 */
public class Pacman extends PackageManager {

	private static final Logger log = LoggerFactory.getLogger(Pacman.class);

	@Override
	public Optional<String> getVersion() {
		return SystemUtil.exec(1000, TimeUnit.MILLISECONDS, "pacman", "-V").map(output -> {
			return Pattern.compile("Pacman v(.*) -").matcher(output).group(1);
		});
	}

	@Override
	public Optional<List<Package>> getInstalledPackages() {

		return SystemUtil.exec(1000, TimeUnit.MILLISECONDS, "pacman", "-Q").map(output -> {
			List<Package> packages = new ArrayList<>();
			for (String pkg : output.split("\n")) {
				String[] l = pkg.split(" ");
				packages.add(Package.newBuilder().setName(l[0]).setVersion(l[1]).build());
			}
			return packages;
		});
	}

	@Override
	public Optional<List<Package>> getOutdatedPackages() {

		return SystemUtil.exec(1000, TimeUnit.MILLISECONDS, "pacman", "-Suq", "--print-format", "\"%n %v %s %l %r\"")
				.map(output -> {
					List<Package> packages = new ArrayList<>();
					for (String pkg : output.split("\n")) {
						String[] l = pkg.split(" ");
						packages.add(Package.newBuilder().setName(l[0]).setVersion(l[1]).setSize(Long.parseLong(l[2]))
								.setRemoteLocation(l[3]).setRepository(l[4]).build());
					}
					return packages;
				});
	}

	public Optional<Package> getPackageFiles(String name) {

		return SystemUtil.exec(1000, TimeUnit.MILLISECONDS, "pacman", "-Ql", name).map(output -> {
			Package.Builder pkg = Package.newBuilder().setName(name);
			for (String file : output.split("\n")) {
				pkg.addFile(file.split(" ")[1]);
			}
			return pkg.build();
		});
	}

	public void upgradePackages(List<Package> packages) {
		SystemUtil.execp("pacman", "-S", "--noconfirm",
				packages.stream().map(Package::getName).collect(Collectors.joining(" ")));
	}

	public void refreshPackages() {
		SystemUtil.execp("pacman", "-Sy");
	}
}
