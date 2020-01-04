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
package com.sandpolis.viewer.lifegem.view.main.menu;

import java.io.IOException;

import com.sandpolis.viewer.lifegem.common.FxUtil;
import com.sandpolis.viewer.lifegem.common.controller.AbstractController;
import com.sandpolis.viewer.lifegem.view.main.Events.MenuOpenEvent;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;

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

	private Region pane_interface;
	private Region pane_management;
	private Region pane_generator;
	private Region pane_configuration;
	private Region pane_about;

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
