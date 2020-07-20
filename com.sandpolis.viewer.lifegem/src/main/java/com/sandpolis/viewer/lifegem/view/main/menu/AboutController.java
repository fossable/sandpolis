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
import static com.sandpolis.viewer.lifegem.store.stage.StageStore.StageStore;

import java.io.IOException;

import com.sandpolis.viewer.lifegem.common.FxUtil;
import com.sandpolis.viewer.lifegem.common.controller.AbstractController;

import javafx.fxml.FXML;

public class AboutController extends AbstractController {

	@FXML
	public void open_about() throws IOException {
		StageStore.newStage().root("/fxml/view/about/About.fxml")
				.size(PrefStore.getInt("ui.view.about.width"), PrefStore.getInt("ui.view.about.height"))
				.title(FxUtil.translate("stage.about.title")).resizable(false).show();
	}
}
