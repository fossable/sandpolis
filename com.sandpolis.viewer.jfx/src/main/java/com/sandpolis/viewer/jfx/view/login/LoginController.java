/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
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
package com.sandpolis.viewer.jfx.view.login;

import java.util.Objects;

import com.sandpolis.core.net.Sock;
import com.sandpolis.core.net.future.ResponseFuture;
import com.sandpolis.core.net.future.SockFuture;
import com.sandpolis.core.proto.net.MCServer.RS_ServerBanner;
import com.sandpolis.core.proto.util.Result.Outcome;
import com.sandpolis.viewer.cmd.LoginCmd;
import com.sandpolis.viewer.cmd.NetworkCmd;
import com.sandpolis.viewer.cmd.ServerCmd;
import com.sandpolis.viewer.jfx.common.controller.FxController;
import com.sandpolis.viewer.jfx.common.pane.CarouselPane;
import com.sandpolis.viewer.jfx.store.stage.StageStore;
import com.sandpolis.viewer.jfx.view.login.phase.ServerPhaseController;
import com.sandpolis.viewer.jfx.view.login.phase.UserPhaseController;

import javafx.animation.FadeTransition;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

public class LoginController extends FxController {
	@FXML
	private Button btn_back;
	@FXML
	private Button btn_continue;
	@FXML
	private CarouselPane carousel;
	@FXML
	private UserPhaseController userPhaseController;
	@FXML
	private ServerPhaseController serverPhaseController;
	@FXML
	private ImageView bannerImage;

	/**
	 * The cached default banner.
	 */
	private Image defaultImage;

	/**
	 * The connection to the server.
	 */
	private Sock connection;

	private enum LoginPhase {
		SERVER_INPUT, USER_INPUT, PLUGIN_PHASE;
	}

	private SimpleObjectProperty<LoginPhase> phase = new SimpleObjectProperty<>(LoginPhase.SERVER_INPUT);

	@FXML
	private void initialize() {
		register(serverPhaseController, userPhaseController);

		defaultImage = new Image(LoginController.class.getResourceAsStream("/image/view/login/banner.png"));
		resetBannerImage();

		phase.addListener((p, o, n) -> {
			if (o != n) {
				switch (n) {
				case SERVER_INPUT:
					btn_back.setText("Exit");
					btn_continue.setText("Connect");
					carousel.moveBackward();
					resetBannerImage();
					break;
				case USER_INPUT:
					btn_back.setText("Back");
					btn_continue.setText("Login");
					carousel.moveForward();
					break;
				default:
					throw new RuntimeException();
				}
			}
		});
	}

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
			break;
		default:
			throw new RuntimeException();
		}
	}

	@FXML
	private void btn_continue(ActionEvent event) {
		switch (phase.get()) {
		case SERVER_INPUT:
			NetworkCmd.async().connect(serverPhaseController.getAddress(), serverPhaseController.getPort())
					.addListener((SockFuture sockFuture) -> {
						if (sockFuture.isSuccess()) {
							connection = sockFuture.get();
							ServerCmd.async().target(connection).pool("ui.fx").getServerBanner()
									.addListener((ResponseFuture<RS_ServerBanner> responseFuture) -> {
										if (responseFuture.isSuccess()) {
											RS_ServerBanner banner = responseFuture.get();
											if (!banner.getBannerImage().isEmpty())
												setBannerImage(new Image(banner.getBannerImage().newInput()));

											bus.post(connection);
											bus.post(banner);
											phase.set(LoginPhase.USER_INPUT);
										}
									});
						}
					});

			break;
		case USER_INPUT:
			LoginCmd.async().target(connection).pool("ui.fx")
					.login(userPhaseController.getUsername(), userPhaseController.getPassword())
					.addListener((ResponseFuture<Outcome> outcomeFuture) -> {
						if (outcomeFuture.isSuccess()) {
							// TODO plugin sync
							launchApplication();
						}
					});
			break;
		case PLUGIN_PHASE:
			// TODO launch main
			break;
		default:
			throw new RuntimeException();
		}
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

	/**
	 * Change the banner to default if necessary.
	 */
	private void resetBannerImage() {
		setBannerImage(defaultImage);
	}

	/**
	 * Close the current stage and open the main stage.
	 */
	private void launchApplication() {
		StageStore.close(stage);

		StageStore.newStage().root("/fxml/view/main/Main.fxml").size(420, 420 * 0.618).show();
	}

}
