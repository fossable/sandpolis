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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * @author cilki
 * @since 5.1.2
 */
public class CliInstaller implements Callable<Void> {

	private static final Logger log = LoggerFactory.getLogger(CliInstaller.class);

	private Installer installer;

	protected CliInstaller(Installer installer) {
		this.installer = Objects.requireNonNull(installer);
		this.installer.status = log::debug;
	}

	public static CliInstaller newServerInstaller(Path destination) {
		Installer installer = new Installer(destination, "com.sandpolis:sandpolis-server-vanilla:");
		return new CliInstaller(installer);
	}

	public static CliInstaller newViewerJfxInstaller(Path destination) {
		Installer installer = new Installer(destination, "com.sandpolis:sandpolis-viewer-jfx:");
		return new CliInstaller(installer);
	}

	public static CliInstaller newViewerCliInstaller(Path destination) {
		Installer installer = new Installer(destination, "com.sandpolis:sandpolis-viewer-cli:");
		return new CliInstaller(installer);
	}

	public static CliInstaller newClientInstaller(Path destination, String config) {
		Installer installer = new Installer(destination, "com.sandpolis:sandpolis-client-mega:");
		installer.config = config;
		return new CliInstaller(installer);
	}

	@Override
	public Void call() throws Exception {
		installer.run();
		return null;
	}
}
