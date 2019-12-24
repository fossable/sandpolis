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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.sandpolis.core.util.ArtifactUtil.ParsedCoordinate.fromCoordinate;

final class Installer {

	public interface InstallExtension {
		void run() throws Exception;
	}

	/**
	 * The installation directory.
	 */
	Path destination;

	/**
	 * The installation artifact.
	 */
	Path executable;

	/**
	 * The coordinates of the instance that will be installed.
	 */
	String coordinate;

	/**
	 * The client configuration.
	 */
	String config;

	/**
	 * Whether the installation completed successfully.
	 */
	boolean completed;

	/**
	 * Called with status updates.
	 */
	Consumer<String> status = (s) -> {
		// NOP by default
	};

	/**
	 * Called with progress updates.
	 */
	BiConsumer<Long, Long> progress = (s, t) -> {
		// NOP by default
	};

	Installer(Path destination, String coordinate) {
		this.destination = Objects.requireNonNull(destination);
		this.coordinate = Objects.requireNonNull(coordinate);
	}

	public void run() throws Exception {
		status.accept("Executing installation for " + coordinate);

		String version = Main.VERSION;
		if (version == null || version.equalsIgnoreCase("latest")) {
			// Request latest version number
			status.accept("Downloading metadata");
			version = ArtifactUtil.getLatestVersion(coordinate);
		}
		coordinate += version;

		// Create directories
		Path lib = destination.resolve("lib");
		Files.createDirectories(lib);

		// Download executable
		status.accept("Downloading " + coordinate);
		executable = ArtifactUtil.download(lib, coordinate);

		// Calculate dependencies
		Set<String> dependencies = InstallUtil.computeDependencies(SoiUtil.readMatrix(executable), coordinate);

		long current = 0;
		for (String dep : dependencies) {
			var coordinate = fromCoordinate(dep);
			Path dependency = lib.resolve(coordinate.filename);
			if (!Files.exists(dependency)) {
				InputStream in = JavafxInstaller.class.getResourceAsStream("/" + coordinate.filename);
				if (in != null) {
					status.accept("Extracting " + dep);
					try (in) {
						Files.copy(in, dependency);
					}
				} else {
					status.accept("Downloading " + dep);
					ArtifactUtil.download(lib, dep);
				}
			}

			current++;
			progress.accept(current, (long) dependencies.size());
		}

		// Run extensions
		if (coordinate.contains(":sandpolis-viewer-jfx")) {
			if (Main.IS_WINDOWS) {
				VIEWER_DESKTOP_WINDOWS.run();
			} else if (Main.IS_LINUX) {
				VIEWER_DESKTOP_LINUX.run();
			}
		} else if (coordinate.contains(":sandpolis-client-mega:")) {
			CLIENT_INJECT.run();
			CLIENT_EXECUTE.run();
		}

		completed = true;
	}

	private InstallExtension SERVER_EXECUTE_LINUX = () -> {
		// Install systemd unit
		// TODO

		Runtime.getRuntime().exec("systemctl enable sandpolisd.service").waitFor();
		Runtime.getRuntime().exec("systemctl start sandpolisd.service").waitFor();
	};

	private InstallExtension VIEWER_DESKTOP_LINUX = () -> {
		Path bin = InstallUtil.installLinuxBinaries(executable, coordinate);
		Path icon = InstallUtil.installIcon(executable, "/image/icon@4x.png",
				executable.getParent().resolveSibling("Sandpolis.png"));
		InstallUtil.installLinuxDesktopEntry(bin, icon, coordinate, "Sandpolis Viewer");
	};

	private InstallExtension VIEWER_DESKTOP_WINDOWS = () -> {
		Path bin = InstallUtil.installWindowsBinaries(executable, coordinate);
		Path icon = InstallUtil.installIcon(executable, "/image/icon.ico",
				executable.getParent().resolveSibling("Sandpolis.ico"));
		InstallUtil.installWindowsStartMenuEntry(bin, icon, "Sandpolis Viewer");
		InstallUtil.installWindowsDesktopShortcut(bin, icon, "Sandpolis Viewer");
	};

	/**
	 * Inject the client configuration into the client executable.
	 */
	private InstallExtension CLIENT_INJECT = () -> {
		try (FileSystem zip = FileSystems.newFileSystem(executable, (ClassLoader) null)) {
			try (var out = Files.newOutputStream(zip.getPath("/soi/client.bin"))) {
				new ByteArrayInputStream(Base64.getDecoder().decode(config)).transferTo(out);
			}
		}
	};

	/**
	 * Execute the client instance.
	 */
	private InstallExtension CLIENT_EXECUTE = () -> {
		// Start client
		// TODO
	};
}
