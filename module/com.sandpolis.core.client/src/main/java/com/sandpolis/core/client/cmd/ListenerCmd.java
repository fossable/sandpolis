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
package com.sandpolis.core.client.cmd;

import static com.sandpolis.core.clientserver.msg.MsgListener.RQ_ListenerOperation.ListenerOperation.LISTENER_CREATE;
import static com.sandpolis.core.clientserver.msg.MsgListener.RQ_ListenerOperation.ListenerOperation.LISTENER_DELETE;

import java.util.concurrent.CompletionStage;

import com.sandpolis.core.foundation.Result.Outcome;
import com.sandpolis.core.instance.Listener.ListenerConfig;
import com.sandpolis.core.net.cmdlet.Cmdlet;
import com.sandpolis.core.clientserver.msg.MsgListener.RQ_ListenerOperation;

/**
 * An API for interacting with listeners on the server.
 *
 * @author cilki
 * @since 4.0.0
 */
public final class ListenerCmd extends Cmdlet<ListenerCmd> {

	/**
	 * Add a new listener on the server.
	 *
	 * @param config The listener configuration
	 * @return An asynchronous {@link CompletionStage}
	 */
	public CompletionStage<Outcome> create(ListenerConfig config) {
		return request(Outcome.class,
				RQ_ListenerOperation.newBuilder().setOperation(LISTENER_CREATE).addListenerConfig(config));
	}

	/**
	 * Stop and remove a listener on the server.
	 *
	 * @param id The listener ID
	 * @return An asynchronous {@link CompletionStage}
	 */
	public CompletionStage<Outcome> remove(int id) {
		return request(Outcome.class, RQ_ListenerOperation.newBuilder().setOperation(LISTENER_DELETE)
				.addListenerConfig(ListenerConfig.newBuilder().setId(id)));
	}

	/**
	 * Prepare for an asynchronous command.
	 *
	 * @return A configurable object from which all asynchronous (nonstatic)
	 *         commands in {@link ListenerCmd} can be invoked
	 */
	public static ListenerCmd async() {
		return new ListenerCmd();
	}

	private ListenerCmd() {
	}
}
