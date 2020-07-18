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
package com.sandpolis.viewer.lifegem.view.login.phase;

import com.google.common.eventbus.Subscribe;
import com.sandpolis.core.instance.Core;
import com.sandpolis.core.foundation.util.ValidationUtil;
import com.sandpolis.viewer.lifegem.common.controller.AbstractController;
import com.sandpolis.viewer.lifegem.view.login.LoginEvents.ConnectEndedEvent;
import com.sandpolis.viewer.lifegem.view.login.LoginEvents.ConnectStartedEvent;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class ServerPhaseController extends AbstractController {

	@FXML
	private TextField address;
	@FXML
	private TextField port;

	/**
	 * Get the current address value.
	 *
	 * @return The address
	 */
	public String getAddress() {
		return address.getText();
	}

	/**
	 * Get the current port value.
	 *
	 * @return The port number
	 */
	public int getPort() {
		return Integer.parseInt(port.getText());
	}

	@FXML
	private void initialize() {

		// Set address filter
		address.textProperty().addListener((p, o, n) -> {
			// TODO filter invalid characters
		});

		// Set port filter
		port.textProperty().addListener((p, o, n) -> {
			if (!ValidationUtil.port(n) && !n.isEmpty())
				port.setText(o);
		});

		if (Core.SO_BUILD.getDevelopment()) {
			address.setText("127.0.0.1");
		}
	}

	@Subscribe
	void onEvent(ConnectStartedEvent event) {
		address.setDisable(true);
		port.setDisable(true);
	}

	@Subscribe
	void onEvent(ConnectEndedEvent event) {
		address.setDisable(false);
		port.setDisable(false);
	}

}
