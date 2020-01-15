//============================================================================//
//                                                                            //
//                Copyright © 2015 - 2020 Subterranean Security               //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPL    //
//  as published by the Mozilla Foundation at:                                //
//                                                                            //
//    https://mozilla.org/MPL/2.0                                             //
//                                                                            //
//=========================================================S A N D P O L I S==//
package com.sandpolis.client.mega.cmd;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sandpolis.core.net.HandlerKey;
import com.sandpolis.core.net.command.Cmdlet;
import com.sandpolis.core.net.future.ResponseFuture;
import com.sandpolis.core.net.handler.sand5.Sand5Handler;
import com.sandpolis.core.proto.net.MsgAuth.RQ_KeyAuth;
import com.sandpolis.core.proto.net.MsgAuth.RQ_NoAuth;
import com.sandpolis.core.proto.net.MsgAuth.RQ_PasswordAuth;
import com.sandpolis.core.proto.util.Result.Outcome;
import com.sandpolis.core.util.CryptoUtil.SAND5.ReciprocalKeyPair;

/**
 * Contains authentication commands for client instances.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class AuthCmd extends Cmdlet<AuthCmd> {

	/**
	 * Attempt to authenticate without providing any form of identification.
	 *
	 * @return A response future
	 */
	public ResponseFuture<Outcome> none() {
		sock.authenticate();// TODO do after successful response
		return request(RQ_NoAuth.newBuilder());
	}

	/**
	 * Attempt to authenticate with a password.
	 *
	 * @return A response future
	 */
	public ResponseFuture<Outcome> password(String password) {
		checkNotNull(password);

		sock.authenticate();// TODO do after successful response
		return request(RQ_PasswordAuth.newBuilder().setPassword(password));
	}

	/**
	 * Attempt to authenticate with a SAND5 keypair.
	 *
	 * @param group The group ID
	 * @param mech  The key mechanism ID
	 * @param key   The client keypair
	 * @return A response future
	 */
	public ResponseFuture<Outcome> key(String group, long mech, ReciprocalKeyPair key) {
		checkNotNull(group);
		checkNotNull(key);

		Sand5Handler sand5 = Sand5Handler.newResponseHandler(key);
		sock.engage(HandlerKey.SAND5, sand5);

		sand5.challengeFuture().addListener(future -> {
			if (future.isSuccess())
				sock.authenticate();
		});

		return request(RQ_KeyAuth.newBuilder().setGroupId(group).setMechId(mech));
	}

	/**
	 * Prepare for an asynchronous command.
	 *
	 * @return A configurable object from which all asynchronous (nonstatic)
	 *         commands in {@link AuthCmd} can be invoked
	 */
	public static AuthCmd async() {
		return new AuthCmd();
	}

	private AuthCmd() {
	}
}
