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
import java.util.List;
import java.util.stream.Collectors;

import com.sandpolis.core.util.SystemUtil;
import com.sandpolis.plugin.upgrade.MsgUpgrade.Package;
import com.sandpolis.plugin.upgrade.client.mega.PackageManager;

/**
 * Integration with Debian's Advanced Package Tool (APT).
 *
 * @author cilki
 *
 */
public class Apt extends PackageManager {

	@Override
	public Path getManagerLocation() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getManagerVersion() throws Exception {
		return SystemUtil.exec("apt", "--version").string();
	}

	@Override
	public void clean() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Package> getInstalled() throws Exception {
		return SystemUtil.exec("apt", "list", "--installed").lines()
				// Each line is a package
				.map(line -> line.split("\\s+"))
				// Protect against any invalid lines
				.filter(pkg -> pkg.length >= 3)
				// Build new Package object
				.map(pkg -> Package.newBuilder().setName(pkg[0].substring(0, pkg[0].indexOf('/'))).setVersion(pkg[1])
						.setArchitecture(pkg[2]).build())
				.collect(Collectors.toList());
	}

	@Override
	public Package getMetadata(String name) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Package> getOutdated() throws Exception {
		return SystemUtil.exec("apt", "list", "--upgradable").lines()
				// Each line is a package
				.map(line -> line.split("\\s+"))
				// Protect against any invalid lines
				.filter(pkg -> pkg.length >= 3)
				// Build new Package object
				.map(pkg -> Package.newBuilder().setName(pkg[0].substring(0, pkg[0].indexOf('/'))).setVersion(pkg[1])
						.setArchitecture(pkg[2]).build())
				.collect(Collectors.toList());
	}

	@Override
	public void install(List<String> packages) throws Exception {
		SystemUtil.execp("apt", "-y", "install", packages.stream().collect(Collectors.joining(" ")));
	}

	@Override
	public void refresh() throws Exception {
		SystemUtil.execp("apt", "update");
	}

	@Override
	public void remove(List<String> packages) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void upgrade(List<String> packages) throws Exception {
		SystemUtil.execp("apt", "-y", "upgrade", packages.stream().collect(Collectors.joining(" ")));
	}

}
