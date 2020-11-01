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
package com.sandpolis.client.lifegem.view.login.phase;

import com.sandpolis.client.lifegem.common.controller.AbstractController;
import com.sandpolis.client.lifegem.state.FxPlugin;
import com.sandpolis.core.clientagent.cmd.PluginCmd;
import com.sandpolis.core.instance.state.st.STDocument;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;

public class PluginPhaseController extends AbstractController {

	public static class PluginProperty extends FxPlugin {
		private final StringProperty description = new SimpleStringProperty(this, "description");
		private final BooleanProperty install = new SimpleBooleanProperty(this, "install");
		private final StringProperty trust = new SimpleStringProperty(this, "trust");

		public PluginProperty(STDocument document) {
			super(document);
		}

		public StringProperty descriptionProperty() {
			return description;
		}

		public BooleanProperty installProperty() {
			return install;
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
				PluginCmd.async().install(plugin.packageIdProperty().getValue());
			}
		});
	}

	public void setPlugins(ObservableList<PluginProperty> plugins) {
		this.plugins.setItems(plugins);
	}
}
