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

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AboutController extends AbstractController {

	@FXML
	public void open_about() throws IOException {
		Stage stage = new Stage();
		stage.setScene(new Scene(FxUtil.loadRoot("/fxml/view/about/About.fxml"), 420, 380));
		stage.show();
	}
}
