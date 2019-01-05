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
package com.sandpolis.client.mega.cmd;

import static com.sandpolis.core.util.ProtoUtil.begin;
import static com.sandpolis.core.util.ProtoUtil.complete;
import static com.sandpolis.core.util.ProtoUtil.getOutcome;
import static com.sandpolis.core.util.ProtoUtil.rq;
import static com.sandpolis.core.util.ProtoUtil.rs;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import com.google.protobuf.ByteString;
import com.sandpolis.core.net.Cmdlet;
import com.sandpolis.core.net.exception.MessageFlowException;
import com.sandpolis.core.net.store.network.NetworkStore;
import com.sandpolis.core.proto.net.MCAuth.IM_Nonce;
import com.sandpolis.core.proto.net.MCAuth.RQ_KeyAuth;
import com.sandpolis.core.proto.net.MCAuth.RQ_NoAuth;
import com.sandpolis.core.proto.net.MCAuth.RQ_PasswordAuth;
import com.sandpolis.core.proto.net.MSG.Message;
import com.sandpolis.core.proto.util.Result.Outcome;
import com.sandpolis.core.util.CryptoUtil;
import com.sandpolis.core.util.CryptoUtil.SAND5.ReciprocalKeyPair;

/**
 * Contains authentication commands for client instances.
 * 
 * @author cilki
 * @since 5.0.0
 */
public final class AuthCmd extends Cmdlet {

	/**
	 * Attempt to authenticate with nothing.
	 * 
	 * @return The outcome of the action
	 */
	public static Outcome none() throws InterruptedException, ExecutionException, TimeoutException {
		return getOutcome(
				NetworkStore.route(rq().setRqNoAuth(RQ_NoAuth.newBuilder()), "net.timeout.response.default").get());
	}

	/**
	 * Attempt to authenticate with a password.
	 * 
	 * @return The outcome of the action
	 */
	public static Outcome password(String password) throws InterruptedException, ExecutionException, TimeoutException {
		return getOutcome(NetworkStore.route(rq().setRqPasswordAuth(RQ_PasswordAuth.newBuilder().setPassword(password)),
				"net.timeout.response.default").get());
	}

	/**
	 * Attempt to authenticate with a SAND5 key.
	 * 
	 * @return The outcome of the action
	 */
	public static Outcome key(String groupId, long mechId, ReciprocalKeyPair key)
			throws InterruptedException, ExecutionException, TimeoutException {
		Outcome.Builder outcome = begin();

		byte[] nonceA = CryptoUtil.SAND5.getNonce();
		Message rs = NetworkStore.route(rq().setRqKeyAuth(
				RQ_KeyAuth.newBuilder().setGroupId(groupId).setMechId(mechId).setNonce(ByteString.copyFrom(nonceA))),
				"net.timeout.response.default").get();
		if (rs.getRsOutcome() != null)
			return rs.getRsOutcome();
		if (rs.getImNonce() == null)
			throw new MessageFlowException(RQ_KeyAuth.class, rs);

		byte[] signed = CryptoUtil.SAND5.sign(key, rs.getImNonce().getNonce().toByteArray());
		rs = NetworkStore.route(rq().setImNonce(IM_Nonce.newBuilder().setNonce(ByteString.copyFrom(signed))),
				"net.timeout.response.default").get();
		if (rs.getRsOutcome() != null)
			return rs.getRsOutcome();
		if (rs.getImNonce() == null)
			throw new MessageFlowException(IM_Nonce.class, rs);

		boolean verifyServer = CryptoUtil.SAND5.check(key, nonceA, rs.getImNonce().getNonce().toByteArray());
		NetworkStore.route(rs(rs).setRsOutcome(Outcome.newBuilder().setResult(verifyServer)));

		return complete(outcome.setResult(verifyServer));
	}

	private AuthCmd() {
	}
}
