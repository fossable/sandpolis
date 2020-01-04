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
package com.sandpolis.viewer.lifegem.view.generator.config_tree;

import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

/**
 * A controller for items that have a textfield attribute.
 *
 * @author cilki
 * @since 5.0.0
 */
public class TreeAttributeTextController extends TreeAttributeController {

	@FXML
	protected TextField text;

	@Override
	public StringProperty value() {
		return text.textProperty();
	}
}
