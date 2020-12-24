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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.sandpolis.core.instance.msg.MsgPing.RQ_Ping;
import com.sandpolis.core.instance.msg.MsgPing.RS_Ping;
import com.sandpolis.core.net.cmdlet.Cmdlet;
import com.sandpolis.core.clientserver.msg.MsgServer.RQ_ServerBanner;
import com.sandpolis.core.clientserver.msg.MsgServer.RS_ServerBanner;

/**
 * Contains server commands.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class ServerCmd extends Cmdlet<ServerCmd> {

	/**
	 * @return An asynchronous {@link CompletionStage}
	 */
	public CompletionStage<RS_ServerBanner> getBanner() {
		return request(RS_ServerBanner.class, RQ_ServerBanner.newBuilder());
	}

	/**
	 * Perform an application-level ping to estimate the link latency in
	 * milliseconds.
	 *
	 * @return An asynchronous {@link CompletionStage}
	 */
	public CompletionStage<Long> ping() {
		return CompletableFuture.supplyAsync(() -> {
			long t1 = System.nanoTime();
			request(RS_Ping.class, RQ_Ping.newBuilder()).toCompletableFuture().join();
			long t2 = System.nanoTime();

			// To get from 1e9 to (1e3)/2, multiply by (1e-6)/2 = 1/2000000
			return (t2 - t1) / 2000000;
		}).orTimeout(2000, MILLISECONDS);
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
