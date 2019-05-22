/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
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
