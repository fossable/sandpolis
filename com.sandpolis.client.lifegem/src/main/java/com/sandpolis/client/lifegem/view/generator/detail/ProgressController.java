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

import com.google.common.eventbus.Subscribe;
import com.sandpolis.client.lifegem.common.controller.AbstractController;
import com.sandpolis.client.lifegem.view.generator.Events.GenerationCompletedEvent;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

public class ProgressController extends AbstractController {

	@FXML
	private ProgressBar progress;
	@FXML
	private Label text;
	@FXML
	private ButtonBar buttons;

	@FXML
	private void initialize() {
		buttons.setVisible(false);
	}

	@Subscribe
	public void completed(GenerationCompletedEvent event) {
		buttons.setVisible(true);

		var rs = event.get();
		if (rs.getReport().getResult()) {
			progress.setProgress(1.0);
			text.setText("Wrote " + rs.getReport().getOutputSize() + " bytes");
		} else {
			progress.setProgress(0.0);
		}
	}
}
