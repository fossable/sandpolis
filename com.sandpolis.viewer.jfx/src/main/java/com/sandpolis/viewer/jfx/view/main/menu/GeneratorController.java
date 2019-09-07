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
package com.sandpolis.viewer.jfx.view.main.menu;

import static com.sandpolis.core.instance.store.pref.PrefStore.PrefStore;
import static com.sandpolis.core.proto.util.Generator.OutputPayload.OUTPUT_MEGA;
import static com.sandpolis.core.proto.util.Generator.OutputPayload.OUTPUT_MICRO;
import static com.sandpolis.viewer.jfx.store.stage.StageStore.StageStore;

import java.io.IOException;

import com.sandpolis.viewer.jfx.PrefConstant.ui;
import com.sandpolis.viewer.jfx.common.FxUtil;
import com.sandpolis.viewer.jfx.common.controller.AbstractController;

import javafx.fxml.FXML;

public class GeneratorController extends AbstractController {

	@FXML
	private void open_history() throws IOException {
		// TODO
	}

	@FXML
	private void open_mega() throws IOException {
		StageStore.newStage().root("/fxml/view/generator/Generator.fxml", OUTPUT_MEGA)
				.size(PrefStore.getInt(ui.view.generator.width), PrefStore.getInt(ui.view.generator.height))
				.title(FxUtil.translate("stage.generator.title")).show();
	}

	@FXML
	private void open_micro() throws IOException {
		StageStore.newStage().root("/fxml/view/generator/Generator.fxml", OUTPUT_MICRO)
				.size(PrefStore.getInt(ui.view.generator.width), PrefStore.getInt(ui.view.generator.height))
				.title(FxUtil.translate("stage.generator.title")).show();
	}
}
