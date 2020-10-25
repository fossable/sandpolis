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
package com.sandpolis.agent.vanilla.cmd;

import java.util.concurrent.CompletionStage;

import com.sandpolis.core.serveragent.msg.MsgAuth.RQ_NoAuth;
import com.sandpolis.core.serveragent.msg.MsgAuth.RQ_PasswordAuth;
import com.sandpolis.core.foundation.Result.Outcome;
import com.sandpolis.core.net.cmdlet.Cmdlet;

/**
 * {@link AuthCmd} contains commands required for agent instances to
 * authenticate with a server.
 *
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
