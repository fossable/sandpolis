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

import static com.sandpolis.core.util.ProtoUtil.begin;
import static com.sandpolis.core.util.ProtoUtil.failure;
import static com.sandpolis.core.util.ProtoUtil.rs;
import static com.sandpolis.core.util.ProtoUtil.success;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.attribute.key.AK_VIEWER;
import com.sandpolis.core.net.Exelet;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.net.Sock.ConnectionState;
import com.sandpolis.core.net.store.connection.ConnectionStore;
import com.sandpolis.core.profile.Profile;
import com.sandpolis.core.profile.store.profile.ProfileStore;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.core.proto.util.Result.Outcome;
import com.sandpolis.core.util.ValidationUtil;
import com.sandpolis.server.store.user.UserStore;

/**
 * This {@link Exelet} handles login and logout requests from viewer instances.
 * 
 * @author cilki
 * @since 4.0.0
 */
public class LoginExe extends Exelet {

	private static final Logger log = LoggerFactory.getLogger(LoginExe.class);

	public LoginExe(Sock connector) {
		super(connector);
	}

	@Auth
	public void rq_logout(Message m) {
		log.debug("Processing logout request from: {}", connector.getRemoteIP());

		connector.send(rs(m).setRsOutcome(Outcome.newBuilder().setResult(true)));
		ConnectionStore.close(connector);
	}

	@Unauth
	public void rq_login(Message m) {
		log.debug("Processing login request from: {}", connector.getRemoteIP());
		Outcome.Builder outcome = begin();
		int id = m.getId();

		// Extract username
		String user = m.getRqLogin().getUsername();

		// Validate username
		if (!ValidationUtil.username(user)) {
			log.debug("The username ({}) is invalid", user);
			failLogin(outcome.setComment("Invalid username"), id, user);
			return;
		}

		// Check for user
		if (!UserStore.exists(user)) {
			log.debug("The user ({}) does not exist", user);
			failLogin(outcome.setComment("Nonexistant user"), id, user);
			return;
		}

		// Perform authentication
		if (!UserStore.validLogin(user, m.getRqLogin().getPassword()).getResult()) {
			log.debug("Authentication failed", user);
			failLogin(outcome.setComment("Authentication failed"), id, user);
			return;
		}

		passLogin(outcome, id, user);
	}

	/**
	 * Utility method to allow a login request.
	 * 
	 * @param outcome
	 *            The current outcome
	 * @param id
	 *            The request id
	 * @param user
	 *            The username
	 */
	private void passLogin(Outcome.Builder outcome, int id, String user) {
		log.debug("Accepting login request for user: {}", user);

		// Mark connection as authenticated
		connector.authenticate();

		// this connection is now authenticated
		connector.setState(ConnectionState.AUTHENTICATED);

		// Retrieve profile
		Profile profile = ProfileStore.getViewer(user);

		// Update login metadata
		profile.set(AK_VIEWER.LOGIN_IP, connector.getRemoteIP());
		profile.set(AK_VIEWER.LOGIN_TIME, System.currentTimeMillis());

		connector.send(rs(id).setRsOutcome(success(outcome)));
	}

	/**
	 * Reject the login request, but leave the connection open.
	 * 
	 * @param outcome
	 *            The current outcome
	 * @param id
	 *            The request id
	 * @param user
	 *            The username
	 */
	private void failLogin(Outcome.Builder outcome, int id, String user) {
		log.debug("Rejecting login request for user: {}", user);

		connector.send(rs(id).setRsOutcome(failure(outcome)));
	}

}
