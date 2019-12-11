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

import java.util.Objects;

import com.sandpolis.viewer.jfx.common.controller.AbstractController;
import com.sandpolis.viewer.jfx.common.label.SvgLabel;

import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;

/**
 * A controller for all items in the configuration tree.
 *
 * @author cilki
 * @since 5.0.0
 */
public abstract class TreeItemController extends AbstractController {

	private TreeItem<Node> item;

	@FXML
	protected SvgLabel icon;

	@FXML
	protected Label name;

	public void setIcon(String image) {
		icon.svgProperty().set(image);
	}

	public StringProperty name() {
		return name.textProperty();
	}

	public void setItem(TreeItem<Node> item) {
		this.item = Objects.requireNonNull(item);
	}

	public TreeItem<Node> getItem() {
		return item;
	}

}
