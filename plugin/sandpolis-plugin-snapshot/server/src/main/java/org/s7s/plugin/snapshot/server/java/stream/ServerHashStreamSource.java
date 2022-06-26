//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.plugin.snapshot.server.java.stream;

import org.s7s.core.integration.qcow2.Qcow2;
import org.s7s.core.instance.stream.StreamSource;
import org.s7s.plugin.snapshot.Messages.EV_SnapshotHashBlock;

public class ServerHashStreamSource extends StreamSource<EV_SnapshotHashBlock> {

	private Qcow2 container;

	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

}
