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

import com.google.protobuf.Message;
import com.sandpolis.core.instance.PermissionConstant.server;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.proto.net.MCUser.RQ_AddUser;
import com.sandpolis.core.proto.net.MCUser.RQ_RemoveUser;
import com.sandpolis.core.proto.net.MCUser.RQ_UserDelta;
import com.sandpolis.core.proto.net.MSG;
import com.sandpolis.server.vanilla.store.user.User;
import com.sandpolis.server.vanilla.store.user.UserStore;

/**
 * User message handlers.
 * 
 * @author cilki
 * @since 4.0.0
 */
public class UserExe extends Exelet {

	@Auth
	@Permission(permission = server.user.create)
	@Handler(tag = MSG.Message.RQ_ADD_USER_FIELD_NUMBER)
	public Message.Builder rq_add_user(RQ_AddUser rq) {
		var outcome = begin();

		UserStore.add(rq.getConfig());
		return success(outcome);
	}

	@Auth
	@Handler(tag = MSG.Message.RQ_REMOVE_USER_FIELD_NUMBER)
	public Message.Builder rq_remove_user(RQ_RemoveUser rq) {
		var outcome = begin();
		if (!ownership(rq.getId()))
			return failure(outcome, ACCESS_DENIED);

		UserStore.remove(rq.getId());
		return success(outcome);
	}

	@Auth
	@Handler(tag = MSG.Message.RQ_USER_DELTA_FIELD_NUMBER)
	public Message.Builder rq_user_delta(RQ_UserDelta rq) {
		var outcome = begin();
		if (!ownership(rq.getDelta().getConfig().getId()))
			return failure(outcome, ACCESS_DENIED);

		return complete(outcome, UserStore.delta(rq.getId(), rq.getDelta()));
	}

	/**
	 * Check that the user associated with the connection owns the given listener.
	 * 
	 * @param id The user ID
	 * @return Whether the access check passed
	 */
	@AccessPredicate
	private boolean ownership(long id) {
		User user = UserStore.get(id).orElse(null);
		if (user == null)
			// User does not exist
			return false;

		// Check CVID
		return user.getCvid() == connector.getRemoteCvid();
	}

}
