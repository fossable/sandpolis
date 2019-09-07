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
package com.sandpolis.viewer.jfx.view.generator.config_tree;

import java.io.File;
import java.util.Objects;
import java.util.function.Function;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * A controller for items that have a file attribute.
 *
 * @author cilki
 * @since 5.0.0
 */
public class TreeAttributeFileController extends TreeAttributeTextController {

	@FXML
	protected Button select;

	/**
	 * A function that translates files to install paths.
	 */
	private Function<File, String> fileMapper = file -> file.getAbsolutePath();

	@FXML
	private void select() {
		select.setDisable(true);
		FileChooser fc = new FileChooser();
		File file = fc.showSaveDialog(new Stage());

		text.setText(fileMapper.apply(file));
		select.setDisable(false);
	}

	public void setFileMapper(Function<File, String> fileMapper) {
		this.fileMapper = Objects.requireNonNull(fileMapper);
	}

}
