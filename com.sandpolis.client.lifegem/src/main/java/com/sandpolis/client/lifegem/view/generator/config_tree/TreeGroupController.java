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
package com.sandpolis.client.lifegem.view.generator.config_tree;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.StringProperty;

/**
 * A controller for group items.
 *
 * @author cilki
 * @since 5.0.0
 */
public class TreeGroupController extends TreeItemController {

	private List<TreeItemController> children = new ArrayList<>();

	public List<TreeItemController> getChildren() {
		return children;
	}

	/**
	 * Get the value of the item with the given ID.
	 *
	 * @param id The ID to find
	 * @return The value associated with the given ID
	 */
	public StringProperty getValueForId(String id) {
		for (var item : children) {
			if (item instanceof TreeAttributeController) {
				if (((TreeAttributeController) item).id().get().equals(id))
					return ((TreeAttributeController) item).value();
			}
		}
		return null;
	}

}
