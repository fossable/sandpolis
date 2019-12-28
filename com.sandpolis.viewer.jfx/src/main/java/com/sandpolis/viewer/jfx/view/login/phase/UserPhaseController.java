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
package com.sandpolis.viewer.jfx.view.login.phase;

import java.security.cert.X509Certificate;

import javax.net.ssl.SSLPeerUnverifiedException;

import com.google.common.eventbus.Subscribe;
import com.sandpolis.core.net.sock.Sock;
import com.sandpolis.core.proto.net.MsgServer.RS_ServerBanner;
import com.sandpolis.core.util.CertUtil;
import com.sandpolis.core.viewer.cmd.ServerCmd;
import com.sandpolis.viewer.jfx.common.controller.AbstractController;
import com.sandpolis.viewer.jfx.view.login.Events.LoginEndedEvent;
import com.sandpolis.viewer.jfx.view.login.Events.LoginStartedEvent;
import com.sandpolis.viewer.jfx.view.login.LoginController.LoginPhase;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

public class UserPhaseController extends AbstractController {

	/**
	 * The amount of time to wait between pings in milliseconds.
	 */
	private static final int PING_PERIOD = 1500;

	@FXML
	private PasswordField password;
	@FXML
	private ProgressIndicator ping_indicator;
	@FXML
	private Label server_certificate;
	@FXML
	private TitledPane server_info;
	@FXML
	private Label server_ip;
	@FXML
	private Label server_ping;
	@FXML
	private Label server_uuid;
	@FXML
	private Label server_version;
	@FXML
	private TextField username;

	private Task<Void> pinger;

	/**
	 * Scale the ping approximation so it's easier for the user to distinguish
	 * between small pings and large pings.
	 *
	 * @param ping The last ping value
	 * @return A duration representative of the given ping value
	 */
	private Duration calculatePingVisual(long ping) {
		return Duration.millis(Math.min(PING_PERIOD, 4 * ping + 80));
	}

	/**
	 * Get the current password.
	 *
	 * @return The password
	 */
	public String getPassword() {
		return password.getText();
	}

	/**
	 * Get the current username.
	 *
	 * @return The username
	 */
	public String getUsername() {
		return username.getText();
	}

	@Subscribe
	void phaseChanged(LoginPhase phase) {
		switch (phase) {
		case USER_INPUT:
			break;
		default:
			if (pinger != null)
				pinger.cancel();
		}
	}

	@Subscribe
	void setBannerInfo(RS_ServerBanner rs) {

		// Set static info
		if (!rs.getBanner().isBlank())
			server_info.setText(rs.getBanner());
		server_version.setText(rs.getVersion());
	}

	@Subscribe
	void setCertificateInfo(Sock sock) {

		try {
			X509Certificate certificate = sock.getRemoteCertificate();

			// Set certificate info
			server_certificate.setTooltip(new Tooltip(CertUtil.getInfoString(certificate)));

			// Set validity
			if (CertUtil.checkValidity(certificate)) {
				server_certificate.setText("Valid certificate");
				server_certificate.getStyleClass().add("login-info_field-green");
			} else {
				server_certificate.setText("Expired certificate!");
				server_certificate.getStyleClass().add("login-info_field-red");
			}

		} catch (SSLPeerUnverifiedException e) {
			server_certificate.setText("Invalid certificate!");
			server_certificate.getStyleClass().add("login-info_field-red");
		}

	}

	@Subscribe
	void setSockInfo(Sock sock) {
		server_ip.setText(sock.getRemoteIP());
		server_uuid.setText(sock.getRemoteUuid());
	}

	@Subscribe
	void startPinger(Sock sock) {
		if (pinger != null)
			pinger.cancel();

		pinger = new Task<>() {

			@Override
			protected Void call() throws Exception {
				while (!isCancelled()) {
					long ping = ServerCmd.async().target(sock).ping();// TODO not async
					updateMessage(ping + " ms");

					Platform.runLater(() -> {
						ping_indicator.setProgress(0);

						// Trigger animation
						new Timeline(new KeyFrame(calculatePingVisual(ping),
								new KeyValue(ping_indicator.progressProperty(), 0.999999999))).play();
					});
					Thread.sleep(PING_PERIOD);
				}
				return null;
			}
		};
		server_ping.textProperty().bind(pinger.messageProperty());
		new Thread(pinger).start();
	}

	@Subscribe
	void onEvent(LoginStartedEvent event) {
		username.setDisable(true);
		password.setDisable(true);
	}

	@Subscribe
	void onEvent(LoginEndedEvent event) {
		username.setDisable(false);
		password.setDisable(false);
	}
}
