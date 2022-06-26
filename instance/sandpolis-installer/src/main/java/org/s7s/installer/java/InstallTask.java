//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.installer.java;

import static java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.GROUP_READ;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.s7s.core.foundation.S7SJarFile;
import org.s7s.core.foundation.S7SMavenArtifact;
import org.s7s.core.foundation.S7SSystem;
import org.s7s.core.integration.systemd.Systemctl;
import org.s7s.core.integration.systemd.SystemdService;

import javafx.concurrent.Task;

public class InstallTask extends Task<Void> {

	private static final Path LINUX_DESKTOP_DIR_SYSTEM = Paths.get("/usr/share/applications");

	private static final Path LINUX_DESKTOP_DIR_USER = Paths
			.get(System.getProperty("user.home") + "/.local/share/applications");

	private static final Path LINUX_BIN_DIR = Paths.get("/usr/local/bin");

	private static final Path WINDOWS_START_DIR_SYSTEM = Paths
			.get("C:/ProgramData/Microsoft/Windows/Start Menu/Programs");

	private static final Path WINDOWS_START_DIR_USER = Paths
			.get(System.getProperty("user.home") + "/AppData/Roaming/Microsoft/Windows/Start Menu/Programs");

	/**
	 * The component to install.
	 */
	private final S7SMavenArtifact component;

	/**
	 * The root directory for the installation.
	 */
	private final Path root;

	private final boolean gui;

	private boolean completed;

	private InstallTask(boolean gui, S7SMavenArtifact component, Path root) {
		updateProgress(0, 1);

		this.gui = gui;
		this.root = root;

		if (component.version() == null) {
			updateMessage("Downloading metadata");
			try {
				this.component = S7SMavenArtifact.of(component.groupId(), component.artifactId(),
						component.getLatestVersion(), component.classifier());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			this.component = component;
		}
	}

	private static String getPlatformClassifier() {
		switch (S7SSystem.OS_TYPE) {
		case WINDOWS:
			return "windows";
		case LINUX:
			return "linux";
		case MACOS:
			return "macos";
		default:
			throw new RuntimeException();
		}
	}

	public static InstallTask newServerTask(Path root) {
		return new InstallTask(false, S7SMavenArtifact.of("org.s7s", "server.java", null), root);
	}

	public static InstallTask newServerTaskGui() {
		return new InstallTask(true, S7SMavenArtifact.of("org.s7s", "server.java", null), Paths.get("/"));
	}

	public static InstallTask newClientLifegemTask(Path root) {
		return new InstallTask(false, S7SMavenArtifact.of("org.s7s", "client.lifegem", null, getPlatformClassifier()),
				root);
	}

	public static InstallTask newClientLifegemTaskGui() {
		return new InstallTask(true, S7SMavenArtifact.of("org.s7s", "client.lifegem", null, getPlatformClassifier()),
				Paths.get("/"));
	}

	public static InstallTask newClientAsceticTask(Path root) {
		return new InstallTask(false, S7SMavenArtifact.of("org.s7s", "client.ascetic", null), root);
	}

	public static InstallTask newClientAsceticTaskGui() {
		return new InstallTask(true, S7SMavenArtifact.of("org.s7s", "client.ascetic", null), Paths.get("/"));
	}

	public static InstallTask newAgentTask(Path root) {
		return new InstallTask(false, S7SMavenArtifact.of("org.s7s", "agent.java", null), root);
	}

	public static InstallTask newAgentTaskGui() {
		return new InstallTask(true, S7SMavenArtifact.of("org.s7s", "agent.java", null), Paths.get("/"));
	}

	@Override
	protected Void call() throws Exception {

		updateMessage("Executing installation for component: " + component.artifactId());

		// Choose install directories
		Path lib;
		Path bin;

		switch (S7SSystem.OS_TYPE) {
		case WINDOWS:
			// TODO
			lib = null;
			bin = null;
			break;
		default:
			lib = root.resolve("/opt/lib");
			bin = root.resolve(LINUX_BIN_DIR);
			break;
		}

		Files.createDirectories(lib);

		Path exe = lib.resolve(component.filename());

		// Download executable
		updateMessage("Downloading " + component.filename());
		try (var in = component.download()) {
			Files.copy(in, exe);
		}

		// Get dependencies from executable
		var tree = S7SJarFile.of(exe).getResource("/config/org.s7s.build.json", in -> new ObjectMapper().readTree(in))
				.get();

		long current = 0;
		long total = tree.get("dependencies").size();

		for (var element : tree.get("dependencies")) {
			var module = S7SMavenArtifact.of(element.get("group").asText(), element.get("artifact").asText(),
					element.get("version").asText(), element.get("classifier").asText());

			Path dependency = lib.resolve(module.filename());
			if (!Files.exists(dependency)) {
				InputStream local = InstallTask.class.getResourceAsStream("/" + module.filename());
				if (local != null) {
					updateMessage("Extracting " + module.filename());
					try (local) {
						Files.copy(local, dependency);
					}
				} else {
					updateMessage("Downloading " + module.filename());
					try (var in = module.download()) {
						Files.copy(in, dependency);
					}
				}
			}

			current++;
			updateProgress(current, total);
		}

		// Install start script
		switch (S7SSystem.OS_TYPE) {
		case LINUX:
			Files.writeString(lib.resolve("start"), """
					#!/bin/sh
					exec /usr/bin/java --module-path "%s" -m %s/%s.Main "%%@"
					""".formatted());
			Files.setPosixFilePermissions(lib.resolve("start"), Set.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE,
					GROUP_READ, GROUP_EXECUTE, OTHERS_READ, OTHERS_EXECUTE));
			break;
		case WINDOWS:
			Files.writeString(lib.resolve("start.bat"), """
					@echo off
					start javaw --module-path "%s" -m %s/%s.Main
					""".formatted());
			break;
		}

		switch (component.artifactId()) {
		case "server.java":
			if (Systemctl.isAvailable()) {
				var service = SystemdService.of(config -> {
					config.Type = SystemdService.Type.SIMPLE;
				});

				Systemctl.load().enable(service);
			}
			break;
		case "client.lifegem":
			switch (S7SSystem.OS_TYPE) {
			case WINDOWS:
				Files.write(lib.resolveSibling("Sandpolis.ico"),
						S7SJarFile.of(exe).getResource("/image/icon.ico").get());
				break;
			default:
				Files.write(lib.resolveSibling("Sandpolis.png"),
						S7SJarFile.of(exe).getResource("/image/icon@4x.png").get());
				break;
			}
			break;
		}

		completed = true;
		return null;
	}

	public boolean isCompleted() {
		return completed;
	}

	@Override
	protected void updateMessage(String message) {
		if (gui) {
			super.updateMessage(message);
		} else {
			System.out.println(message);
		}
	}

	@Override
	protected void updateProgress(double workDone, double max) {
		if (gui) {
			super.updateProgress(workDone, max);
		}
	}
}
