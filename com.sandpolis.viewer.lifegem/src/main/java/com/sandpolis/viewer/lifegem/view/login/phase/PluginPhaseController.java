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
package com.sandpolis.viewer.lifegem.view.login.phase;

import java.util.List;
import java.util.stream.Collectors;

import com.sandpolis.core.instance.Plugin.PluginDescriptor;
import com.sandpolis.core.viewer.cmd.PluginCmd;
import com.sandpolis.viewer.lifegem.common.controller.AbstractController;

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

		private final PluginDescriptor descriptor;

		public PluginProperty(PluginDescriptor descriptor) {
			this.descriptor = descriptor;

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
		plugins.setDisable(true);

		plugins.getItems().forEach(plugin -> {
			if (plugin.install.get()) {
				PluginCmd.async().install(plugin.descriptor.getCoordinate());
			}
		});
	}

	public void setPlugins(List<PluginDescriptor> newPlugins) {
		plugins.getItems().setAll(newPlugins.stream().map(PluginProperty::new).collect(Collectors.toList()));
	}
}
