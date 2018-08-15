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
package com.sandpolis.server.exe;

import com.sandpolis.core.instance.Perm;
import com.sandpolis.core.net.Exelet;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.proto.net.MCListener.RQ_EditListener;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.server.store.listener.Listener;
import com.sandpolis.server.store.listener.ListenerStore;

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
	@Permission(permission = Perm.server.listeners.create)
	public void rq_add_listener(Message m) {
		reply(m, ListenerStore.add(m.getRqAddListener().getConfig()));
	}

	@Auth
	public void rq_remove_listener(Message m) {
		if (!accessCheck(m, this::ownership, m.getRqRemoveListener().getId()))
			return;

		reply(m, ListenerStore.remove(m.getRqRemoveListener().getId()));
	}

	@Auth
	public void rq_edit_listener(Message m) {
		RQ_EditListener rq = m.getRqEditListener();
		if (!accessCheck(m, this::ownership, rq.getId()))
			return;

		reply(m, ListenerStore.edit(rq.getId(), rq.getChangedList(), rq.getConfig()));
	}

	@Auth
	public void rq_change_listener(Message m) {
		if (!accessCheck(m, this::ownership, m.getRqChangeListener().getId()))
			return;

		reply(m, ListenerStore.change(m.getRqChangeListener().getId(), m.getRqChangeListener().getState()));
	}

	/**
	 * Check that the user associated with the connection owns the given listener.
	 * 
	 * @param id The listener ID
	 * @return Whether the access check passed
	 */
	@AccessPredicate
	private boolean ownership(long id) {
		Listener listener = ListenerStore.get(id);
		if (listener == null)
			// Listener does not exist
			return false;

		// Check CVID
		return listener.getOwner().getCvid() == connector.getRemoteCvid();
	}

}
