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
package com.sandpolis.viewer.lifegem.view.main.list;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.sandpolis.core.instance.StateTree.VirtProfile;
import com.sandpolis.core.instance.state.Oid.AttributeOid;
import com.sandpolis.core.net.state.STCmd;
import com.sandpolis.viewer.lifegem.StateTree.FxProfile;
import com.sandpolis.viewer.lifegem.common.controller.AbstractController;
import com.sandpolis.viewer.lifegem.state.FxCollection;
import com.sandpolis.viewer.lifegem.view.main.Events.HostDetailCloseEvent;
import com.sandpolis.viewer.lifegem.view.main.Events.HostDetailOpenEvent;
import com.sandpolis.viewer.lifegem.view.main.Events.HostListHeaderChangeEvent;

import javafx.fxml.FXML;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;

public class HostListController extends AbstractController {

	private static final Logger log = LoggerFactory.getLogger(HostListController.class);

	@FXML
	private TableView<FxProfile> table;

	private static final List<AttributeOid<?>> DEFAULT_HEADERS = List.of(VirtProfile.UUID);

	@FXML
	public void initialize() {

		table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		table.getSelectionModel().selectedItemProperty().addListener((p, o, n) -> {
			if (n == null)
				post(HostDetailCloseEvent::new);
			else
				post(HostDetailOpenEvent::new, n);
		});

		STCmd.async().sync(VirtProfile.COLLECTION).thenAccept(snapshot -> {
			var collection = new FxCollection<>(snapshot, FxProfile::new);
			this.table.setItems(collection);
		});

		// Set default headers
		addColumns(DEFAULT_HEADERS);
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
		columns.removeIf(column -> !event.get().contains(column.getOid()));

		// Add new columns that are not in the current set
		addColumns(event.get().stream()
				.filter(header -> !columns.stream().anyMatch(column -> header.equals(column.getOid())))
				.collect(Collectors.toList()));
	}

	/**
	 * Add columns to the host table.
	 *
	 * @param headers The headers to add
	 */
	private void addColumns(List<AttributeOid<?>> headers) {
		headers.stream().map(AttributeColumn::new).forEach(table.getColumns()::add);
	}

}
