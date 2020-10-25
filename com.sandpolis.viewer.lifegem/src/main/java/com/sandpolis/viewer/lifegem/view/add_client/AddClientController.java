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
package com.sandpolis.viewer.lifegem.view.add_client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.sandpolis.viewer.lifegem.common.FxUtil;
import com.sandpolis.viewer.lifegem.common.controller.FxController;
import com.sandpolis.viewer.lifegem.common.pane.ExtendPane;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.Region;

public class AddClientController extends FxController {

	@FXML
	private ExtendPane pane_extend;
	@FXML
	private ChoiceBox<String> chb_type;

	private Map<String, Region> dialogs;

	@FXML
	private void initialize() throws IOException {
		dialogs = new HashMap<>();
		dialogs.put("SNMP subagent", FxUtil.load("/fxml/view/add_client/Snmp.fxml", this));
		chb_type.getItems().addAll(dialogs.keySet());

		chb_type.valueProperty().addListener((p, o, n) -> {
			Region r = dialogs.get(n);
			pane_extend.raise(r, ExtendPane.ExtendSide.BOTTOM, 1000, (int) r.getPrefHeight());
		});
	}
}
