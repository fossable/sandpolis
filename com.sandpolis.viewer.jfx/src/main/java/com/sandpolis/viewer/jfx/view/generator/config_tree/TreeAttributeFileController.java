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
