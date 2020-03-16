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

import java.util.List;
import java.util.Optional;

import com.sandpolis.plugin.upgrade.client.mega.PackageManager;
import com.sandpolis.plugin.upgrade.net.MsgUpgrade.Package;

/**
 * Integration with Debian's Advanced Package Tool (APT).
 * 
 * @author cilki
 *
 */
public class Apt extends PackageManager {

	@Override
	public Optional<String> getVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Optional<List<Package>> getInstalledPackages() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Optional<List<Package>> getOutdatedPackages() {
		// TODO Auto-generated method stub
		return null;
	}

}
