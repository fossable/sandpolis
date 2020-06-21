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

import com.sandpolis.core.instance.Listener.ListenerConfig;
import com.sandpolis.core.instance.Result.Outcome;
import com.sandpolis.core.net.MsgListener.RQ_AddListener;
import com.sandpolis.core.net.MsgListener.RQ_ChangeListener;
import com.sandpolis.core.net.MsgListener.RQ_ChangeListener.ListenerState;
import com.sandpolis.core.net.MsgListener.RQ_RemoveListener;
import com.sandpolis.core.net.command.Cmdlet;
import com.sandpolis.core.net.future.ResponseFuture;

/**
 * Contains listener commands.
 *
 * @author cilki
 * @since 4.0.0
 */
public final class ListenerCmd extends Cmdlet<ListenerCmd> {

	/**
	 * Add a new listener on the server.
	 *
	 * @param config The listener configuration
	 * @return A future that will receive the outcome of this action
	 */
	public ResponseFuture<Outcome> addListener(ListenerConfig config) {
		return request(RQ_AddListener.newBuilder().setConfig(config));

	}

	/**
	 * Stop and remove a listener on the server.
	 *
	 * @param id The listener ID
	 * @return A future that will receive the outcome of this action
	 */
	public ResponseFuture<Outcome> removeListener(int id) {
		return request(RQ_RemoveListener.newBuilder().setId(id));

	}

	/**
	 * Change the state of a listener on the server.
	 *
	 * @param id    The listener
	 * @param state The new state
	 * @return A future that will receive the outcome of this action
	 */
	public ResponseFuture<Outcome> changeListenerState(ListenerState state, long id) {
		return request(RQ_ChangeListener.newBuilder().setId(id).setState(state));

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
