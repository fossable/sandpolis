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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.sandpolis.core.util.RandUtil;
import com.sandpolis.installer.JavafxInstaller;
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
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

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
	private Button btn_install;

	@FXML
	private ImageView banner;

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
		btn_install.setDisable((!chk_server.isSelected() && !chk_viewer_jfx.isSelected() && !chk_viewer_cli.isSelected()
				&& !chk_client.isSelected()) || qrTask != null);
	};

	private ChangeListener<Boolean> refreshClient = (ObservableValue<? extends Boolean> p, Boolean o, Boolean n) -> {
		if (!o && n) {
			qrTask = service.submit(() -> {
				do {
					String token = RandUtil.nextAlphabetic(32).toUpperCase();
					Node node = QrUtil.buildQr(token, qr_box.widthProperty(), qr_box.heightProperty(), Color.BLACK);
					Platform.runLater(() -> {
						qr_box.getChildren().setAll(node);
					});
					try {
						var result = CloudUtil.listen(token);
						if (result.status != 200) {
							setQrMessage("An error occurred");
							return;
						}

						client_config = result.config;
					} catch (IOException e) {
						setQrMessage("An error occurred");
						return;
					} catch (InterruptedException e) {
						return;
					}
				} while (!Thread.currentThread().isInterrupted() && client_config == null);

				setQrMessage("Association successful");
				qrTask = null;
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
		chk_viewer_jfx.selectedProperty().addListener(refreshScene);
		chk_viewer_cli.selectedProperty().addListener(refreshScene);
		chk_client.selectedProperty().addListener(refreshClient);

		pane_server.expandedProperty().bindBidirectional(chk_server.selectedProperty());
		pane_viewer_jfx.expandedProperty().bindBidirectional(chk_viewer_jfx.selectedProperty());
		pane_viewer_cli.expandedProperty().bindBidirectional(chk_viewer_cli.selectedProperty());
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

		chk_server.selectedProperty().removeListener(refreshScene);
		chk_viewer_jfx.selectedProperty().removeListener(refreshScene);
		chk_viewer_cli.selectedProperty().removeListener(refreshScene);
		chk_client.selectedProperty().removeListener(refreshClient);

		chk_server.setDisable(true);
		chk_viewer_jfx.setDisable(true);
		chk_viewer_cli.setDisable(true);
		chk_client.setDisable(true);
		btn_install.setDisable(true);

		Path base = Paths.get(System.getProperty("user.home") + "/.sandpolis");

		// Add installer tasks to the queue
		if (chk_viewer_jfx.isSelected()) {
			install(pane_viewer_jfx, JavafxInstaller.newViewerJfxInstaller(base.resolve("viewer")));
		} else {
			pane_viewer_jfx.setCollapsible(false);
		}
		if (chk_viewer_cli.isSelected()) {
			install(pane_viewer_cli, JavafxInstaller.newViewerCliInstaller(base.resolve("viewer-cli")));
		} else {
			pane_viewer_cli.setCollapsible(false);
		}
		if (chk_server.isSelected()) {
			install(pane_server, JavafxInstaller.newServerInstaller(base.resolve("server")));
		} else {
			pane_server.setCollapsible(false);
		}
		if (chk_client.isSelected()) {
			install(pane_client, JavafxInstaller.newClientInstaller(base.resolve("client"), client_config));
		} else {
			pane_client.setCollapsible(false);
		}

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

	private void install(TitledPane section, JavafxInstaller installer) {

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
