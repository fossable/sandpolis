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

import com.sandpolis.core.soi.SoiUtil;
import com.sandpolis.core.util.ArtifactUtil;
import com.sandpolis.installer.util.InstallUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;

import static com.sandpolis.core.util.ArtifactUtil.ParsedCoordinate.fromCoordinate;

/**
 * @author cilki
 * @since 5.1.2
 */
public class CliInstaller implements Callable<Void> {

	private static final Logger log = LoggerFactory.getLogger(CliInstaller.class);

	/**
	 * The installation directory.
	 */
	private Path destination;

	/**
	 * The Sandpolis instance that will be installed.
	 */
	private String coordinate;

	/**
	 * The Sandpolis version that will be installed.
	 */
	private String version = System.getProperty("version");

	/**
	 * The client configuration.
	 */
	protected String config;

	/**
	 * Whether the installation completed successfully.
	 */
	private boolean completed;

	/**
	 * A list of installation extensions.
	 */
	private List<Runnable> extensions;

	protected CliInstaller(Path destination) {
		this.destination = Objects.requireNonNull(destination);
	}

	public static CliInstaller newServerInstaller(Path destination) {
		CliInstaller installer = new CliInstaller(destination);
		installer.coordinate = "com.sandpolis:sandpolis-server-vanilla:";
		return installer;
	}

	public static CliInstaller newViewerJfxInstaller(Path destination) {
		CliInstaller installer = new CliInstaller(destination);
		installer.coordinate = "com.sandpolis:sandpolis-viewer-jfx:";
		return installer;
	}

	public static CliInstaller newViewerCliInstaller(Path destination) {
		CliInstaller installer = new CliInstaller(destination);
		installer.coordinate = "com.sandpolis:sandpolis-viewer-cli:";
		return installer;
	}

	public static CliInstaller newClientInstaller(Path destination, String config) {
		CliInstaller installer = new CliInstaller(destination);
		installer.coordinate = "com.sandpolis:sandpolis-client-mega:";
		installer.config = config;
		return installer;
	}

	@Override
	public Void call() throws Exception {
		log.debug("Executing installation for " + coordinate);

		if (version == null) {
			// Request latest version number
			log.info("Downloading metadata");
			version = ArtifactUtil.getLatestVersion(coordinate);
		}
		coordinate += version;

		// Create directories
		Path lib = destination.resolve("lib");
		Files.createDirectories(lib);

		// Download executable
		log.info("Downloading " + coordinate);
		Path executable = ArtifactUtil.download(lib, coordinate);

		// Calculate dependencies
		Set<String> dependencies = InstallUtil.computeDependencies(SoiUtil.readMatrix(executable), coordinate);

		for (String dep : dependencies) {
			var coordinate = fromCoordinate(dep);
			Path dependency = lib.resolve(coordinate.filename);
			if (!Files.exists(dependency)) {
				InputStream in = CliInstaller.class.getResourceAsStream("/" + coordinate.filename);
				if (in != null) {
					log.info("Extracting " + dep);
					try (in) {
						Files.copy(in, dependency);
					}
				} else {
					log.info("Downloading " + dep);
					ArtifactUtil.download(lib, dep);
				}
			}
		}

		if (Main.IS_LINUX) {
			String desktopEntryDest = System.getProperty("desktop-entry");
			if (desktopEntryDest != null) {
				InstallUtil.installLinuxDesktopEntry(Paths.get(desktopEntryDest), executable, coordinate, "Sandpolis");
			}
		}

		else if (Main.IS_WINDOWS) {
			if (coordinate.contains(":sandpolis-viewer-jfx:")) {
				InstallUtil.installWindowsStartMenuEntry(coordinate);
				InstallUtil.installWindowsDesktopShortcut();
			}
		}

		completed = true;
		return null;
	}

	public boolean isCompleted() {
		return completed;
	}

}
