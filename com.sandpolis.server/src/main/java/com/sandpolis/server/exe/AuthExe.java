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
package com.sandpolis.server.exe;

import static com.sandpolis.core.util.ProtoUtil.begin;
import static com.sandpolis.core.util.ProtoUtil.failure;
import static com.sandpolis.core.util.ProtoUtil.rs;
import static com.sandpolis.core.util.ProtoUtil.success;

import java.util.List;
import java.util.Objects;

import com.google.protobuf.ByteString;
import com.sandpolis.core.net.Exelet;
import com.sandpolis.core.net.Sock;
import com.sandpolis.core.net.Sock.ConnectionState;
import com.sandpolis.core.net.store.network.NetworkStore;
import com.sandpolis.core.proto.net.MCAuth.IM_Nonce;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.core.util.CryptoUtil;
import com.sandpolis.server.auth.KeyMechanism;
import com.sandpolis.server.store.group.Group;
import com.sandpolis.server.store.group.GroupStore;

/**
 * Authentication message handlers.
 * 
 * @author cilki
 * @since 5.0.0
 */
public class AuthExe extends Exelet {

	public AuthExe(Sock connector) {
		super(connector);
	}

	@Unauth
	public void rq_no_auth(Message m) {
		var outcome = begin();

		// Get all groups that accept no auth
		List<Group> unauth = GroupStore.getUnauthGroups();
		if (unauth.size() == 0) {
			connector.send(rs(m, failure(outcome)));
		} else {

			// Mark connection as authenticated
			connector.authenticate();
			connector.changeState(ConnectionState.AUTHENTICATED);

			connector.send(rs(m, success(outcome)));

			// TODO add client to group
		}
	}

	@Unauth
	public void rq_password_auth(Message m) {
		var outcome = begin();

		var rq = Objects.requireNonNull(m.getRqPasswordAuth());

		// Get all groups with the password
		List<Group> groups = GroupStore.getByPassword(rq.getPassword());
		if (groups.size() == 0) {
			connector.send(rs(m, failure(outcome)));
		} else {

			// Mark connection as authenticated
			connector.authenticate();
			connector.changeState(ConnectionState.AUTHENTICATED);

			connector.send(rs(m, success(outcome)));

			// TODO add client to group
		}
	}

	@Unauth
	public void rq_key_auth(Message m) throws Exception {
		var outcome = begin();

		var rq = Objects.requireNonNull(m.getRqKeyAuth());
		Group group = GroupStore.get(rq.getGroupId());
		if (group == null) {
			connector.send(rs(m, failure(outcome)));
			return;
		}

		KeyMechanism mech = group.getKeyMechanism(rq.getMechId());
		if (mech == null) {
			connector.send(rs(m, failure(outcome)));
			return;
		}

		// Verify nonce A
		if (rq.getNonce() == null) {
			connector.send(rs(m, failure(outcome)));
			return;
		}
		byte[] nonceA = rq.getNonce().toByteArray();

		// Send nonce B
		byte[] nonceB = CryptoUtil.SAND5.getNonce();
		Message rs = NetworkStore.route(rs(m).setImNonce(IM_Nonce.newBuilder().setNonce(ByteString.copyFrom(nonceB))),
				"net.timeout.response.default").get();

		// Check nonce B
		if (rs.getImNonce() == null
				|| !CryptoUtil.SAND5.check(mech.getServer(), nonceB, rs.getImNonce().toByteArray())) {
			connector.send(rs(m, failure(outcome)));
			return;
		}

		// Send nonce A
		rs = NetworkStore.route(
				rs(m).setImNonce(IM_Nonce.newBuilder()
						.setNonce(ByteString.copyFrom(CryptoUtil.SAND5.sign(mech.getServer(), nonceA)))),
				"net.timeout.response.default").get();

		if (rs.getRsOutcome() != null && rs.getRsOutcome().getResult()) {
			// Mark connection as authenticated
			connector.authenticate();
			connector.changeState(ConnectionState.AUTHENTICATED);
		}
	}
}
