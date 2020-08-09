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

import static com.sandpolis.core.instance.pref.PrefStore.PrefStore;
import static com.sandpolis.viewer.lifegem.stage.StageStore.StageStore;

import java.io.IOException;

import com.sandpolis.viewer.lifegem.common.FxUtil;
import com.sandpolis.viewer.lifegem.common.controller.AbstractController;

import javafx.fxml.FXML;
import javafx.stage.StageStyle;

public class AboutController extends AbstractController {

	@FXML
	public void open_about() throws IOException {
		StageStore.create(stage -> {
			stage.initStyle(StageStyle.TRANSPARENT);
			stage.setRoot("/fxml/view/about/About.fxml");
			stage.setWidth(PrefStore.getInt("ui.view.about.width"));
			stage.setHeight(PrefStore.getInt("ui.view.about.height"));
			stage.setTitle(FxUtil.translate("stage.about.title"));
			stage.setResizable(false);
		});
	}
}
