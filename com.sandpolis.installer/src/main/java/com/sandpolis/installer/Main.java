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

import java.nio.file.Paths;
import java.util.stream.Stream;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * This stub is the entry point for Installer instances.
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
	 * The installation path.
	 */
	public static final String PATH = System.getProperty("path", System.getProperty("user.home") + "/.sandpolis");

	/**
	 * The components to install.
	 */
	public static final String COMPONENTS = System.getProperty("components", "");

	/**
	 * The version to install.
	 */
	public static final String VERSION = System.getProperty("version", "latest");

	public static final String EXT_LINUX_DESKTOP = System.getProperty("ext.linux.desktop",
			"/usr/share/applications;" + System.getProperty("user.home") + "/.local/share/applications");
	public static final String EXT_LINUX_BIN = System.getProperty("ext.linux.bin",
			"/usr/bin;/usr/local/sbin;/usr/local/bin");
	public static final String EXT_WINDOWS_START = System.getProperty("ext.windows.start",
			"C:/ProgramData/Microsoft/Windows/Start Menu/Programs;" + System.getProperty("user.home")
					+ "/AppData/Roaming/Microsoft/Windows/Start Menu/Programs");
	public static final String EXT_WINDOWS_DESKTOP = System.getProperty("ext.windows.desktop",
			System.getProperty("user.home") + "/Desktop");

	public static void main(String[] args) throws Exception {

		if (System.getProperty("path") != null) {
			for (String component : COMPONENTS.split(",")) {
				switch (component) {
				case "server":
					CliInstaller.newServerInstaller(Paths.get(PATH)).call();
					break;
				case "viewer-jfx":
					CliInstaller.newViewerJfxInstaller(Paths.get(PATH)).call();
					break;
				case "viewer-cli":
					CliInstaller.newViewerCliInstaller(Paths.get(PATH)).call();
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
		}
	}

	private Main() {
	}
}
