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
