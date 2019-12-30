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

public class InstallerLinux extends InstallerPosix {

	protected InstallerLinux(Path destination, InstallComponent component) {
		super(destination, component);
	}

	@Override
	protected boolean installAutostart(Path launch, String name) throws Exception {
		var process = InstallUtil.exec("systemctl --version");
		if (!process.waitFor(1000, TimeUnit.MILLISECONDS) || process.exitValue() != 0) {
			return false;
		}

		Path service = Files.createTempFile(null, null);
		Files.writeString(service, String.format(
				"[Unit]\n" + "After=network.target\n\n" + "[Service]\n" + "Type=simple\n" + "Restart=always\n"
						+ "RestartSec=1\n" + "ExecStart=%s\n\n" + "[Install]\n" + "WantedBy=multi-user.target\n",
				launch));

		process = execElevated("mv " + service + " /usr/lib/systemd/system/" + name + ".service");
		if (!process.waitFor(1000, TimeUnit.MILLISECONDS) || process.exitValue() != 0) {
			return false;
		}

		process = execElevated("systemctl enable " + name + ".service");
		if (!process.waitFor(1000, TimeUnit.MILLISECONDS) || process.exitValue() != 0) {
			return false;
		}

		process = execElevated("systemctl start " + name + ".service");
		if (!process.waitFor(1000, TimeUnit.MILLISECONDS) || process.exitValue() != 0) {
			return false;
		}

		return true;
	}
}
