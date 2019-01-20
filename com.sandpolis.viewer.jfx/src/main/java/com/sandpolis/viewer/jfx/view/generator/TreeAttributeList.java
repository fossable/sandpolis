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
package com.sandpolis.viewer.jfx.view.generator;

import java.util.Objects;

import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;

/**
 * Represents a list attribute cell in the configuration tree.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class TreeAttributeList extends TreeAttribute {

	private ComboBox<String> list = new ComboBox<>();

	public TreeAttributeList(String name) {
		super(name);

		// Setup value binding
		value().bind(list.valueProperty());

		// Add control
		control.setRight(list);
		list.setCellFactory(p -> new ListCell<String>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);

				if (empty) {
					// Clear contents
					setText(null);
					setGraphic(null);
					return;
				}

				// Parse option
				String[] parts = item.split(";");
				if (parts.length > 2)
					throw new RuntimeException("Invalid option: " + item);
				if (parts.length == 2) {
					setText(parts[1]);
					setGraphic(new ImageView("/image/icon16/" + parts[0]));
				} else {
					setText(parts[0]);
					setGraphic(null);
				}
			};
		});

		// Set the button to use the same renderer
		list.setButtonCell(list.getCellFactory().call(new ListView<>()));
	}

	/**
	 * Set the property's possible values.
	 * 
	 * @param options The property's values
	 * @return {@code this}
	 */
	public TreeAttributeList options(String... options) {
		this.list.getItems().addAll(Objects.requireNonNull(options));
		return this;
	}

	/**
	 * Set the current value.
	 * 
	 * @param value The new value
	 * @return {@code this}
	 */
	public TreeAttributeList value(String value) {
		this.list.setValue(value);
		return this;
	}
}