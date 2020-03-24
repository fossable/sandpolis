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
package com.sandpolis.server.vanilla.exe;

import static com.sandpolis.core.instance.Result.ErrorCode.ACCESS_DENIED;
import static com.sandpolis.core.instance.Result.ErrorCode.INVALID_USERNAME;
import static com.sandpolis.core.instance.util.ProtoUtil.begin;
import static com.sandpolis.core.instance.util.ProtoUtil.failure;
import static com.sandpolis.core.instance.util.ProtoUtil.success;
import static com.sandpolis.core.profile.store.ProfileStore.ProfileStore;
import static com.sandpolis.core.util.CryptoUtil.SHA256;
import static com.sandpolis.server.vanilla.store.user.UserStore.UserStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.net.Message.MSG;
import com.sandpolis.core.net.MsgLogin.RQ_Login;
import com.sandpolis.core.net.MsgLogin.RQ_Logout;
import com.sandpolis.core.net.command.Exelet;
import com.sandpolis.core.net.handler.exelet.ExeletContext;
import com.sandpolis.core.profile.AK_VIEWER;
import com.sandpolis.core.profile.store.Profile;
import com.sandpolis.core.util.CryptoUtil;
import com.sandpolis.core.util.ValidationUtil;
import com.sandpolis.server.vanilla.store.user.User;

/**
 * This {@link Exelet} handles login and logout requests from viewer instances.
 *
 * @author cilki
 * @since 4.0.0
 */
public final class LoginExe extends Exelet {

	private static final Logger log = LoggerFactory.getLogger(LoginExe.class);

	@Auth
	@Handler(tag = MSG.RQ_LOGOUT_FIELD_NUMBER)
	public static void rq_logout(ExeletContext context, RQ_Logout rq) {
		log.debug("Processing logout request from: {}", context.connector.getRemoteIP());
		context.connector.close();
	}

	@Unauth
	@Handler(tag = MSG.RQ_LOGIN_FIELD_NUMBER)
	public static MessageOrBuilder rq_login(ExeletContext context, RQ_Login rq) {
		log.debug("Processing login request from: {}", context.connector.getRemoteIP());
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
		context.connector.authenticate();

		// Update login metadata
		Profile profile = ProfileStore.getViewer(username).orElse(null);
		if (profile == null) {
			// Build new profile
			profile = new Profile(context.connector.getRemoteUuid(), context.connector.getRemoteInstance(),
					context.connector.getRemoteInstanceFlavor());
			profile.set(AK_VIEWER.USERNAME, username);
			ProfileStore.add(profile);
		}

		profile.set(AK_VIEWER.LOGIN_IP, context.connector.getRemoteIP());
		profile.set(AK_VIEWER.LOGIN_TIME, System.currentTimeMillis());

		return success(outcome);
	}

	private LoginExe() {
	}
}
