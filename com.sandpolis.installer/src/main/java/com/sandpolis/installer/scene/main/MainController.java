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
package com.sandpolis.installer.scene.main;

import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;

public class MainController {

	@FXML
	private CheckBox chk_server;

	@FXML
	private CheckBox chk_viewer_jfx;

	@FXML
	private CheckBox chk_viewer_cli;

	@FXML
	private Button btn_install;

	@FXML
	private BorderPane pane;

	@FXML
	private void initialize() {
		chk_server.selectedProperty().addListener(this::refresh);
		chk_viewer_jfx.selectedProperty().addListener(this::refresh);
		chk_viewer_cli.selectedProperty().addListener(this::refresh);
	}

	/**
	 * Refresh the scene's state.
	 */
	private void refresh(ObservableValue<?> p, boolean o, boolean n) {
		// Ensure at least one box is checked
		btn_install
				.setDisable(!chk_server.isSelected() && !chk_viewer_jfx.isSelected() && !chk_viewer_cli.isSelected());
	}

	@FXML
	private void install() {

		// Replace buttons with a progressbar
		ProgressBar progress = new ProgressBar(0);
		pane.setBottom(progress);

		new Thread(new Task<Boolean>() {

			{
				setOnSucceeded(event -> {
				});
			}

			@Override
			public Boolean call() throws Exception {
				return false;
			}

		}).start();
	}
}
