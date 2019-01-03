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
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.server.store.user.User;
import com.sandpolis.server.store.user.UserStore;

/**
 * User message handlers.
 * 
 * @author cilki
 * @since 4.0.0
 */
public class UserExe extends Exelet {

	public UserExe(Sock connector) {
		super(connector);
	}

	@Auth
	@Permission(permission = Perm.server.users.create)
	public void rq_add_user(Message m) {
		var rq = m.getRqAddUser();
		reply(m, UserStore.add(rq.getConfig()));
	}

	@Auth
	public void rq_remove_user(Message m) {
		var rq = m.getRqRemoveUser();
		if (!accessCheck(m, this::ownership, rq.getId()))
			return;

		reply(m, UserStore.remove(rq.getId()));
	}

	@Auth
	public void rq_user_delta(Message m) {
		var rq = m.getRqUserDelta();
		if (!accessCheck(m, this::ownership, rq.getDelta().getConfig().getId()))
			return;

		reply(m, UserStore.delta(rq.getId(), rq.getDelta()));
	}

	/**
	 * Check that the user associated with the connection owns the given listener.
	 * 
	 * @param id The user ID
	 * @return Whether the access check passed
	 */
	@AccessPredicate
	private boolean ownership(long id) {
		User user = UserStore.get(id);
		if (user == null)
			// User does not exist
			return false;

		// Check CVID
		return user.getCvid() == connector.getRemoteCvid();
	}

}
