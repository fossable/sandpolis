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
package com.sandpolis.installer;

import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Objects;

/**
 * @author cilki
 * @since 5.0.0
 */
public class JavafxInstaller extends Task<Void> {

	private static final Logger log = LoggerFactory.getLogger(JavafxInstaller.class);

	private Installer installer;

	protected JavafxInstaller(Installer installer) {
		updateProgress(0, 1);
		this.installer = Objects.requireNonNull(installer);
		this.installer.status = this::updateMessage;
		this.installer.progress = this::updateProgress;
	}

	public static JavafxInstaller newServerInstaller(Path destination) {
		Installer installer = new Installer(destination, "com.sandpolis:sandpolis-server-vanilla:");
		return new JavafxInstaller(installer);
	}

	public static JavafxInstaller newViewerJfxInstaller(Path destination) {
		Installer installer = new Installer(destination, "com.sandpolis:sandpolis-viewer-jfx:");
		return new JavafxInstaller(installer);
	}

	public static JavafxInstaller newViewerCliInstaller(Path destination) {
		Installer installer = new Installer(destination, "com.sandpolis:sandpolis-viewer-cli:");
		return new JavafxInstaller(installer);
	}

	public static JavafxInstaller newClientInstaller(Path destination, String config) {
		Installer installer = new Installer(destination, "com.sandpolis:sandpolis-client-mega:");
		installer.config = config;
		return new JavafxInstaller(installer);
	}

	@Override
	protected Void call() throws Exception {
		installer.run();
		return null;
	}

	public boolean isCompleted() {
		return installer.completed;
	}
}
