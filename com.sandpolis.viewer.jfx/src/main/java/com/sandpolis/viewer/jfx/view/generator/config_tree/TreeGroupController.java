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
