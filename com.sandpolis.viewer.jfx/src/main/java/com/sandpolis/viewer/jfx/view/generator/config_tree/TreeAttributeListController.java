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

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;

/**
 * A controller for items that have a list attribute.
 *
 * @author cilki
 * @since 5.0.0
 */
public class TreeAttributeListController extends TreeAttributeController {

	@FXML
	private ComboBox<String> list;

	private StringProperty value = new SimpleStringProperty();

	@FXML
	@Override
	protected void initialize() {
		super.initialize();

		// Define value
		value().bindBidirectional(list.valueProperty());

		list.setCellFactory(p -> new ListCell<String>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);

				if (empty) {
					setText(null);
					setGraphic(null);
				} else {
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
				}
			};
		});

		// Set the button to use the same renderer
		list.setButtonCell(list.getCellFactory().call(new ListView<>()));
	}

	@Override
	public StringProperty value() {
		return value;
	}

	public ObservableList<String> getItems() {
		return list.getItems();
	}
}
