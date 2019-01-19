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
package com.sandpolis.viewer.jfx.view.generator.detail;

import java.util.stream.Stream;

import com.sandpolis.core.util.ValidationUtil;
import com.sandpolis.viewer.jfx.common.controller.AbstractController;
import com.sandpolis.viewer.jfx.view.generator.Events.AddServerEvent;
import com.sandpolis.viewer.jfx.view.generator.Events.DetailCloseEvent;
import com.sandpolis.viewer.jfx.view.generator.Item.ConfigGroup;
import com.sandpolis.viewer.jfx.view.generator.Item.ConfigPropertyList;
import com.sandpolis.viewer.jfx.view.generator.Item.ConfigPropertyText;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;

public class AddServerController extends AbstractController {

	@FXML
	private TextField fld_address;
	@FXML
	private TextField fld_port;
	@FXML
	private Spinner<Integer> spn_cooldown;
	@FXML
	private CheckBox chk_certificates;

	@FXML
	private void test_address(ActionEvent event) {

		// Retrieve values
		String address = fld_address.getText();
		String port = fld_port.getText();

		// Validate
		if (!ValidationUtil.address(address)) {
			// TODO
		}
		if (!ValidationUtil.port(port)) {
			// TODO
		}

		// TODO DNS query and make temporary connection
	}

	@FXML
	private void add_server(ActionEvent event) {

		// Retrieve values
		String address = fld_address.getText();
		String port = fld_port.getText();
		Integer cooldown = spn_cooldown.getValue();
		Boolean certificates = chk_certificates.isSelected();

		// Validate
		if (!ValidationUtil.address(address)) {
			// TODO
		}
		if (!ValidationUtil.port(port)) {
			// TODO
		}

		// Add the server to the configuration tree
		var group = new TreeItem<>(new ConfigGroup(address).icon("server.png"));

		var a = new ConfigPropertyText("address").validator(ValidationUtil::address).value(address);
		var p = new ConfigPropertyText("port").validator(ValidationUtil::port).value(port).icon("ip.png");
		var c = new ConfigPropertyList("certificates").options("true", "false").value(certificates.toString())
				.icon("ssl_certificate.png");
		var cc = new ConfigPropertyText("connection cooldown").value(cooldown.toString());

		Stream.of(a, p, c, cc).map(TreeItem::new).forEach(group.getChildren()::add);

		// Bind address to group name
		group.getValue().name().bind(a.value());

		post(AddServerEvent::new, group);
		post(DetailCloseEvent::new);
	}

	@FXML
	private void cancel(ActionEvent event) {
		post(DetailCloseEvent::new);
	}

}
