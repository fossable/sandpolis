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

import com.sandpolis.installer.InstallComponent;
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

	public GuiInstallTask(Path destination, InstallComponent component) {
		this(Installer.newPlatformInstaller(destination, component));
	}

	public GuiInstallTask(Path destination, InstallComponent component, String config) {
		this(Installer.newPlatformInstaller(destination, component));

		if (component != InstallComponent.CLIENT_MEGA)
			throw new IllegalArgumentException();

		this.installer.setConfig(config);
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
