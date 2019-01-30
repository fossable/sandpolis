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

import java.io.IOException;

import com.sandpolis.viewer.jfx.common.FxUtil;
import com.sandpolis.viewer.jfx.common.controller.AbstractController;
import com.sandpolis.viewer.jfx.view.main.Events.MenuOpenEvent;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

public class VerticalMenuController extends AbstractController {

	@FXML
	private Button btn_interface;
	@FXML
	private Button btn_management;
	@FXML
	private Button btn_generator;
	@FXML
	private Button btn_configuration;
	@FXML
	private Button btn_about;

	private VBox pane_interface;
	private VBox pane_management;
	private VBox pane_generator;
	private VBox pane_configuration;
	private VBox pane_about;

	@FXML
	public void initialize() throws IOException {
		pane_interface = FxUtil.load("/fxml/view/main/menu/Interface.fxml", this);
		pane_management = FxUtil.load("/fxml/view/main/menu/Management.fxml", this);
		pane_generator = FxUtil.load("/fxml/view/main/menu/Generator.fxml", this);
		pane_configuration = FxUtil.load("/fxml/view/main/menu/Configuration.fxml", this);
		pane_about = FxUtil.load("/fxml/view/main/menu/About.fxml", this);
	}

	@FXML
	public void btn_interface(ActionEvent event) {
		post(MenuOpenEvent::new, pane_interface);
	}

	@FXML
	public void btn_management(ActionEvent event) {
		post(MenuOpenEvent::new, pane_management);
	}

	@FXML
	public void btn_generator(ActionEvent event) {
		post(MenuOpenEvent::new, pane_generator);
	}

	@FXML
	public void btn_configuration(ActionEvent event) {
		post(MenuOpenEvent::new, pane_configuration);
	}

	@FXML
	public void btn_about(ActionEvent event) {
		post(MenuOpenEvent::new, pane_about);
	}

}
