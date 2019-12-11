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

import com.sandpolis.core.util.ValidationUtil;
import com.sandpolis.viewer.jfx.common.controller.AbstractController;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class ServerPhaseController extends AbstractController {

	@FXML
	private TextField address;
	@FXML
	private TextField port;

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
	}

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

}
