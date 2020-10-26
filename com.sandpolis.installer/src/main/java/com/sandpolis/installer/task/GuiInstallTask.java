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
import static com.sandpolis.installer.InstallComponent.CLIENT_ASCETIC;
import static com.sandpolis.installer.InstallComponent.CLIENT_LIFEGEM;

import java.nio.file.Path;
import java.util.Objects;

import com.sandpolis.installer.platform.Installer;

import javafx.concurrent.Task;

/**
 * @author cilki
 * @since 5.0.0
 */
public class GuiInstallTask extends Task<Void> {

	private Installer installer;

	private GuiInstallTask(Installer installer) {
		this.installer = Objects.requireNonNull(installer);
		this.installer.setStatusOutput(this::updateMessage);
		this.installer.setProgressOutput(this::updateProgress);

		updateProgress(0, 1);
	}

	public static GuiInstallTask newServerTask(Path destination, String username, String password) {
		var task = new GuiInstallTask(Installer.newPlatformInstaller(destination, SERVER_VANILLA));
		task.installer.setUsername(username);
		task.installer.setPassword(password);

		return task;
	}

	public static GuiInstallTask newClientTask(Path destination, String config) {
		var task = new GuiInstallTask(Installer.newPlatformInstaller(destination, AGENT_VANILLA));
		task.installer.setConfig(config);

		return task;
	}

	public static GuiInstallTask newClientLifegemTask(Path destination) {
		return new GuiInstallTask(Installer.newPlatformInstaller(destination, CLIENT_LIFEGEM));
	}

	public static GuiInstallTask newClientAsceticTask(Path destination) {
		return new GuiInstallTask(Installer.newPlatformInstaller(destination, CLIENT_ASCETIC));
	}

	@Override
	protected Void call() throws Exception {
		installer.run();
		return null;
	}

	public boolean isCompleted() {
		return installer.isCompleted();
	}
}
