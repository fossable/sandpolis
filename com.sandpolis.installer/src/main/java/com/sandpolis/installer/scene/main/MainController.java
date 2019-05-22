/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.installer.scene.main;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sandpolis.installer.install.AbstractInstaller;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;

public class MainController {

	@FXML
	private CheckBox chk_server;

	@FXML
	private CheckBox chk_viewer_jfx;

	@FXML
	private CheckBox chk_viewer_cli;

	@FXML
	private CheckBox chk_client;

	@FXML
	private TitledPane pane_server;

	@FXML
	private TitledPane pane_viewer_jfx;

	@FXML
	private TitledPane pane_viewer_cli;

	@FXML
	private TitledPane pane_client;

	@FXML
	private TextField username;

	@FXML
	private PasswordField password;

	@FXML
	private TextField client_key;

	@FXML
	private Button btn_install;

	@FXML
	private ImageView banner;

	/**
	 * An executor that will run the installation.
	 */
	private static final ExecutorService service = Executors.newSingleThreadExecutor((Runnable r) -> {
		Thread thread = new Thread(r, "installation_thread");
		thread.setDaemon(true);
		return thread;
	});

	@FXML
	private void initialize() {

		chk_server.selectedProperty().addListener(this::refreshScene);
		chk_viewer_jfx.selectedProperty().addListener(this::refreshScene);
		chk_viewer_cli.selectedProperty().addListener(this::refreshScene);
		chk_client.selectedProperty().addListener(this::refreshScene);

		pane_server.expandedProperty().bindBidirectional(chk_server.selectedProperty());
		pane_viewer_jfx.expandedProperty().bindBidirectional(chk_viewer_jfx.selectedProperty());
		pane_viewer_cli.expandedProperty().bindBidirectional(chk_viewer_cli.selectedProperty());
		pane_client.expandedProperty().bindBidirectional(chk_client.selectedProperty());

		banner.setImage(new Image(MainController.class.getResourceAsStream("/image/logo.png")));
	}

	private void refreshScene(ObservableValue<?> p, boolean o, boolean n) {
		// Ensure at least one box is checked
		btn_install.setDisable(!chk_server.isSelected() && !chk_viewer_jfx.isSelected() && !chk_viewer_cli.isSelected()
				&& !chk_client.isSelected());
	}

	@FXML
	private void paste_key() {
		client_key.setText(Clipboard.getSystemClipboard().getString());
	}

	@FXML
	private void install() {

		username.setDisable(true);
		password.setDisable(true);
		chk_server.setDisable(true);
		chk_viewer_jfx.setDisable(true);
		chk_viewer_cli.setDisable(true);
		chk_client.setDisable(true);
		btn_install.setDisable(true);

		if (chk_viewer_jfx.isSelected()) {
			install(pane_viewer_jfx, AbstractInstaller.newViewerJfxInstaller());
		} else {
			pane_viewer_jfx.setCollapsible(false);
		}
		if (chk_viewer_cli.isSelected()) {
			install(pane_viewer_cli, AbstractInstaller.newViewerCliInstaller());
		} else {
			pane_viewer_cli.setCollapsible(false);
		}
		if (chk_server.isSelected()) {
			install(pane_server, AbstractInstaller.newServerInstaller(username.getText(), password.getText()));
		} else {
			pane_server.setCollapsible(false);
		}
		if (chk_client.isSelected()) {
			install(pane_client, AbstractInstaller.newClientInstaller(client_key.getText()));
		} else {
			pane_client.setCollapsible(false);
		}

		service.execute(() -> {
			Platform.runLater(() -> {
				btn_install.setDisable(false);
				btn_install.setText("Finish");
				btn_install.setOnAction((e) -> {
					System.exit(0);
				});
			});
		});
	}

	private void install(TitledPane section, AbstractInstaller installer) {

		ProgressIndicator progress = new ProgressIndicator(0.0);
		installer.setOnScheduled(event -> {
			progress.setPrefHeight(22);
			progress.progressProperty().bind(installer.progressProperty());
			section.setGraphic(progress);
		});

		installer.setOnSucceeded(event -> {
			section.setText("Installed successfully");
			section.setExpanded(false);
			section.setCollapsible(false);

			progress.progressProperty().unbind();
			progress.setProgress(1.0);
		});

		installer.setOnFailed(event -> {
			section.setText("Installation failed!");
			section.setCollapsible(false);

			installer.getException().printStackTrace();
		});

		service.execute(installer);

		// Check outcome of task
		service.execute(() -> {
			if (!installer.isCompleted()) {
				Platform.runLater(() -> {
					btn_install.setDisable(false);
					btn_install.setText("Exit");
					btn_install.setOnAction((e) -> {
						System.exit(0);
					});
				});

				service.shutdownNow();
			}
		});
	}
}
