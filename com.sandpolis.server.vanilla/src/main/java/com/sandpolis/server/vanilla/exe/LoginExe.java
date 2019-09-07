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

import static com.sandpolis.core.profile.ProfileStore.ProfileStore;
import static com.sandpolis.core.proto.util.Result.ErrorCode.ACCESS_DENIED;
import static com.sandpolis.core.proto.util.Result.ErrorCode.INVALID_USERNAME;
import static com.sandpolis.core.util.CryptoUtil.SHA256;
import static com.sandpolis.server.vanilla.store.user.UserStore.UserStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;
import com.sandpolis.core.attribute.key.AK_VIEWER;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.profile.Profile;
import com.sandpolis.core.proto.net.MCLogin.RQ_Login;
import com.sandpolis.core.proto.net.MSG;
import com.sandpolis.core.util.CryptoUtil;
import com.sandpolis.core.util.ValidationUtil;
import com.sandpolis.server.vanilla.store.user.User;

/**
 * This {@link Exelet} handles login and logout requests from viewer instances.
 * 
 * @author cilki
 * @since 4.0.0
 */
public class LoginExe extends Exelet {

	private static final Logger log = LoggerFactory.getLogger(LoginExe.class);

	@Auth
	@Handler(tag = MSG.Message.RQ_LOGOUT_FIELD_NUMBER)
	public void rq_logout(MSG.Message rq) {
		log.debug("Processing logout request from: {}", connector.getRemoteIP());

		connector.close();
	}

	@Unauth
	@Handler(tag = MSG.Message.RQ_LOGIN_FIELD_NUMBER)
	public Message.Builder rq_login(RQ_Login rq) {
		log.debug("Processing login request from: {}", connector.getRemoteIP());
		var outcome = begin();

		// Validate username
		String username = rq.getUsername();
		if (!ValidationUtil.username(username)) {
			log.debug("The username ({}) is invalid", username);
			return failure(outcome, INVALID_USERNAME);
		}

		User user = UserStore.get(username).orElse(null);
		if (user == null) {
			log.debug("The user ({}) does not exist", username);
			return failure(outcome, ACCESS_DENIED);
		}

		// Check expiration
		if (UserStore.isExpired(user)) {
			log.debug("The user ({}) is expired", username);
			return failure(outcome, ACCESS_DENIED);
		}

		// Check password
		if (!CryptoUtil.PBKDF2.check(CryptoUtil.hash(SHA256, rq.getPassword()), user.getHash())) {
			log.debug("Authentication failed", username);
			return failure(outcome, ACCESS_DENIED);
		}

		log.debug("Accepting login request for user: {}", username);

		// Mark connection as authenticated
		connector.authenticate();

		// Update login metadata
		Profile viewer = ProfileStore.getViewer(username).orElse(null);
		if (viewer == null) {
			// Build new profile
			viewer = ProfileStore.getProfileOrCreate(connector.getRemoteCvid(), connector.getRemoteUuid());
			viewer.set(AK_VIEWER.USERNAME, username);
		}

		viewer.set(AK_VIEWER.LOGIN_IP, connector.getRemoteIP());
		viewer.set(AK_VIEWER.LOGIN_TIME, System.currentTimeMillis());

		return success(outcome);
	}
}
