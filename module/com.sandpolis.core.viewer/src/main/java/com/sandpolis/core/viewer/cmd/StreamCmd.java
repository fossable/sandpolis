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
package com.sandpolis.core.viewer.cmd;

import com.sandpolis.core.instance.Result.Outcome;
import com.sandpolis.core.net.MsgStream.RQ_ProfileStream;
import com.sandpolis.core.net.MsgStream.RQ_StreamStop;
import com.sandpolis.core.net.command.Cmdlet;
import com.sandpolis.core.net.future.ResponseFuture;
import com.sandpolis.core.util.IDUtil;

/**
 * Stream commands.
 *
 * @author cilki
 * @since 5.0.2
 */
public class StreamCmd extends Cmdlet<StreamCmd> {

	public ResponseFuture<Outcome> startProfileStream() {
		return request(RQ_ProfileStream.newBuilder().setId(IDUtil.stream()));
	}

	/**
	 * Stop the given stream.
	 *
	 * @param streamID The ID of the stream to stop
	 * @return A response future
	 */
	public ResponseFuture<Outcome> stop(int streamID) {
		return request(RQ_StreamStop.newBuilder().setId(streamID));
	}

	/**
	 * Prepare for an asynchronous command.
	 *
	 * @return A configurable object from which all asynchronous (nonstatic)
	 *         commands in {@link StreamCmd} can be invoked
	 */
	public static StreamCmd async() {
		return new StreamCmd();
	}

	private StreamCmd() {
	}
}
