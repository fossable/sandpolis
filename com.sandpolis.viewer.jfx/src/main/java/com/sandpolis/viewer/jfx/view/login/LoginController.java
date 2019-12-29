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
package com.sandpolis.viewer.jfx.view.login;

import static com.sandpolis.core.instance.store.plugin.PluginStore.PluginStore;
import static com.sandpolis.core.instance.store.pref.PrefStore.PrefStore;
import static com.sandpolis.core.net.store.connection.ConnectionStore.ConnectionStore;
import static com.sandpolis.viewer.jfx.store.stage.StageStore.StageStore;

import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.eventbus.Subscribe;
import com.sandpolis.core.net.future.SockFuture;
import com.sandpolis.core.net.sock.Sock;
import com.sandpolis.core.proto.net.MsgPlugin.RS_PluginList;
import com.sandpolis.core.proto.net.MsgServer.RS_ServerBanner;
import com.sandpolis.core.proto.util.Result.Outcome;
import com.sandpolis.core.viewer.cmd.LoginCmd;
import com.sandpolis.core.viewer.cmd.PluginCmd;
import com.sandpolis.core.viewer.cmd.ServerCmd;
import com.sandpolis.viewer.jfx.common.FxUtil;
import com.sandpolis.viewer.jfx.common.controller.FxController;
import com.sandpolis.viewer.jfx.common.pane.CarouselPane;
import com.sandpolis.viewer.jfx.view.login.Events.ConnectEndedEvent;
import com.sandpolis.viewer.jfx.view.login.Events.ConnectStartedEvent;
import com.sandpolis.viewer.jfx.view.login.Events.LoginEndedEvent;
import com.sandpolis.viewer.jfx.view.login.Events.LoginStartedEvent;
import com.sandpolis.viewer.jfx.view.login.phase.PluginPhaseController;
import com.sandpolis.viewer.jfx.view.login.phase.ServerPhaseController;
import com.sandpolis.viewer.jfx.view.login.phase.UserPhaseController;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

public class LoginController extends FxController {
	public enum LoginPhase {
		COMPLETE, PLUGIN_PHASE, SERVER_INPUT, USER_INPUT;
	}

	/**
	 * The cached default banner.
	 */
	private static final Image defaultImage = new Image(
			LoginController.class.getResourceAsStream("/image/view/login/banner.png"));
	@FXML
	private ImageView bannerImage;
	@FXML
	private Button btn_back;
	@FXML
	private Button btn_continue;
	@FXML
	private CarouselPane carousel;
	/**
	 * The connection to the server.
	 */
	private Sock connection;
	/**
	 * The current login phase.
	 */
	private SimpleObjectProperty<LoginPhase> phase = new SimpleObjectProperty<>(LoginPhase.SERVER_INPUT);

	@FXML
	private PluginPhaseController pluginPhaseController;

	@FXML
	private ServerPhaseController serverPhaseController;

	@FXML
	private Label status;

	@FXML
	private UserPhaseController userPhaseController;

	@FXML
	private void btn_back(ActionEvent event) {
		switch (phase.get()) {
		case SERVER_INPUT:
			System.exit(0);
			break;
		case USER_INPUT:
			phase.set(LoginPhase.SERVER_INPUT);
			if (connection != null)
				connection.close();
			break;
		case PLUGIN_PHASE:
			// Back button isn't allowed
			break;
		default:
			throw new RuntimeException();
		}
	}

	@FXML
	private void btn_continue(ActionEvent event) {
		switch (phase.get()) {
		case SERVER_INPUT:
			post(ConnectStartedEvent::new);
			setStatus("Connecting to " + serverPhaseController.getAddress());
			ConnectionStore.connect(serverPhaseController.getAddress(), serverPhaseController.getPort(), false)
					.addListener((SockFuture sockFuture) -> {
						if (sockFuture.isSuccess()) {
							connection = sockFuture.get();
							// TODO check sock state
							setStatus("Downloading server metadata");
							ServerCmd.async().target(connection).pool("ui.fx_thread").getServerBanner()
									.addHandler((RS_ServerBanner rs) -> {
										if (!rs.getBannerImage().isEmpty())
											setBannerImage(new Image(rs.getBannerImage().newInput()));

										bus.post(connection);
										bus.post(rs);
										phase.set(LoginPhase.USER_INPUT);
										post(ConnectEndedEvent::new);
									}).addHandler((Outcome rs) -> {
										post(ConnectEndedEvent::new);
										setStatus("Connection attempt failed");
									});
						} else {
							setStatus("Connection attempt failed");
							post(ConnectEndedEvent::new);
						}
					});
			break;
		case USER_INPUT:
			post(LoginStartedEvent::new);
			setStatus("Logging in");
			LoginCmd.async().target(connection).pool("ui.fx_thread")
					.login(userPhaseController.getUsername(), userPhaseController.getPassword())
					.addHandler((Outcome rs) -> {
						if (rs.getResult()) {
							PluginCmd.async().enumerate().addHandler((RS_PluginList rs2) -> {

								var newPlugins = rs2.getPluginList().stream().filter(descriptor -> {
									return PluginStore.get(descriptor.getId()).isEmpty();
								}).collect(Collectors.toList());

								post(LoginEndedEvent::new);

								if (!newPlugins.isEmpty()) {
									pluginPhaseController.setPlugins(newPlugins);
									phase.set(LoginPhase.PLUGIN_PHASE);
								} else {
									phase.set(LoginPhase.COMPLETE);
									launchApplication();
								}
							});
						} else {
							setStatus("Login attempt failed");
							post(LoginEndedEvent::new);
						}
					});
			break;
		case PLUGIN_PHASE:
			pluginPhaseController.install();
			phase.set(LoginPhase.COMPLETE);
			launchApplication();
			break;
		default:
			throw new RuntimeException();
		}
	}

	@FXML
	private void initialize() {
		register(serverPhaseController, userPhaseController, pluginPhaseController);

		resetBannerImage();
		phase.addListener((p, o, n) -> {
			if (o != n) {
				bus.post(n);

				Platform.runLater(() -> {
					switch (n) {
					case SERVER_INPUT:
						setStatus("");
						btn_back.setText("Exit");
						btn_back.setDisable(false);
						btn_continue.setText("Connect");
						btn_continue.setDisable(false);
						carousel.moveBackward();
						resetBannerImage();
						break;
					case USER_INPUT:
						setStatus("");
						btn_back.setText("Back");
						btn_back.setDisable(false);
						btn_continue.setText("Login");
						btn_continue.setDisable(false);
						carousel.moveForward();
						break;
					case PLUGIN_PHASE:
						status.setText("The server requests that additional plugins be installed");
						btn_back.setText("Back");
						btn_back.setDisable(true);
						btn_continue.setText("Continue");
						btn_continue.setDisable(false);
						carousel.moveForward();
						break;
					case COMPLETE:
						break;
					default:
						throw new RuntimeException("Unexpected phase: " + n);
					}
				});
			}
		});
	}

	/**
	 * Close the current stage and open the main stage.
	 */
	private void launchApplication() {
		StageStore.close(stage);
		StageStore.newStage().root("/fxml/view/main/Main.fxml")
				.size(PrefStore.getInt("ui.view.main.width"), PrefStore.getInt("ui.view.main.height"))
				.title(FxUtil.translate("stage.main.title")).show();
	}

	@Subscribe
	void onEvent(LoginStartedEvent event) {
		btn_continue.setDisable(true);
		btn_back.setDisable(true);
	}

	@Subscribe
	void onEvent(LoginEndedEvent event) {
		btn_continue.setDisable(false);
		btn_back.setDisable(false);
	}

	@Subscribe
	void onEvent(ConnectStartedEvent event) {
		btn_continue.setDisable(true);
		btn_back.setDisable(true);
	}

	@Subscribe
	void onEvent(ConnectEndedEvent event) {
		btn_continue.setDisable(false);
		btn_back.setDisable(false);
	}

	/**
	 * Change the banner to default if necessary.
	 */
	private void resetBannerImage() {
		setBannerImage(defaultImage);
	}

	/**
	 * Change the banner to the given image if it's not already displayed.
	 *
	 * @param nextImage The new banner
	 */
	private void setBannerImage(Image nextImage) {
		Objects.requireNonNull(nextImage);
		if (nextImage.equals(bannerImage.getImage()))
			return;

		FadeTransition fade = new FadeTransition(Duration.millis(300), bannerImage);
		fade.setFromValue(1.0);
		fade.setToValue(0.0);
		fade.setOnFinished(event -> {
			bannerImage.setImage(nextImage);
			fade.setOnFinished(null);
			fade.setRate(-fade.getRate());
			fade.play();
		});

		fade.play();
	}

	private void setStatus(String s) {
		setStatus(s, "");
	}

	private void setStatus(String s, String type) {
		Platform.runLater(() -> {
			status.setText(s);
		});
	}

}
