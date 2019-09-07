/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.viewer.jfx.view.generator.detail;

import com.google.common.eventbus.Subscribe;
import com.sandpolis.viewer.jfx.common.controller.AbstractController;
import com.sandpolis.viewer.jfx.view.generator.Events.GenerationCompletedEvent;

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
