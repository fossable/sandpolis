/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
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
