/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
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
package com.sandpolis.viewer.jfx.view.main.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fxgraph.graph.Graph;
import com.fxgraph.graph.Model;
import com.sandpolis.core.instance.Signaler;
import com.sandpolis.core.net.store.connection.ConnectionStore.Events;
import com.sandpolis.core.net.store.network.NetworkStore;
import com.sandpolis.core.profile.ProfileStore;
import com.sandpolis.viewer.jfx.common.controller.AbstractController;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;

public class HostGraphController extends AbstractController {

	private static final Logger log = LoggerFactory.getLogger(HostGraphController.class);

	@FXML
	private BorderPane pane;

	private Graph graph;

	@FXML
	public void initialize() {
		graph = new Graph();
		pane.setCenter(graph.getCanvas());

		Model model = graph.getModel();

		// Load the network's current state
		graph.beginUpdate();
		for (int cvid : NetworkStore.getNetwork().nodes()) {
			ProfileStore.getProfile(cvid).ifPresentOrElse(profile -> {
				model.addCell(HostCell.build(profile));
			}, () -> {
				log.warn("No profile found for: {}", cvid);
			});
		}
		graph.endUpdate();

		// Setup network change listeners
		Signaler.register(Events.SOCK_LOST, sock -> {
			Platform.runLater(() -> {
				graph.beginUpdate();
				// TODO condition
				graph.getModel().getAllEdges().removeIf(edge -> true);
				graph.endUpdate();
			});
		});
		Signaler.register(Events.SOCK_ESTABLISHED, sock -> {
			Platform.runLater(() -> {
				// TODO
			});
		});
	}

}