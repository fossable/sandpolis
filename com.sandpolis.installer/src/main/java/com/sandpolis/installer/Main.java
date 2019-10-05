/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.installer;

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
	private Main() {
	}

	public static void main(String[] args) {
		new Thread(() -> Application.launch(UI.class)).start();
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

			Scene scene = new Scene(node, 430, 650);
			scene.getStylesheets().add("/css/default.css");
			stage.setScene(scene);
			stage.setResizable(false);
			stage.show();
		}
	}
}
