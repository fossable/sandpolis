//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.plugin.snapshot.server.java.stream;

import java.util.concurrent.BlockingQueue;

import org.s7s.core.instance.stream.StreamSink;
import org.s7s.plugin.snapshot.Messages.EV_SnapshotHashBlock;

public class ServerHashStreamSink extends StreamSink<EV_SnapshotHashBlock> {

	BlockingQueue<EV_SnapshotHashBlock> queue;

	@Override
	public void onNext(EV_SnapshotHashBlock item) {
		queue.add(item);
	}

}
