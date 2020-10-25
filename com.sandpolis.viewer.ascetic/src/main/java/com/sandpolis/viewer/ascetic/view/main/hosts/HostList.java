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
package com.sandpolis.viewer.ascetic.view.main.hosts;

import static com.sandpolis.core.instance.state.InstanceOid.InstanceOid;
import static com.sandpolis.core.instance.state.STStore.STStore;
import static com.sandpolis.core.net.connection.ConnectionStore.ConnectionStore;
import static com.sandpolis.core.net.network.NetworkStore.NetworkStore;

import com.googlecode.lanterna.gui2.table.Table;
import com.sandpolis.core.instance.state.st.STCollection;
import com.sandpolis.core.net.state.STCmd;

public class HostList extends Table<String> {

	private STCollection collection = STStore.root().get(InstanceOid().profile);

	private String serverUuid;

	public HostList() {
		super("UUID", "Hostname", "IP Address", "Platform");

		NetworkStore.getPreferredServer().ifPresentOrElse(cvid -> {
			serverUuid = ConnectionStore.getByCvid(cvid).get().getRemoteUuid();

			// Attach the local collection
			STCmd.async().sync(collection, InstanceOid().profile);
		}, () -> {
			serverUuid = null;
		});
	}
}
