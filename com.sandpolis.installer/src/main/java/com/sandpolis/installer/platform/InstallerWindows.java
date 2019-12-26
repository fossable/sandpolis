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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.installer.InstallComponent;
import com.sandpolis.installer.Main;
import com.sandpolis.installer.util.InstallUtil;

import mslinks.ShellLink;

public class InstallerWindows extends Installer {

	private static final Logger log = LoggerFactory.getLogger(InstallerWindows.class);

	protected InstallerWindows(Path destination, InstallComponent component) {
		super(destination, component);
	}

	@Override
	protected Path installLaunchExecutable() throws Exception {
		for (Path destination : Main.EXT_WINDOWS_BIN.evaluateWritable()) {
			if (!Files.exists(destination))
				continue;
			destination = destination.resolve(component.fileBase + ".bat");

			Files.writeString(destination, String.format("@echo off%nstart javaw --module-path \"%s\" -m %s/%s.Main",
					executable.getParent(), component.id, component.id));
			log.debug("Installed binaries to: {}", destination);
			return destination;
		}

		throw new RuntimeException();
	}

	@Override
	protected Path installIcon() throws Exception {
		return InstallUtil.installIcon(executable, "/image/icon.ico",
				executable.getParent().resolveSibling("Sandpolis.ico"));
	}

	@Override
	protected Path installDesktopEntry(Path launch, Path icon, String name) throws Exception {
		for (Path destination : Main.EXT_WINDOWS_DESKTOP.evaluateWritable()) {
			if (!Files.exists(destination))
				continue;
			destination = destination.resolve(name + ".lnk");

			ShellLink.createLink(launch.toString()).setName(name).setIconLocation(icon.toString())
					.saveTo(destination.toString());
			log.debug("Installed desktop shortcut to: {}", destination);
			return destination;
		}

		throw new RuntimeException();
	}

	protected void installStartEntry(Path launch, Path icon, String name) throws Exception {
		for (Path destination : Main.EXT_WINDOWS_START.evaluateWritable()) {
			if (!Files.exists(destination))
				continue;
			destination = destination.resolve(name + ".lnk");

			ShellLink.createLink(launch.toString()).setName(name).setIconLocation(icon.toString())
					.saveTo(destination.toString());
			log.debug("Installed start menu entry to: {}", destination);
		}
	}

	@Override
	protected boolean installAutostart(Path launch, String name) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected Process exec(Path launch) throws Exception {
		log.debug("Executing launch executable: {}", launch);
		return Runtime.getRuntime().exec(new String[] { "start", launch.toString() });
	}

	@Override
	protected Process execElevated(String cmd) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
}
