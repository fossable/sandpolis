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
package com.sandpolis.core.viewer.cmd;

import static com.sandpolis.core.util.CryptoUtil.SHA256;

import java.util.Objects;

import com.sandpolis.core.net.command.Cmdlet;
import com.sandpolis.core.net.future.ResponseFuture;
import com.sandpolis.core.proto.net.MsgLogin.RQ_Login;
import com.sandpolis.core.proto.util.Result.Outcome;
import com.sandpolis.core.util.CryptoUtil;

/**
 * Contains login commands.
 *
 * @author cilki
 * @since 4.0.0
 */
public final class LoginCmd extends Cmdlet<LoginCmd> {

	/**
	 * Attempt to login to a server.
	 *
	 * @param user The user's username
	 * @param pass The user's plaintext password
	 * @return A response future
	 */
	public ResponseFuture<Outcome> login(String user, String pass) {
		Objects.requireNonNull(user);
		Objects.requireNonNull(pass);

		return request(RQ_Login.newBuilder().setUsername(user).setPassword(CryptoUtil.hash(SHA256, pass)));
	}

	/**
	 * Prepare for an asynchronous command.
	 *
	 * @return A configurable object from which all asynchronous (nonstatic)
	 *         commands in {@link LoginCmd} can be invoked
	 */
	public static LoginCmd async() {
		return new LoginCmd();
	}

	private LoginCmd() {
	}
}
