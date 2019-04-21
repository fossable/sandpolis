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
package com.sandpolis.viewer.jfx.view.login.phase;

import java.security.cert.X509Certificate;

import javax.net.ssl.SSLPeerUnverifiedException;

import com.google.common.eventbus.Subscribe;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.proto.net.MCServer.RS_ServerBanner;
import com.sandpolis.core.util.CertUtil;
import com.sandpolis.viewer.jfx.common.controller.AbstractController;
import com.sandpolis.viewer.jfx.view.login.LoginController.LoginPhase;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;

public class UserPhaseController extends AbstractController {

	@FXML
	private Label server_ip;
	@FXML
	private Label server_certificate;
	@FXML
	private Label server_banner;
	@FXML
	private Label server_version;
	@FXML
	private Label server_ping;
	@FXML
	private TextField username;
	@FXML
	private PasswordField password;

	private Task<Void> pinger;

	@Subscribe
	public void setBannerInfo(RS_ServerBanner rs) {

		// Set static info
		server_banner.setText(rs.getBanner());
		server_version.setText(rs.getVersion());
	}

	@Subscribe
	public void setSockInfo(Sock sock) {
		server_ip.setText(sock.getRemoteIP());
	}

	@Subscribe
	public void setCertificate(Sock sock) {

		try {
			X509Certificate certificate = sock.getRemoteCertificate();

			// Set certificate info
			server_certificate.setTooltip(new Tooltip(CertUtil.getInfoString(certificate)));

			// Set validity
			if (CertUtil.getValidity(certificate)) {
				server_certificate.setText("Valid certificate");
				// TODO colorize
			} else {
				server_certificate.setText("Expired certificate!");
				// TODO colorize
			}

		} catch (SSLPeerUnverifiedException e) {
			server_certificate.setText("Invalid certificate!");
			// TODO colorize
		}

	}

	@Subscribe
	public void startPinger(Sock sock) {
		if (pinger != null)
			pinger.cancel();

		pinger = new Task<>() {

			@Override
			protected Void call() throws Exception {
				while (!isCancelled()) {
					updateMessage(sock.ping() + " ms");
					Thread.sleep(1000);
				}
				return null;
			}
		};
		server_ping.textProperty().bind(pinger.messageProperty());
		new Thread(pinger).start();
	}

	@Subscribe
	public void phaseChanged(LoginPhase phase) {
		switch (phase) {
		case USER_INPUT:
			break;
		default:
			if (pinger != null)
				pinger.cancel();
		}
	}

	/**
	 * Get the current username.
	 * 
	 * @return The username
	 */
	public String getUsername() {
		return username.getText();
	}

	/**
	 * Get the current password.
	 * 
	 * @return The password
	 */
	public String getPassword() {
		return password.getText();
	}
}
