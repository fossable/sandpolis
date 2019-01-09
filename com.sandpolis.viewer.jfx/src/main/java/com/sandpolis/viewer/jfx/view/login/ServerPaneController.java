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

import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.InetAddressValidator;

import com.sandpolis.core.util.ValidationUtil;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class ServerPaneController {

	@FXML
	private TextField address;
	@FXML
	private TextField port;

	@FXML
	private void initialize() {

		// Set address filter
		// TODO

		// Set port filter
		port.textProperty().addListener((p, o, n) -> {
			if (!ValidationUtil.port(n) && !n.isEmpty())
				port.setText(o);
		});

	}

	public boolean checkInput() {
		if (!ValidationUtil.port(port.getText())) {
			// TODO visual errors
			return false;
		}

		if (!DomainValidator.getInstance().isValid(address.getText())
				&& !InetAddressValidator.getInstance().isValid(address.getText())) {
			// TODO visual errors
			return false;
		}

		return true;
	}

	public void setEnabled(boolean enabled) {
		address.setDisable(!enabled);
		port.setDisable(!enabled);
	}

	public String getAddress() {
		return address.getText();
	}

	public int getPort() {
		return Integer.parseInt(port.getText());
	}
}
