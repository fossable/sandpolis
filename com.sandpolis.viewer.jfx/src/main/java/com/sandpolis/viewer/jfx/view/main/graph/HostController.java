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
package com.sandpolis.viewer.jfx.view.main.graph;

import java.util.Objects;

import com.sandpolis.core.profile.Profile;
import com.sandpolis.viewer.jfx.common.controller.AbstractController;

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

		switch (profile.getInstance()) {
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
