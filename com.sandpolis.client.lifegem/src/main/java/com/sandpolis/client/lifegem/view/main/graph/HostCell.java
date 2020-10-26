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
package com.sandpolis.client.lifegem.view.main.graph;

import java.io.IOException;

import com.fxgraph.cells.AbstractCell;
import com.fxgraph.graph.Graph;
import com.sandpolis.core.instance.profile.Profile;
import com.sandpolis.client.lifegem.common.FxUtil;

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
