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
package com.sandpolis.client.lifegem.view.generator.detail;

import static com.sandpolis.core.instance.thread.ThreadStore.ThreadStore;

import java.util.concurrent.ExecutorService;

import com.sandpolis.core.net.util.DnsUtil;
import com.sandpolis.core.foundation.util.NetUtil;
import com.sandpolis.core.foundation.util.ValidationUtil;
import com.sandpolis.client.lifegem.common.controller.AbstractController;
import com.sandpolis.client.lifegem.view.generator.Events.AddServerEvent;
import com.sandpolis.client.lifegem.view.generator.Events.DetailCloseEvent;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class AddServerController extends AbstractController {

	@FXML
	private TextField fld_address;
	@FXML
	private TextField fld_port;
	@FXML
	private Button btn_test;
	@FXML
	private Spinner<Integer> spn_cooldown;
	@FXML
	private CheckBox chk_certificates;
	@FXML
	private HBox test_box;
	@FXML
	private ProgressIndicator test_progress;
	@FXML
	private Label test_status;

	@FXML
	private void test_address() {

		// Retrieve values
		String address = fld_address.getText();
		String port = fld_port.getText();

		// Validate
		if (!ValidationUtil.address(address)) {
			test_status.setText("Invalid address");
			return;
		}
		if (!ValidationUtil.port(port) && !port.isEmpty()) {
			test_status.setText("Invalid port");
			return;
		}

		test_box.setVisible(true);
		test_progress.setProgress(-1);

		Task<Boolean> portCheck = new Task<>() {

			@Override
			protected Boolean call() throws Exception {
				int p = Integer.parseInt(port);
				// DNS query if port is missing
				if (port.isEmpty()) {
					try {
						p = DnsUtil.getPort(address).get();
					} catch (Exception e) {
						return false;
					}
				}

				return NetUtil.checkPort(address, p);
			}
		};

		portCheck.setOnSucceeded(event -> {
			if (portCheck.getValue())
				test_status.setText("Connection succeeded!");
			else
				test_status.setText("Connection failed!");

			fld_address.setDisable(false);
			fld_port.setDisable(false);
			btn_test.setDisable(false);
		});

		test_status.setText("Checking connection");
		fld_address.setDisable(true);
		fld_port.setDisable(true);
		btn_test.setDisable(true);

		ExecutorService executor = ThreadStore.get("net.connection.outgoing");
		executor.submit(portCheck);

	}

	@FXML
	private void add_server() {

		String address = fld_address.getText();
		String port = fld_port.getText();
		String cooldown = "" + spn_cooldown.getValue();
		Boolean strict_certs = chk_certificates.isSelected();

		// Validate input
		if (!ValidationUtil.address(address)) {
			// TODO
			return;
		}
		if (!ValidationUtil.port(port)) {
			// TODO
			return;
		}

		post(AddServerEvent::new, new AddServerEvent.Container(address, port, strict_certs.toString(), cooldown));
		post(DetailCloseEvent::new);
	}

	@FXML
	private void cancel(ActionEvent event) {
		post(DetailCloseEvent::new);
	}

}
