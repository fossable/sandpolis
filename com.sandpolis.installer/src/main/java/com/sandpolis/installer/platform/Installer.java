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

import static com.sandpolis.core.util.ArtifactUtil.ParsedCoordinate.fromCoordinate;

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

import com.sandpolis.core.soi.SoiUtil;
import com.sandpolis.core.util.ArtifactUtil;
import com.sandpolis.installer.InstallComponent;
import com.sandpolis.installer.Main;
import com.sandpolis.installer.task.GuiInstallTask;
import com.sandpolis.installer.util.InstallUtil;

public abstract class Installer {

	public static Installer newPlatformInstaller(Path destination, InstallComponent component) {
		if (Main.IS_WINDOWS)
			return new InstallerWindows(destination, component);
		if (Main.IS_LINUX)
			return new InstallerLinux(destination, component);
		if (Main.IS_MAC)
			return new InstallerMac(destination, component);

		throw new RuntimeException("No installer found for platform");
	}

	/**
	 * Whether the installation completed successfully.
	 */
	private boolean completed;

	/**
	 * The component that will be installed.
	 */
	protected final InstallComponent component;

	/**
	 * The client configuration in Base64 (if the installer component is a client).
	 */
	private String config;

	/**
	 * The installation directory.
	 */
	protected final Path destination;

	/**
	 * The installation artifact.
	 */
	protected Path executable;

	/**
	 * The server password.
	 */
	private String password;

	/**
	 * Called with progress updates.
	 */
	private BiConsumer<Long, Long> progress = (s, t) -> {
		// NOP by default
	};

	/**
	 * Called with status updates.
	 */
	private Consumer<String> status = (s) -> {
		// NOP by default
	};

	/**
	 * The server username.
	 */
	private String username;

	protected Installer(Path destination, InstallComponent component) {
		this.destination = Objects.requireNonNull(destination);
		this.component = Objects.requireNonNull(component);
	}

	/**
	 * Execute the installed instance.
	 *
	 * @param launch The launch executable
	 * @return A new process
	 * @throws Exception
	 */
	protected abstract Process exec(Path launch) throws Exception;

	/**
	 * Execute a command with elevated privileges if possible.
	 *
	 * @param cmd The command to execute
	 * @return A new process
	 * @throws Exception
	 */
	protected abstract Process execElevated(String cmd) throws Exception;

	protected abstract boolean installAutostart(Path launch, String name) throws Exception;

	/**
	 * Inject the client configuration into the client executable.
	 *
	 * @throws Exception
	 */
	protected void installClientConfig() throws Exception {
		try (FileSystem zip = FileSystems.newFileSystem(executable, (ClassLoader) null)) {
			try (var out = Files.newOutputStream(zip.getPath("/soi/client.bin"))) {
				new ByteArrayInputStream(Base64.getDecoder().decode(config)).transferTo(out);
			}
		}
	}

	/**
	 * Install a desktop entry.
	 *
	 * @param launch The path to the launch executable
	 * @param icon   The icon path
	 * @param name   The entry name
	 * @return The desktop entry path
	 * @throws Exception
	 */
	protected abstract Path installDesktopEntry(Path launch, Path icon, String name) throws Exception;

	/**
	 * Install a program icon.
	 *
	 * @return The icon path
	 * @throws Exception
	 */
	protected abstract Path installIcon() throws Exception;

	/**
	 * Install an executable that launches the instance.
	 *
	 * @return The launch executable path
	 * @throws Exception
	 */
	protected abstract Path installLaunchExecutable() throws Exception;

	/**
	 * Drop the admin credentials in a plaintext file in the database directory.
	 * This isn't much of a security risk because the credentials will be moved to a
	 * database in the same directory.
	 *
	 * @throws Exception
	 */
	protected void installServerCredentials() throws Exception {
		Path dest = destination.resolve("db");

		if (!Files.exists(dest))
			Files.createDirectories(dest);

		Files.writeString(dest.resolve("install.txt"), String.format("%s%n%s%n", username, password));
	}

	public boolean isCompleted() {
		return completed;
	}

	public void run() throws Exception {
		status.accept("Executing installation for " + component.name());

		String version = Main.VERSION;
		if (version == null || version.equalsIgnoreCase("latest")) {
			// Request latest version number
			status.accept("Downloading metadata");
			version = ArtifactUtil.getLatestVersion(component.coordinate);
		}
		String coordinate = component.coordinate + version;

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
			var gav = fromCoordinate(dep);
			Path dependency = lib.resolve(gav.filename);
			if (!Files.exists(dependency)) {
				InputStream in = GuiInstallTask.class.getResourceAsStream("/" + gav.filename);
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

		// Run installation extensions
		if (component == InstallComponent.SERVER_VANILLA) {
			Path launch = installLaunchExecutable();
			installServerCredentials();
			if (!installAutostart(launch, "sandpolis-server")) {
				exec(launch);
			}
		} else if (component == InstallComponent.VIEWER_LIFEGEM) {
			Path launch = installLaunchExecutable();
			Path icon = installIcon();
			installDesktopEntry(launch, icon, "Sandpolis Viewer");
		} else if (component == InstallComponent.CLIENT_MEGA) {
			installClientConfig();
			Path launch = installLaunchExecutable();
			if (!installAutostart(launch, "sandpolis-client")) {
				exec(launch);
			}
		}

		completed = true;
	}

	public void setConfig(String config) {
		this.config = config;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setProgressOutput(BiConsumer<Long, Long> progress) {
		this.progress = progress;
	}

	public void setStatusOutput(Consumer<String> status) {
		this.status = status;
	}

	public void setUsername(String username) {
		this.username = username;
	}
}
