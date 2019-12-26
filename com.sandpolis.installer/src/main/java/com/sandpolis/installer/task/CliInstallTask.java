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
package com.sandpolis.installer.task;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.installer.InstallComponent;
import com.sandpolis.installer.platform.Installer;

/**
 * @author cilki
 * @since 5.1.2
 */
public class CliInstallTask implements Callable<Void> {

	private static final Logger log = LoggerFactory.getLogger(CliInstallTask.class);

	private Installer installer;

	private CliInstallTask(Installer installer) {
		this.installer = Objects.requireNonNull(installer);
		this.installer.setStatusOutput(log::debug);
	}

	public CliInstallTask(Path destination, InstallComponent component) {
		this(Installer.newPlatformInstaller(destination, component));
	}

	public CliInstallTask(Path destination, InstallComponent component, String config) {
		this(Installer.newPlatformInstaller(destination, component));

		if (component != InstallComponent.CLIENT_MEGA)
			throw new IllegalArgumentException();

		this.installer.setConfig(config);
	}

	@Override
	public Void call() throws Exception {
		installer.run();
		return null;
	}
}
