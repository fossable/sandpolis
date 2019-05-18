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

import static com.sandpolis.core.util.ProtoUtil.begin;
import static com.sandpolis.core.util.ProtoUtil.success;

import com.sandpolis.core.instance.PermissionConstant.server;
import com.sandpolis.core.net.Exelet;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.server.vanilla.store.listener.Listener;
import com.sandpolis.server.vanilla.store.listener.ListenerStore;

/**
 * Listener message handlers.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class ListenerExe extends Exelet {

	public ListenerExe(Sock connector) {
		super(connector);
	}

	@Auth
	@Permission(permission = server.listener.create)
	public void rq_add_listener(Message m) {
		var rq = m.getRqAddListener();

		var outcome = begin();
		ListenerStore.add(rq.getConfig());
		reply(m, success(outcome));
	}

	@Auth
	public void rq_remove_listener(Message m) {
		var rq = m.getRqRemoveListener();
		if (!accessCheck(m, this::ownership, rq.getId()))
			return;

		var outcome = begin();
		ListenerStore.remove(rq.getId());
		reply(m, success(outcome));
	}

	@Auth
	public void rq_listener_delta(Message m) {
		var rq = m.getRqListenerDelta();
		if (!accessCheck(m, this::ownership, rq.getId()))
			return;

		reply(m, ListenerStore.delta(rq.getId(), rq.getDelta()));
	}

	@Auth
	public void rq_change_listener(Message m) {
		var rq = m.getRqChangeListener();
		if (!accessCheck(m, this::ownership, rq.getId()))
			return;

		reply(m, ListenerStore.change(rq.getId(), rq.getState()));
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
