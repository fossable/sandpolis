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
package com.sandpolis.plugin.upgrade.client.mega;

import java.util.List;
import java.util.Optional;

import com.sandpolis.plugin.upgrade.net.MsgUpgrade.Package;

/**
 * A package manager installs "packages" from a remote repository onto the local
 * system.
 *
 * @author cilki
 *
 */
public abstract class PackageManager {

	public boolean detect() {
		return getVersion().isPresent();
	}

	public abstract Optional<String> getVersion();

	public abstract Optional<List<Package>> getInstalledPackages();

	public abstract Optional<List<Package>> getOutdatedPackages();
}
