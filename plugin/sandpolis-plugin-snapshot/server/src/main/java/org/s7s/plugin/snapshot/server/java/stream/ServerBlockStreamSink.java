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
import org.s7s.core.instance.stream.StreamSink;
import org.s7s.plugin.snapshot.Messages.EV_SnapshotDataBlock;

public class ServerBlockStreamSink extends StreamSink<EV_SnapshotDataBlock> {

	private Qcow2 container;

	@Override
	public void onNext(EV_SnapshotDataBlock item) {
		container.write(item.getData().asReadOnlyByteBuffer());
	}

	@Override
	public void close() {
		super.close();
		// container.close();
	}
}
