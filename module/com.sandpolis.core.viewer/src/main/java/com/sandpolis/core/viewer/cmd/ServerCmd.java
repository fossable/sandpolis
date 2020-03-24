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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.sandpolis.core.net.Message.MSG;
import com.sandpolis.core.net.MsgPing.RQ_Ping;
import com.sandpolis.core.net.MsgServer.RQ_ServerBanner;
import com.sandpolis.core.net.MsgServer.RS_ServerBanner;
import com.sandpolis.core.net.command.Cmdlet;
import com.sandpolis.core.net.future.ResponseFuture;

/**
 * Contains server commands.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class ServerCmd extends Cmdlet<ServerCmd> {

	public ResponseFuture<RS_ServerBanner> getServerBanner() {
		return request(RQ_ServerBanner.newBuilder());
	}

	/**
	 * Estimate the link latency by measuring how long it takes to receive a
	 * message.
	 *
	 * @return The approximate time for a message to travel to the remote host in
	 *         milliseconds
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 */
	// TODO not async
	public long ping() throws InterruptedException, ExecutionException, TimeoutException {
		long t1 = System.nanoTime();
		sock.request(MSG.newBuilder().setRqPing(RQ_Ping.newBuilder())).get(2000, TimeUnit.MILLISECONDS);
		long t2 = System.nanoTime();

		// To get from 1e9 to (1e3)/2, multiply by (1e-6)/2 = 1/2000000
		return (t2 - t1) / 2000000;
	}

	/**
	 * Prepare for an asynchronous command.
	 *
	 * @return A configurable object from which all asynchronous (nonstatic)
	 *         commands in {@link ServerCmd} can be invoked
	 */
	public static ServerCmd async() {
		return new ServerCmd();
	}

	private ServerCmd() {
	}
}
