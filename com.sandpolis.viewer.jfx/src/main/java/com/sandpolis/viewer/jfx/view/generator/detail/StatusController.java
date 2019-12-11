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
package com.sandpolis.viewer.jfx.view.generator.detail;

import com.google.common.eventbus.Subscribe;
import com.sandpolis.viewer.jfx.common.controller.AbstractController;
import com.sandpolis.viewer.jfx.view.generator.Events.OutputFormatChangedEvent;
import com.sandpolis.viewer.jfx.view.generator.Events.OutputLocationChangedEvent;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class StatusController extends AbstractController {

	@FXML
	private Label output_location;
	@FXML
	private Label output_type;
	@FXML
	private Label output_size;

	@Subscribe
	private void locationChanged(OutputLocationChangedEvent event) {
		output_location.setText(event.get());
	}

	@Subscribe
	private void formatChanged(OutputFormatChangedEvent event) {
		output_type.setText(event.get());
	}

}
