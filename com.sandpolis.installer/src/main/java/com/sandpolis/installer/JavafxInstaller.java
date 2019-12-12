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
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.sandpolis.core.util.ArtifactUtil.ParsedCoordinate.fromCoordinate;

/**
 * @author cilki
 * @since 5.0.0
 */
public class JavafxInstaller extends Task<Void> {

	private static final Logger log = LoggerFactory.getLogger(JavafxInstaller.class);

	/**
	 * The installation directory.
	 */
	private Path destination;

	/**
	 * The Sandpolis instance that will be installed.
	 */
	private String coordinate;

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

	protected JavafxInstaller(Path destination) {
		this.destination = Objects.requireNonNull(destination);
		updateProgress(0, 1);
	}

	public static JavafxInstaller newServerInstaller(Path destination) {
		JavafxInstaller installer = new JavafxInstaller(destination);
		installer.coordinate = "com.sandpolis:sandpolis-server-vanilla:";
		return installer;
	}

	public static JavafxInstaller newViewerJfxInstaller(Path destination) {
		JavafxInstaller installer = new JavafxInstaller(destination);
		installer.coordinate = "com.sandpolis:sandpolis-viewer-jfx:";
		return installer;
	}

	public static JavafxInstaller newViewerCliInstaller(Path destination) {
		JavafxInstaller installer = new JavafxInstaller(destination);
		installer.coordinate = "com.sandpolis:sandpolis-viewer-cli:";
		return installer;
	}

	public static JavafxInstaller newClientInstaller(Path destination, String config) {
		JavafxInstaller installer = new JavafxInstaller(destination);
		installer.coordinate = "com.sandpolis:sandpolis-client-mega:";
		installer.config = config;
		return installer;
	}

	@Override
	protected Void call() throws Exception {
		log.debug("Executing installation for " + coordinate);

		String version = Main.VERSION;
		if (version == null || version.equalsIgnoreCase("latest")) {
			// Request latest version number
			log.info("Downloading metadata");
			version = ArtifactUtil.getLatestVersion(coordinate);
		}
		coordinate += version;

		// Create directories
		Path lib = destination.resolve("lib");
		Files.createDirectories(lib);

		// Download executable
		updateMessage("Downloading " + coordinate);
		Path executable = ArtifactUtil.download(lib, coordinate);

		// Calculate dependencies
		Set<String> dependencies = InstallUtil.computeDependencies(SoiUtil.readMatrix(executable), coordinate);

		double progress = 0;
		for (String dep : dependencies) {
			var coordinate = fromCoordinate(dep);
			Path dependency = lib.resolve(coordinate.filename);
			if (!Files.exists(dependency)) {
				InputStream in = JavafxInstaller.class.getResourceAsStream("/" + coordinate.filename);
				if (in != null) {
					updateMessage("Extracting " + dep);
					try (in) {
						Files.copy(in, dependency);
					}
				} else {
					updateMessage("Downloading " + dep);
					ArtifactUtil.download(lib, dep);
				}
			}

			progress++;
			updateProgress(progress, dependencies.size());
		}

		if (Main.IS_LINUX) {
			if (coordinate.contains(":sandpolis-viewer-jfx:")) {
				Path bin = InstallUtil.installLinuxBinaries(executable, coordinate);
				InstallUtil.installLinuxDesktopEntry(executable, coordinate, bin, "Sandpolis Viewer");
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
