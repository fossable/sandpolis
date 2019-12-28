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
package com.sandpolis.viewer.jfx.view.login.phase;

import java.util.stream.Collectors;

import com.google.common.eventbus.Subscribe;
import com.sandpolis.core.proto.net.MsgPlugin.PluginDescriptor;
import com.sandpolis.viewer.jfx.common.controller.AbstractController;
import com.sandpolis.viewer.jfx.view.login.Events.PluginListEvent;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;

public class PluginPhaseController extends AbstractController {

	public static class PluginProperty {
		private final StringProperty description = new SimpleStringProperty(this, "description");
		private final BooleanProperty install = new SimpleBooleanProperty(this, "install");
		private final StringProperty name = new SimpleStringProperty(this, "name");
		private final StringProperty trust = new SimpleStringProperty(this, "trust");

		public PluginProperty(PluginDescriptor descriptor) {
			name.set(descriptor.getName());
			description.set(descriptor.getDescription());
			trust.set(descriptor.getTrustAuthority());
			install.set(false);
		}

		public StringProperty descriptionProperty() {
			return description;
		}

		public BooleanProperty installProperty() {
			return install;
		}

		public StringProperty nameProperty() {
			return name;
		}

		public StringProperty trustProperty() {
			return trust;
		}
	}

	@FXML
	private TableView<PluginProperty> plugins;

	@FXML
	private void initialize() {

		plugins.getColumns().get(3).setCellFactory(tc -> new CheckBoxTableCell<>());
	}

	public void install() {
		plugins.getItems().forEach(plugin -> {
			if (plugin.install.get()) {
				// TODO PluginCmd
			}
		});
	}

	@Subscribe
	void onEvent(PluginListEvent event) {
		plugins.getItems().setAll(event.get().stream().map(PluginProperty::new).collect(Collectors.toList()));
	}
}
