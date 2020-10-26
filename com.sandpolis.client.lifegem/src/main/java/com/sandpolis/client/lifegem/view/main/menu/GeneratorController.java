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
package com.sandpolis.client.lifegem.view.main.menu;

import static com.sandpolis.core.instance.Generator.OutputPayload.OUTPUT_MEGA;
import static com.sandpolis.core.instance.Generator.OutputPayload.OUTPUT_MICRO;
import static com.sandpolis.core.instance.pref.PrefStore.PrefStore;
import static com.sandpolis.client.lifegem.stage.StageStore.StageStore;

import java.io.IOException;

import com.sandpolis.client.lifegem.common.FxUtil;
import com.sandpolis.client.lifegem.common.controller.AbstractController;

import javafx.fxml.FXML;

public class GeneratorController extends AbstractController {

	@FXML
	private void open_history() throws IOException {
		// TODO
	}

	@FXML
	private void open_mega() throws IOException {

		StageStore.create(stage -> {
			stage.setRoot("/fxml/view/generator/Generator.fxml", OUTPUT_MEGA);
			stage.setWidth(PrefStore.getInt("ui.view.generator.width"));
			stage.setHeight(PrefStore.getInt("ui.view.generator.height"));
			stage.setTitle(FxUtil.translate("stage.generator.title"));
		});
	}

	@FXML
	private void open_micro() throws IOException {

		StageStore.create(stage -> {
			stage.setRoot("/fxml/view/generator/Generator.fxml", OUTPUT_MICRO);
			stage.setWidth(PrefStore.getInt("ui.view.generator.width"));
			stage.setHeight(PrefStore.getInt("ui.view.generator.height"));
			stage.setTitle(FxUtil.translate("stage.generator.title"));
		});
	}
}
