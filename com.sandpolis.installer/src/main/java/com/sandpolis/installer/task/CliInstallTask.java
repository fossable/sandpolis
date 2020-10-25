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

import static com.sandpolis.installer.InstallComponent.AGENT_VANILLA;
import static com.sandpolis.installer.InstallComponent.SERVER_VANILLA;
import static com.sandpolis.installer.InstallComponent.VIEWER_ASCETIC;
import static com.sandpolis.installer.InstallComponent.VIEWER_LIFEGEM;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	public static CliInstallTask newServerTask(Path destination, String username, String password) {
		var task = new CliInstallTask(Installer.newPlatformInstaller(destination, SERVER_VANILLA));
		task.installer.setUsername(username);
		task.installer.setPassword(password);

		return task;
	}

	public static CliInstallTask newClientTask(Path destination, String config) {
		var task = new CliInstallTask(Installer.newPlatformInstaller(destination, AGENT_VANILLA));
		task.installer.setConfig(config);

		return task;
	}

	public static CliInstallTask newViewerLifegemTask(Path destination) {
		return new CliInstallTask(Installer.newPlatformInstaller(destination, VIEWER_LIFEGEM));
	}

	public static CliInstallTask newViewerAsceticTask(Path destination) {
		return new CliInstallTask(Installer.newPlatformInstaller(destination, VIEWER_ASCETIC));
	}

	@Override
	public Void call() throws Exception {
		installer.run();
		return null;
	}
}
