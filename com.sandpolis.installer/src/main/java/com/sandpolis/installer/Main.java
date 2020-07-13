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

import com.sandpolis.installer.task.CliInstallTask;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * This class is the entry point for Installer instances.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class Main {

	public static final boolean IS_WINDOWS;
	public static final boolean IS_LINUX;
	public static final boolean IS_MAC;

	static {
		String name = System.getProperty("os.name").toLowerCase();

		IS_WINDOWS = name.startsWith("windows");
		IS_LINUX = name.startsWith("linux");
		IS_MAC = name.startsWith("mac") || name.startsWith("darwin");
	}

	/**
	 * The version to install.
	 */
	public static final String VERSION = System.getProperty("version", "latest");

	/**
	 * The components to install.
	 */
	public static final String COMPONENTS = System.getProperty("components", "");

	/**
	 * The installation path.
	 */
	public static final InstallPath PATH = InstallPath.of(System.getProperty("path"),
			System.getProperty("user.home") + "/.sandpolis");

	/**
	 * The desktop file install path.
	 */
	public static final InstallPath EXT_LINUX_DESKTOP = InstallPath.of(System.getProperty("ext.linux.desktop"),
			"/usr/share/applications", System.getProperty("user.home") + "/.local/share/applications");

	/**
	 * The standard PATH on Linux.
	 */
	public static final InstallPath EXT_LINUX_PATH = InstallPath.of("/usr/bin", "/usr/local/sbin", "/usr/local/bin");

	/**
	 * The start menu install path.
	 */
	public static final InstallPath EXT_WINDOWS_START = InstallPath.of(System.getProperty("ext.windows.start"),
			"C:/ProgramData/Microsoft/Windows/Start Menu/Programs",
			System.getProperty("user.home") + "/AppData/Roaming/Microsoft/Windows/Start Menu/Programs");

	/**
	 * The desktop install path.
	 */
	public static final InstallPath EXT_WINDOWS_DESKTOP = InstallPath.of(System.getProperty("ext.windows.desktop"),
			System.getProperty("user.home") + "/Desktop");

	/**
	 * The standard PATH on Windows.
	 */
	public static final InstallPath EXT_WINDOWS_PATH = InstallPath.of();

	public static void main(String[] args) throws Exception {

		if (System.getProperty("path") != null) {
			Path path = PATH.evaluate().get();
			for (String component : COMPONENTS.split(",")) {
				switch (component) {
				case "server":
					CliInstallTask.newServerTask(path, "admin", "password").call();
					break;
				case "viewer-gui":
					CliInstallTask.newViewerLifegemTask(path).call();
					break;
				case "viewer-cli":
					CliInstallTask.newViewerAsceticTask(path).call();
					break;
				}
			}
		} else {
			// Start GUI
			new Thread(() -> Application.launch(UI.class)).start();
		}
	}

	/**
	 * The {@link Application} class for starting the user interface.
	 */
	public static class UI extends Application {

		@Override
		public void start(Stage stage) throws Exception {
			stage.setTitle("Sandpolis Installer");
			stage.setOnCloseRequest(event -> {
				Platform.exit();
				System.exit(0);
			});

			// Set icons
			Stream.of("/image/icon.png", "/image/icon@2x.png", "/image/icon@3x.png", "/image/icon@4x.png")
					.map(UI.class::getResourceAsStream).map(Image::new).forEach(stage.getIcons()::add);

			Parent node = new FXMLLoader(UI.class.getResource("/fxml/Main.fxml")).load();

			Scene scene = new Scene(node, 430, 750);
			scene.getStylesheets().add("/css/default.css");
			stage.setScene(scene);
			stage.setResizable(false);
			stage.show();

			closeSplash();
		}

		/**
		 * Close an AWT splash screen if one exists.
		 */
		private void closeSplash() {
			try {
				var splash = Class.forName("java.awt.SplashScreen").getMethod("getSplashScreen").invoke(null);
				if (splash != null) {
					splash.getClass().getMethod("close").invoke(splash);
				}
			} catch (Exception e) {
				// No splash screen found
			}
		}
	}

	private Main() {
	}
}
