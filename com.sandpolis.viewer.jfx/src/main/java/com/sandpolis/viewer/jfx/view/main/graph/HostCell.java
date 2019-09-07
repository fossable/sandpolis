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

import java.io.IOException;

import com.fxgraph.cells.AbstractCell;
import com.fxgraph.graph.Graph;
import com.sandpolis.core.profile.Profile;
import com.sandpolis.viewer.jfx.common.FxUtil;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Region;

public class HostCell extends AbstractCell {

	private Region graphic;

	private HostController host;

	public static HostCell build(Profile profile) {
		FXMLLoader loader = new FXMLLoader(FxUtil.class.getResource("/fxml/view/main/graph/Host.fxml"));
		var cell = new HostCell();

		try {
			cell.graphic = loader.load();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		cell.host = loader.getController();
		cell.host.setProfile(profile);
		return cell;
	}

	@Override
	public Region getGraphic(Graph graph) {
		return graphic;
	}

}
