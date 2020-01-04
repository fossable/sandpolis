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
package com.sandpolis.installer.scene.main;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.util.RandUtil;
import com.sandpolis.installer.Main;
import com.sandpolis.installer.task.GuiInstallTask;
import com.sandpolis.installer.util.CloudUtil;
import com.sandpolis.installer.util.QrUtil;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class MainController {

	private static final Logger log = LoggerFactory.getLogger(MainController.class);

	@FXML
	private CheckBox chk_server;
	@FXML
	private CheckBox chk_viewer_lifegem;
	@FXML
	private CheckBox chk_viewer_ascetic;
	@FXML
	private CheckBox chk_client;
	@FXML
	private TitledPane pane_server;
	@FXML
	private TitledPane pane_viewer_lifegem;
	@FXML
	private TitledPane pane_viewer_ascetic;
	@FXML
	private TitledPane pane_client;
	@FXML
	private Button btn_install;
	@FXML
	private ImageView banner;
	@FXML
	private TextField username;
	@FXML
	private PasswordField password;
	@FXML
	private VBox qr_box;
	@FXML
	private Label status;

	/**
	 * An executor that can run background tasks for the installer.
	 */
	private static final ExecutorService service = Executors.newSingleThreadExecutor((Runnable r) -> {
		Thread thread = new Thread(r, "background_thread");
		thread.setDaemon(true);
		return thread;
	});

	private Future<?> qrTask;

	private String client_config;

	private ChangeListener<Boolean> refreshScene = (ObservableValue<? extends Boolean> p, Boolean o, Boolean n) -> {
		// Ensure at least one box is checked
		btn_install.setDisable((!chk_server.isSelected() && !chk_viewer_lifegem.isSelected()
				&& !chk_viewer_ascetic.isSelected() && !chk_client.isSelected()) || qrTask != null);
	};

	private ChangeListener<Boolean> refreshClient = (ObservableValue<? extends Boolean> p, Boolean o, Boolean n) -> {
		if (client_config != null) {
			setQrMessage("Association successful");
		} else if (!o && n) {
			qrTask = service.submit(() -> {
				do {
					String token = RandUtil.nextAlphabetic(32).toUpperCase();
					Node node = QrUtil.buildQr(token, qr_box.widthProperty(), qr_box.heightProperty(), Color.BLACK);
					Platform.runLater(() -> {
						qr_box.getChildren().setAll(node);
					});
					try {
						CloudUtil.listen(token).ifPresent(config -> {
							client_config = config;
						});
					} catch (IOException e) {
						setQrMessage("An error occurred");
						log.error("Cloud request failed", e);
						return;
					} catch (InterruptedException e) {
						return;
					}
				} while (!Thread.currentThread().isInterrupted() && client_config == null);

				qrTask = null;

				setQrMessage("Association successful");
				refreshScene.changed(p, o, n);
			});
		} else if (o && !n) {
			if (qrTask != null) {
				qrTask.cancel(true);
				qrTask = null;
			}
		}

		refreshScene.changed(p, o, n);
	};

	@FXML
	private void initialize() {

		chk_server.selectedProperty().addListener(refreshScene);
		chk_viewer_lifegem.selectedProperty().addListener(refreshScene);
		chk_viewer_ascetic.selectedProperty().addListener(refreshScene);
		chk_client.selectedProperty().addListener(refreshClient);

		pane_server.expandedProperty().bindBidirectional(chk_server.selectedProperty());
		pane_viewer_lifegem.expandedProperty().bindBidirectional(chk_viewer_lifegem.selectedProperty());
		pane_viewer_ascetic.expandedProperty().bindBidirectional(chk_viewer_ascetic.selectedProperty());
		pane_client.expandedProperty().bindBidirectional(chk_client.selectedProperty());

		banner.setImage(new Image(MainController.class.getResourceAsStream("/image/logo.png")));
	}

	private void setQrMessage(String message) {
		Platform.runLater(() -> {
			var label = new Label(message);
			label.setWrapText(true);
			qr_box.getChildren().setAll(label);
		});
	}

	@FXML
	private void install() {

		// Validate user input
		if (chk_server.isSelected()) {
			if (username.getText().isBlank() || password.getText().isEmpty()) {
				status.setText("Invalid username or password");
				return;
			}

			username.setDisable(true);
			password.setDisable(true);
		}

		chk_server.selectedProperty().removeListener(refreshScene);
		chk_viewer_lifegem.selectedProperty().removeListener(refreshScene);
		chk_viewer_ascetic.selectedProperty().removeListener(refreshScene);
		chk_client.selectedProperty().removeListener(refreshClient);

		chk_server.setDisable(true);
		chk_viewer_lifegem.setDisable(true);
		chk_viewer_ascetic.setDisable(true);
		chk_client.setDisable(true);
		btn_install.setDisable(true);

		Main.PATH.evaluate().ifPresent(base -> {
			// Add installer tasks to the queue
			if (chk_viewer_lifegem.isSelected()) {
				install(pane_viewer_lifegem, GuiInstallTask.newViewerLifegemTask(base.resolve("viewer-gui")));
			} else {
				pane_viewer_lifegem.setCollapsible(false);
			}
			if (chk_viewer_ascetic.isSelected()) {
				install(pane_viewer_ascetic, GuiInstallTask.newViewerAsceticTask(base.resolve("viewer-cli")));
			} else {
				pane_viewer_ascetic.setCollapsible(false);
			}
			if (chk_server.isSelected()) {
				install(pane_server,
						GuiInstallTask.newServerTask(base.resolve("server"), username.getText(), password.getText()));
			} else {
				pane_server.setCollapsible(false);
			}
			if (chk_client.isSelected()) {
				install(pane_client, GuiInstallTask.newClientTask(base.resolve("client"), client_config));
			} else {
				pane_client.setCollapsible(false);
			}
		});

		// The final task only runs if all previous tasks succeeded
		service.execute(() -> {
			Platform.runLater(() -> {
				btn_install.setDisable(false);
				btn_install.setText("Finish");
				btn_install.setOnAction((e) -> {
					System.exit(0);
				});
				status.textProperty().unbind();
				status.setText("Installation succeeded!");
			});
		});
	}

	private void install(TitledPane section, GuiInstallTask installer) {

		ProgressIndicator progress = new ProgressIndicator(0.0);
		installer.setOnScheduled(event -> {
			progress.setPrefHeight(22);
			progress.progressProperty().bind(installer.progressProperty());
			status.textProperty().bind(installer.messageProperty());
			section.setGraphic(progress);
		});

		installer.setOnSucceeded(event -> {
			section.setText("Installed successfully");
			section.setExpanded(false);
			section.setCollapsible(false);

			progress.progressProperty().unbind();
			status.textProperty().unbind();
			progress.setProgress(1.0);
		});

		installer.setOnFailed(event -> {
			section.setText("Installation failed!");
			section.setCollapsible(false);

			progress.progressProperty().unbind();
			status.textProperty().unbind();
			installer.getException().printStackTrace();
		});

		// Schedule the install task
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
					status.textProperty().unbind();
					status.setText("Installation failed!");
				});

				// Don't let any more tasks execute
				service.shutdownNow();
			}
		});
	}
}
