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
package com.sandpolis.viewer.jfx.view.main.menu;

import java.awt.AWTException;

import com.sandpolis.viewer.jfx.common.button.SvgButton;
import com.sandpolis.viewer.jfx.common.controller.AbstractController;
import com.sandpolis.viewer.jfx.common.tray.Tray;
import com.sandpolis.viewer.jfx.view.main.Events.AuxDetailOpenEvent;
import com.sandpolis.viewer.jfx.view.main.Events.ViewChangeEvent;

import javafx.application.Platform;
import javafx.fxml.FXML;

public class InterfaceController extends AbstractController {

	@FXML
	private SvgButton btn_background;

	@FXML
	private void initialize() {
		btn_background.setDisable(!Tray.isSupported());
	}

	@FXML
	private void open_list() {
		post(ViewChangeEvent::new, "list");
	}

	@FXML
	private void open_graph() {
		post(ViewChangeEvent::new, "graph");
	}

	@FXML
	private void open_console() {
		post(AuxDetailOpenEvent::new, "console");
	}

	@FXML
	private void open_status() {
		post(AuxDetailOpenEvent::new, "status");
	}

	@FXML
	private void background() throws AWTException {
		Tray.background();
	}

	@FXML
	private void quit() {
		Platform.exit();
		System.exit(0);
	}

}
