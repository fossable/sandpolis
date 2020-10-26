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
package com.sandpolis.client.lifegem.view.main.list;

import static com.sandpolis.core.instance.state.InstanceOid.InstanceOid;
import static com.sandpolis.core.instance.state.STStore.STStore;
import static com.sandpolis.core.net.connection.ConnectionStore.ConnectionStore;
import static com.sandpolis.core.net.network.NetworkStore.NetworkStore;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.sandpolis.core.instance.state.oid.AbsoluteOid;
import com.sandpolis.core.net.state.STCmd;
import com.sandpolis.client.lifegem.common.controller.AbstractController;
import com.sandpolis.client.lifegem.state.FxCollection;
import com.sandpolis.client.lifegem.state.FxProfile;
import com.sandpolis.client.lifegem.view.main.Events.HostDetailCloseEvent;
import com.sandpolis.client.lifegem.view.main.Events.HostDetailOpenEvent;
import com.sandpolis.client.lifegem.view.main.Events.HostListHeaderChangeEvent;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;

public class HostListController extends AbstractController {

	private static final Logger log = LoggerFactory.getLogger(HostListController.class);

	@FXML
	private TableView<FxProfile> table;

	private FxCollection<FxProfile> collection = (FxCollection<FxProfile>) STStore.root().get(InstanceOid().profile);

	private static final List<AbsoluteOid.STAttributeOid<?>> DEFAULT_HEADERS = List.of(InstanceOid().profile.uuid,
			InstanceOid().profile.ip_address, InstanceOid().profile.instance_type);

	private String serverUuid;

	@FXML
	public void initialize() {

		table.setItems(collection.getObservable());
		table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		table.getSelectionModel().selectedItemProperty().addListener((p, o, n) -> {
			if (n == null)
				post(HostDetailCloseEvent::new);
			else
				post(HostDetailOpenEvent::new, n);
		});

		// Set default headers
		addColumns(DEFAULT_HEADERS);

		resync();
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
	private void addColumns(List<AbsoluteOid.STAttributeOid<?>> headers) {
		if (serverUuid == null) {
			table.getColumns().clear();
		} else {
			headers.stream().map(oid -> oid.resolveUuid(serverUuid)).map(AttributeColumn::new)
					.forEach(table.getColumns()::add);
		}
	}

	private void resync() {
		NetworkStore.getPreferredServer().ifPresentOrElse(cvid -> {
			serverUuid = ConnectionStore.getByCvid(cvid).get().getRemoteUuid();

			// Attach the local collection
			STCmd.async().sync(collection, InstanceOid().profile);
		}, () -> {
			serverUuid = null;
			table.setPlaceholder(new Label("Not connected to a server"));
		});
	}

}
