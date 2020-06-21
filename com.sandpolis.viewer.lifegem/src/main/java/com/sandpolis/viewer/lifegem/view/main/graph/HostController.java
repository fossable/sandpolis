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
package com.sandpolis.viewer.lifegem.view.main.graph;

import java.util.Objects;

import com.sandpolis.core.profile.store.Profile;
import com.sandpolis.viewer.lifegem.common.controller.AbstractController;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;

public class HostController extends AbstractController {

	@FXML
	private ImageView image;

	@FXML
	private BorderPane border;

	@FXML
	private Label undertext;

	private Profile profile;

	/**
	 * Whether the host is currently selected.
	 */
	private BooleanProperty selected = new SimpleBooleanProperty(false);

	@FXML
	private void initialize() {
		image.setOnMouseClicked(event -> {
			selected.set(!selected.get());
		});
	}

	public void setProfile(Profile profile) {
		this.profile = Objects.requireNonNull(profile);

		switch (profile.getInstanceType()) {
		case CLIENT:
			image.setImage(new Image("/image/icon32/common/viewer.png"));
		case SERVER:
			image.setImage(new Image("/image/icon32/common/server.png"));
		case VIEWER:
			image.setImage(new Image("/image/icon32/common/viewer.png"));
		default:
			throw new RuntimeException();
		}
	}

}
