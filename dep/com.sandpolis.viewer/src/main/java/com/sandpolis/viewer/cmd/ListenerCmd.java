/******************************************************************************
 *                                                                            *
 *                    Copyright 2017 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.viewer.cmd;

import com.sandpolis.core.net.Cmdlet;
import com.sandpolis.core.net.future.ResponseFuture;
import com.sandpolis.core.proto.net.MCListener.RQ_AddListener;
import com.sandpolis.core.proto.net.MCListener.RQ_ChangeListener;
import com.sandpolis.core.proto.net.MCListener.RQ_ChangeListener.ListenerState;
import com.sandpolis.core.proto.net.MCListener.RQ_RemoveListener;
import com.sandpolis.core.proto.pojo.Listener.ListenerConfig;
import com.sandpolis.core.proto.util.Result.Outcome;

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
	 * @param conf The listener configuration
	 * @return A future that will receive the outcome of this action
	 */
	public ResponseFuture<Outcome> addListener(ListenerConfig conf) {
		return rq(RQ_AddListener.newBuilder().setConfig(conf));

	}

	/**
	 * Stop and remove a listener on the server.
	 * 
	 * @param id The listener ID
	 * @return A future that will receive the outcome of this action
	 */
	public ResponseFuture<Outcome> removeListener(int id) {
		return rq(RQ_RemoveListener.newBuilder().setId(id));

	}

	/**
	 * Change the state of a listener on the server.
	 * 
	 * @param id    The listener
	 * @param state The new state
	 * @return A future that will receive the outcome of this action
	 */
	public ResponseFuture<Outcome> changeListenerState(ListenerState state, long id) {
		return rq(RQ_ChangeListener.newBuilder().setId(id).setState(state));

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
