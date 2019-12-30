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
package com.sandpolis.installer.platform;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import com.sandpolis.installer.InstallComponent;
import com.sandpolis.installer.util.InstallUtil;

public class InstallerMac extends InstallerPosix {

	protected InstallerMac(Path destination, InstallComponent component) {
		super(destination, component);
	}

	@Override
	protected boolean installAutostart(Path launch, String name) throws Exception {
		var process = InstallUtil.exec("launchctl --version");
		if (!process.waitFor(1000, TimeUnit.MILLISECONDS) || process.exitValue() != 0) {
			return false;
		}

		Path service = Files.createTempFile(null, null);
		// TODO finish
		Files.writeString(service, String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
				+ "<plist version=\"1.0\">\n" + "", launch));

		process = execElevated("mv " + service + " /Library/LaunchDaemons/" + name + ".plist");
		if (!process.waitFor(1000, TimeUnit.MILLISECONDS) || process.exitValue() != 0) {
			return false;
		}

		process = execElevated("launchctl load /Library/LaunchDaemons/" + name + ".plist");
		if (!process.waitFor(1000, TimeUnit.MILLISECONDS) || process.exitValue() != 0) {
			return false;
		}

		process = execElevated("launchctl start " + name);
		if (!process.waitFor(1000, TimeUnit.MILLISECONDS) || process.exitValue() != 0) {
			return false;
		}

		return true;
	}

}
