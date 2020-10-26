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
package com.sandpolis.client.lifegem.view.add_client;

import com.sandpolis.client.lifegem.common.controller.AbstractController;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

public class AddSnmpClientController extends AbstractController {

	@FXML
	private VBox vbox_root;
	@FXML
	private ChoiceBox<String> chb_snmp_version;
	@FXML
	private TitledPane pane_snmpv3;
	@FXML
	private TextField fld_snmpv3_security;
	@FXML
	private ChoiceBox<String> chb_snmpv3_auth_protocol;
	@FXML
	private ChoiceBox<String> chb_snmpv3_priv_protocol;
	@FXML
	private TextField fld_snmpv3_auth_passphrase;
	@FXML
	private TextField fld_snmpv3_priv_passphrase;
	@FXML
	private TitledPane pane_snmpv2c;
	@FXML
	private TextField fld_snmpv2c_community;

	@FXML
	private void initialize() {
		vbox_root.getChildren().removeAll(pane_snmpv3, pane_snmpv2c);

		chb_snmp_version.getItems().addAll("SNMPv3", "SNMPv2c");
		chb_snmp_version.valueProperty().addListener((p, o, n) -> {
			switch (n) {
			case "SNMPv3":
				vbox_root.getChildren().removeAll(pane_snmpv3, pane_snmpv2c);
				vbox_root.getChildren().add(pane_snmpv3);
				break;
			case "SNMPv2c":
				vbox_root.getChildren().removeAll(pane_snmpv3, pane_snmpv2c);
				vbox_root.getChildren().add(pane_snmpv2c);
				break;
			}
		});
		chb_snmp_version.setValue("SNMPv3");

		chb_snmpv3_auth_protocol.getItems().addAll("MD5", "SHA", "SHA-224", "SHA-256", "SHA-384", "SHA-512");
		chb_snmpv3_auth_protocol.setValue("SHA");
		chb_snmpv3_priv_protocol.getItems().addAll("AES", "DES");
		chb_snmpv3_priv_protocol.setValue("AES");
	}
}
