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

import static java.util.Objects.requireNonNull;

import java.util.concurrent.CompletionStage;

import com.sandpolis.core.cs.msg.MsgAuth.RQ_KeyAuth;
import com.sandpolis.core.cs.msg.MsgAuth.RQ_NoAuth;
import com.sandpolis.core.cs.msg.MsgAuth.RQ_PasswordAuth;
import com.sandpolis.core.foundation.Result.Outcome;
import com.sandpolis.core.net.HandlerKey;
import com.sandpolis.core.net.cmdlet.Cmdlet;
import com.sandpolis.core.net.handler.sand5.ReciprocalKeyPair;
import com.sandpolis.core.net.handler.sand5.Sand5Handler;

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
	 * @return An asynchronous {@link CompletionStage}
	 */
	public CompletionStage<Outcome> none() {

		return request(Outcome.class, RQ_NoAuth.newBuilder()).thenApply(rs -> {
			if (rs.getResult()) {
				target.authenticate();
			}
			return rs;
		});
	}

	/**
	 * Attempt to authenticate with a password.
	 *
	 * @return An asynchronous {@link CompletionStage}
	 */
	public CompletionStage<Outcome> password(String password) {

		return request(Outcome.class, RQ_PasswordAuth.newBuilder().setPassword(password)).thenApply(rs -> {
			if (rs.getResult()) {
				target.authenticate();
			}
			return rs;
		});
	}

	/**
	 * Attempt to authenticate with a SAND5 keypair.
	 *
	 * @param group The group ID
	 * @param mech  The key mechanism ID
	 * @param key   The client keypair
	 * @return An asynchronous {@link CompletionStage}
	 */
	public CompletionStage<Outcome> key(String group, long mech, ReciprocalKeyPair key) {
		requireNonNull(group);
		requireNonNull(key);

		Sand5Handler sand5 = Sand5Handler.newResponseHandler(key);
		target.engage(HandlerKey.SAND5, sand5);

		sand5.challengeFuture().addListener(future -> {
			if (future.isSuccess())
				target.authenticate();
		});

		return request(Outcome.class, RQ_KeyAuth.newBuilder().setGroupId(group).setMechId(mech));
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
