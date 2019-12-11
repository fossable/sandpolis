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
package com.sandpolis.viewer.jfx.view.main.list;

import static com.sandpolis.core.profile.store.ProfileStore.ProfileStore;

import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.sandpolis.core.profile.AK_INSTANCE;
import com.sandpolis.core.profile.attribute.key.AttributeKey;
import com.sandpolis.core.profile.store.Profile;
import com.sandpolis.viewer.jfx.common.controller.AbstractController;
import com.sandpolis.viewer.jfx.view.main.Events.HostDetailCloseEvent;
import com.sandpolis.viewer.jfx.view.main.Events.HostDetailOpenEvent;
import com.sandpolis.viewer.jfx.view.main.Events.HostListHeaderChangeEvent;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;

public class HostListController extends AbstractController {

	private static final Logger log = LoggerFactory.getLogger(HostListController.class);

	/**
	 * Default list header types.
	 */
	private static final List<AttributeKey<?>> defaultHeaders = List.of(AK_INSTANCE.UUID, AK_INSTANCE.VERSION);

	@FXML
	private TableView<Profile> table;

	@FXML
	public void initialize() {
		if (ProfileStore.getContainer() == null)
			log.warn("The ProfileStore was not configured to expose the profile container!");

		table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		table.getSelectionModel().selectedItemProperty().addListener((p, o, n) -> {
			if (n == null)
				post(HostDetailCloseEvent::new);
			else
				post(HostDetailOpenEvent::new, n);
		});
		table.setItems((ObservableList<Profile>) ProfileStore.getContainer());

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
