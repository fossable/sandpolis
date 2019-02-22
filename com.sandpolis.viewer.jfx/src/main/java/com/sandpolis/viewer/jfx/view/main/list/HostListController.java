/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
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
package com.sandpolis.viewer.jfx.view.main.list;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.google.common.eventbus.Subscribe;
import com.sandpolis.core.attribute.AttributeKey;
import com.sandpolis.core.attribute.key.AK_INSTANCE;
import com.sandpolis.core.profile.Profile;
import com.sandpolis.viewer.jfx.common.controller.AbstractController;
import com.sandpolis.viewer.jfx.view.main.Events.HostDetailCloseEvent;
import com.sandpolis.viewer.jfx.view.main.Events.HostDetailOpenEvent;
import com.sandpolis.viewer.jfx.view.main.Events.HostListHeaderChangeEvent;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;

public class HostListController extends AbstractController {

	// TODO move
	private static ObservableList<Profile> list;

	public static void setList(ObservableList<Profile> list) {
		HostListController.list = Objects.requireNonNull(list);
	}

	/**
	 * Default list header types.
	 */
	private static final List<AttributeKey<?>> defaultHeaders = List.of(AK_INSTANCE.UUID, AK_INSTANCE.VERSION);

	@FXML
	private TableView<Profile> table;

	@FXML
	public void initialize() {
		table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		table.getSelectionModel().selectedItemProperty().addListener((p, o, n) -> {
			if (n == null)
				post(HostDetailCloseEvent::new);
			else
				post(HostDetailOpenEvent::new, n);
		});
		table.setItems(list);

		// Set default headers
		addColumns(defaultHeaders.stream());
	}

	/**
	 * Change host table columns to the given set.
	 * 
	 * @param event The change event which contains the desired headers
	 */
	@Subscribe
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void changeColumns(HostListHeaderChangeEvent event) {
		List<AttributeColumn> columns = (List) table.getColumns();

		// Remove current columns that are not in the new set
		columns.removeIf(column -> !event.get().contains(column.getKey()));

		// Add new columns that are not in the current set
		addColumns(event.get().stream()
				.filter(header -> !columns.stream().anyMatch(column -> header.equals(column.getKey()))));
	}

	/**
	 * Add columns to the host table.
	 * 
	 * @param headers A {@link Stream} of headers to add
	 */
	private void addColumns(Stream<AttributeKey<?>> headers) {
		headers.map(AttributeColumn::new).forEach(table.getColumns()::add);
	}

}
