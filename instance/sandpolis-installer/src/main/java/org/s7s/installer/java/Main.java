//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.instance.installer.java;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public final class Main {

	private static void help() {
		System.out.println("""
				--install-server
				--install-client-desktop
				--install-client-terminal
				--install-agent

				--uninstall-server
				--uninstall-client-desktop
				--uninstall-client-terminal
				--uninstall-agent

				--chroot

				--help    Help menu
				""");
		System.exit(0);
	}

	public static void main(String[] args) throws Exception {

		if (args.length == 0) {
			// Start GUI
			new Thread(() -> Application.launch(UI.class)).start();
			return;
		}

		Path root = Paths.get("/");
		boolean install_server = false;

		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "--root":
				if (++i >= args.length) {
					throw new RuntimeException();
				}
				root = Paths.get(args[i]);
				break;
			case "--install-server":

				break;
			}
		}

		if (install_server) {
			InstallTask.newServerTask(root).call();
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
