//============================================================================//
//                                                                            //
//                         Copyright © 2015 Sandpolis                         //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation.                                   //
//                                                                            //
//============================================================================//
package com.sandpolis.core.client.cmd;

import java.util.concurrent.CompletionStage;

import com.sandpolis.core.foundation.Result.Outcome;
import com.sandpolis.core.net.cmdlet.Cmdlet;
import com.sandpolis.core.net.msg.MsgStream.RQ_StreamStop;

/**
 * An API for interacting with streams on the server.
 *
 * @author cilki
 * @since 5.0.2
 */
public class StreamCmd extends Cmdlet<StreamCmd> {

	/**
	 * Stop a given stream.
	 *
	 * @param streamID The ID of the stream to stop
	 * @return An asynchronous {@link CompletionStage}
	 */
	public CompletionStage<Outcome> stop(int streamID) {
		return request(Outcome.class, RQ_StreamStop.newBuilder().setId(streamID));
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
