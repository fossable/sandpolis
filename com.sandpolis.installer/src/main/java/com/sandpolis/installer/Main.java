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

	public static void main(String[] args) throws Exception {
		String path = System.getProperty("path");
		String components = System.getProperty("component", "server,viewer-jfx,viewer-cli");

		if (path != null) {
			for (String component : components.split(",")) {
				switch (component) {
				case "server":
					CliInstaller.newServerInstaller(Paths.get(path)).call();
					break;
				case "viewer-jfx":
					CliInstaller.newViewerJfxInstaller(Paths.get(path)).call();
					break;
				case "viewer-cli":
					CliInstaller.newViewerCliInstaller(Paths.get(path)).call();
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
