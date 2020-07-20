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

import com.sandpolis.viewer.lifegem.common.controller.AbstractController;

import javafx.fxml.FXML;

public class ManagementController extends AbstractController {

	@FXML
	public void open_network() {
		StageStore.newStage().root("/fxml/view/network/Network.fxml")
				.size(PrefStore.getInt("ui.view.about.width"), PrefStore.getInt("ui.view.about.height"))
				.title("Network").resizable(true).show();
	}

	@FXML
	public void open_add_client() {
		StageStore.newStage().root("/fxml/view/add_client/AddClient.fxml")
				.size(PrefStore.getInt("ui.view.about.width"), PrefStore.getInt("ui.view.about.height"))
				.title("Add Client").resizable(true).show();
	}

}
