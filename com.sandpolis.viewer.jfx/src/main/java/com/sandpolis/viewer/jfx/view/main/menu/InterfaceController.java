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
package com.sandpolis.viewer.jfx.view.main.menu;

import java.awt.AWTException;

import com.sandpolis.viewer.jfx.common.controller.AbstractController;
import com.sandpolis.viewer.jfx.common.tray.Tray;
import com.sandpolis.viewer.jfx.view.main.Events.AuxDetailOpenEvent;
import com.sandpolis.viewer.jfx.view.main.Events.ViewChangeEvent;

import javafx.fxml.FXML;

public class InterfaceController extends AbstractController {

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
		System.exit(0);
	}

}
