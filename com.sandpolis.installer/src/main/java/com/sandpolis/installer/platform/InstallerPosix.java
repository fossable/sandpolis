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

import static java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.GROUP_READ;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.installer.InstallComponent;
import com.sandpolis.installer.Main;
import com.sandpolis.installer.util.InstallUtil;

public class InstallerPosix extends Installer {

	private static final Logger log = LoggerFactory.getLogger(InstallerPosix.class);

	protected InstallerPosix(Path destination, InstallComponent component) {
		super(destination, component);
	}

	@Override
	protected Path installIcon() throws Exception {
		return InstallUtil.installIcon(executable, "/image/icon@4x.png",
				executable.getParent().resolveSibling("Sandpolis.png"));
	}

	@Override
	protected void installAutostart() throws Exception {
		Runtime.getRuntime().exec("systemctl enable sandpolisd.service").waitFor();
		Runtime.getRuntime().exec("systemctl start sandpolisd.service").waitFor();
	}

	@Override
	protected Path installLaunchExecutable() throws Exception {
		for (Path destination : Main.EXT_LINUX_BIN.evaluateWritable()) {
			if (!Files.exists(destination))
				continue;
			destination = destination.resolve(component.fileBase);

			Files.writeString(destination,
					String.format("#!/bin/sh\nexec /usr/bin/java --module-path \"%s\" -m com.%s/com.%s.Main \"%%@\"\n",
							executable.getParent(), component.id, component.id));
			Files.setPosixFilePermissions(destination, Set.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE, GROUP_READ,
					GROUP_EXECUTE, OTHERS_READ, OTHERS_EXECUTE));
			log.debug("Installed binaries to: {}", destination);
			return destination;
		}

		throw new RuntimeException();
	}

	@Override
	protected Path installDesktopEntry(Path launch, Path icon, String name) throws Exception {
		for (Path destination : Main.EXT_LINUX_DESKTOP.evaluateWritable()) {
			if (!Files.exists(destination))
				continue;
			destination = destination.resolve(component.fileBase + ".desktop");

			Files.writeString(destination,
					String.join("\n",
							List.of("[Desktop Entry]", "Version=1.1", "Type=Application", "Terminal=false",
									"Categories=Network;Utility;RemoteAccess;Security;", "Name=" + name,
									"Icon=\"" + icon + "\"", "Exec=\"" + launch + "\" %f"))
							+ "\n");
			log.debug("Installed desktop entry to: {}", destination);
			return destination;
		}

		throw new RuntimeException();
	}

	@Override
	protected void execute(Path launch) throws Exception {
		// TODO Auto-generated method stub

	}
}
