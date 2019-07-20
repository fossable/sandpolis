/******************************************************************************
 *                                                                            *
 *                    Copyright 2018 Subterranean Security                    *
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

import static com.sandpolis.core.proto.util.Result.ErrorCode.FAILURE_KEY_CHALLENGE;
import static com.sandpolis.core.proto.util.Result.ErrorCode.INVALID_KEY;
import static com.sandpolis.core.proto.util.Result.ErrorCode.UNKNOWN_GROUP;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.net.handler.Sand5Handler;
import com.sandpolis.core.profile.Profile;
import com.sandpolis.core.profile.ProfileStore;
import com.sandpolis.core.proto.net.MCAuth.RQ_KeyAuth;
import com.sandpolis.core.proto.net.MCAuth.RQ_PasswordAuth;
import com.sandpolis.core.proto.net.MSG;
import com.sandpolis.server.vanilla.auth.KeyMechanism;
import com.sandpolis.server.vanilla.store.group.Group;
import com.sandpolis.server.vanilla.store.group.GroupStore;

import io.netty.util.concurrent.Future;

/**
 * Authentication message handlers.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class AuthExe extends Exelet {

	private static final Logger log = LoggerFactory.getLogger(AuthExe.class);

//	@Unauth
//	public Message.Builder rq_no_auth(RQ_NoAuth rq) {
//		var outcome = begin();
//
//		List<Group> groups = GroupStore.getUnauthGroups();
//		if (groups.size() == 0) {
//			log.debug("Refusing free authentication attempt because there are no unauth groups");
//			return failure(outcome, UNKNOWN_GROUP);
//		}
//
//		Profile client = ProfileStore.getProfileOrCreate(connector.getRemoteCvid(), connector.getRemoteUuid());
//		groups.forEach(group -> {
//			// TODO add client to group
//		});
//
//		// Connection is now authenticated
//		connector.authenticate();
//		connector.changeState(ConnectionState.AUTHENTICATED);
//
//		return success(outcome);
//	}

	// TODO temporarily use void-style handler to avoid creating profile before
	// response is sent.
	@Unauth
	@Handler(tag = MSG.Message.RQ_NO_AUTH_FIELD_NUMBER)
	public void rq_no_auth(MSG.Message m) {
		var outcome = begin();

		List<Group> groups = GroupStore.getUnauthGroups();
		if (groups.size() == 0) {
			log.debug("Refusing free authentication attempt because there are no unauth groups");
			reply(m, failure(outcome, UNKNOWN_GROUP));
			return;
		}

		// Connection is now authenticated
		connector.authenticate();

		reply(m, success(outcome));

		Profile client = ProfileStore.getProfileOrCreate(connector.getRemoteCvid(), connector.getRemoteUuid());
		groups.forEach(group -> {
			// TODO add client to group
		});
	}

	@Unauth
	@Handler(tag = MSG.Message.RQ_PASSWORD_AUTH_FIELD_NUMBER)
	public Message.Builder rq_password_auth(RQ_PasswordAuth rq) {
		var outcome = begin();

		List<Group> groups = GroupStore.getByPassword(rq.getPassword());
		if (groups.size() == 0) {
			log.debug("Refusing password authentication attempt because the password did not match any group");
			return failure(outcome, UNKNOWN_GROUP);
		}

		Profile client = ProfileStore.getProfileOrCreate(connector.getRemoteCvid(), connector.getRemoteUuid());
		groups.forEach(group -> {
			// TODO add client to group
		});

		// Connection is now authenticated
		connector.authenticate();

		return success(outcome);
	}

	@Unauth
	@Handler(tag = MSG.Message.RQ_KEY_AUTH_FIELD_NUMBER)
	public Message.Builder rq_key_auth(RQ_KeyAuth rq) throws InterruptedException, ExecutionException {
		var outcome = begin();

		Group group = GroupStore.get(rq.getGroupId()).orElse(null);
		if (group == null) {
			log.debug("Refusing key authentication attempt due to unknown group ID: {}", rq.getGroupId());
			return failure(outcome, UNKNOWN_GROUP);
		}

		KeyMechanism mech = group.getKeyMechanism(rq.getMechId());
		if (mech == null) {
			log.debug("Refusing key authentication attempt due to unknown mechanism ID: {}", rq.getMechId());
			return failure(outcome, INVALID_KEY);
		}

		Sand5Handler sand5 = Sand5Handler.registerRequestHandler(connector.channel(), mech.getServer());
		Future<Boolean> future = sand5.challengeFuture();

		if (future.get()) {
			Profile client = ProfileStore.getProfileOrCreate(connector.getRemoteCvid(), connector.getRemoteUuid());
			// TODO add client to group

			// Connection is now authenticated
			connector.authenticate();
			return success(outcome);
		} else {
			log.debug("Refusing key authentication attempt due to failed challenge");
			return failure(outcome, FAILURE_KEY_CHALLENGE);
		}
	}
}
