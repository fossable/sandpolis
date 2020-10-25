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
package com.sandpolis.core.server.auth;

import static com.sandpolis.core.foundation.Result.ErrorCode.ACCESS_DENIED;
import static com.sandpolis.core.foundation.Result.ErrorCode.INVALID_USERNAME;
import static com.sandpolis.core.foundation.util.ProtoUtil.begin;
import static com.sandpolis.core.foundation.util.ProtoUtil.failure;
import static com.sandpolis.core.foundation.util.ProtoUtil.success;
import static com.sandpolis.core.instance.Metatypes.InstanceType.VIEWER;
import static com.sandpolis.core.instance.profile.ProfileStore.ProfileStore;
import static com.sandpolis.core.server.user.UserStore.UserStore;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageOrBuilder;
import com.sandpolis.core.foundation.util.CryptoUtil;
import com.sandpolis.core.foundation.util.ValidationUtil;
import com.sandpolis.core.net.exelet.Exelet;
import com.sandpolis.core.net.exelet.ExeletContext;
import com.sandpolis.core.server.auth.otp.TimeBasedOneTimePasswordGenerator;
import com.sandpolis.core.server.user.User;
import com.sandpolis.core.clientserver.msg.MsgLogin.RQ_Login;
import com.sandpolis.core.clientserver.msg.MsgLogin.RQ_Logout;

/**
 * This {@link Exelet} handles login and logout requests from viewer instances.
 *
 * @author cilki
 * @since 4.0.0
 */
public final class LoginExe extends Exelet {

	private static final Logger log = LoggerFactory.getLogger(LoginExe.class);

	private static final TimeBasedOneTimePasswordGenerator TOTP;

	static {
		try {
			TOTP = new TimeBasedOneTimePasswordGenerator();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	@Handler(auth = true, instances = VIEWER)
	public static void rq_logout(ExeletContext context, RQ_Logout rq) {
		log.debug("Processing logout request from: {}", context.connector.getRemoteAddress());
		context.connector.close();
	}

	@Handler(auth = false, instances = VIEWER)
	public static MessageOrBuilder rq_login(ExeletContext context, RQ_Login rq) {
		log.debug("Processing login request from: {}", context.connector.getRemoteAddress());
		var outcome = begin();

		// Validate username
		String username = rq.getUsername();
		if (!ValidationUtil.username(username)) {
			log.debug("The username ({}) is invalid", username);
			return failure(outcome, INVALID_USERNAME);
		}

		User user = UserStore.getByUsername(username).orElse(null);
		if (user == null) {
			log.debug("The user ({}) does not exist", username);
			return failure(outcome, ACCESS_DENIED);
		}

		// Check expiration
		if (user.isExpired()) {
			log.debug("The user ({}) is expired", username);
			return failure(outcome, ACCESS_DENIED);
		}

		// Check OTP if required
		if (user.totpSecret().isPresent()) {
			var key = new SecretKeySpec(user.getTotpSecret(), TOTP.getAlgorithm());
			try {
				if (rq.getTotp() != TOTP.generateOneTimePassword(key, Instant.now())) {
					log.debug("OTP validation failed", username);
					return failure(outcome, ACCESS_DENIED);
				}
			} catch (InvalidKeyException e) {
				log.error("Invalid TOTP secret", e);
				return failure(outcome, ACCESS_DENIED);
			} finally {
				try {
					key.destroy();
				} catch (DestroyFailedException e) {
					log.error("Failed to destroy TOTP secret", e);
					return failure(outcome, ACCESS_DENIED);
				}
			}
		}

		// Check password
		if (!CryptoUtil.PBKDF2.check(rq.getPassword(), user.getHash())) {
			log.debug("Password validation failed", username);
			return failure(outcome, ACCESS_DENIED);
		}

		log.debug("Accepting login request for user: {}", username);

		// Mark connection as authenticated
		context.connector.authenticate();

		// Update login metadata
		ProfileStore.getViewer(username).ifPresentOrElse(profile -> {
			profile.viewer().ip().set(context.connector.getRemoteAddress());
		}, () -> {
			ProfileStore.create(profile -> {
				profile.uuid().set(context.connector.getRemoteUuid());
				profile.instanceType().set(context.connector.getRemoteInstance());
				profile.instanceFlavor().set(context.connector.getRemoteInstanceFlavor());
				profile.viewer().username().set(username);
				profile.viewer().ip().set(context.connector.getRemoteAddress());
			});
		});

		return success(outcome);
	}

	private LoginExe() {
	}
}
