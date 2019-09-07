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
package com.sandpolis.server.vanilla.exe;

import static com.sandpolis.core.proto.util.Result.ErrorCode.ACCESS_DENIED;
import static com.sandpolis.server.vanilla.store.listener.ListenerStore.ListenerStore;

import com.google.protobuf.Message;
import com.sandpolis.core.instance.PermissionConstant.server;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.proto.net.MCListener.RQ_AddListener;
import com.sandpolis.core.proto.net.MCListener.RQ_ChangeListener;
import com.sandpolis.core.proto.net.MCListener.RQ_ListenerDelta;
import com.sandpolis.core.proto.net.MCListener.RQ_RemoveListener;
import com.sandpolis.core.proto.net.MSG;
import com.sandpolis.server.vanilla.store.listener.Listener;

/**
 * Listener message handlers.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class ListenerExe extends Exelet {

	@Auth
	@Permission(permission = server.listener.create)
	@Handler(tag = MSG.Message.RQ_ADD_LISTENER_FIELD_NUMBER)
	public Message.Builder rq_add_listener(RQ_AddListener rq) {
		var outcome = begin();

		ListenerStore.add(rq.getConfig());
		return success(outcome);
	}

	@Auth
	@Handler(tag = MSG.Message.RQ_REMOVE_LISTENER_FIELD_NUMBER)
	public Message.Builder rq_remove_listener(RQ_RemoveListener rq) {
		var outcome = begin();
		if (!ownership(rq.getId()))
			return failure(outcome, ACCESS_DENIED);

		ListenerStore.remove(rq.getId());
		return success(outcome);
	}

	@Auth
	@Handler(tag = MSG.Message.RQ_LISTENER_DELTA_FIELD_NUMBER)
	public Message.Builder rq_listener_delta(RQ_ListenerDelta rq) {
		var outcome = begin();
		if (!ownership(rq.getId()))
			return failure(outcome, ACCESS_DENIED);

		return complete(outcome, ListenerStore.delta(rq.getId(), rq.getDelta()));
	}

	@Auth
	@Handler(tag = MSG.Message.RQ_CHANGE_LISTENER_FIELD_NUMBER)
	public Message.Builder rq_change_listener(RQ_ChangeListener rq) {
		var outcome = begin();
		if (!ownership(rq.getId()))
			return failure(outcome, ACCESS_DENIED);

		return complete(outcome, ListenerStore.change(rq.getId(), rq.getState()));
	}

	/**
	 * Check that the user associated with the connection owns the given listener.
	 * 
	 * @param id The listener ID
	 * @return Whether the access check passed
	 */
	@AccessPredicate
	private boolean ownership(long id) {
		Listener listener = ListenerStore.get(id).orElse(null);
		if (listener == null)
			// Listener does not exist
			return false;

		// Check CVID
		return listener.getOwner().getCvid() == connector.getRemoteCvid();
	}

}
