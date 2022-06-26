//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.server.auth;

import static org.s7s.core.foundation.Instance.InstanceType.CLIENT;
import static org.s7s.core.instance.profile.ProfileStore.ProfileStore;
import static org.s7s.core.server.user.UserStore.UserStore;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.regex.Pattern;

import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import org.s7s.core.protocol.Session.RQ_Login;
import org.s7s.core.protocol.Session.RQ_Logout;
import org.s7s.core.protocol.Session.RS_Login;
import org.s7s.core.protocol.Session.RS_Logout;
import org.s7s.core.foundation.S7SPassword;
import org.s7s.core.foundation.S7SRandom;
import org.s7s.core.instance.state.InstanceOids.ProfileOid.ConnectionOid;
import org.s7s.core.instance.state.InstanceOids.ProfileOid;
import org.s7s.core.instance.state.InstanceOids.ProfileOid.ClientOid;
import org.s7s.core.instance.state.InstanceOids.UserOid;
import org.s7s.core.instance.exelet.Exelet;
import org.s7s.core.instance.exelet.ExeletContext;
import org.s7s.core.server.user.User;

/**
 * This {@link Exelet} handles login and logout requests from client instances.
 *
 * @author cilki
 * @since 4.0.0
 */
public final class LoginExe extends Exelet {

	private static final Logger log = LoggerFactory.getLogger(LoginExe.class);

	private static final Pattern USERNAME_VALIDATOR = Pattern.compile("^[a-z]{4,32}$");

	private static final TimeBasedOneTimePasswordGenerator TOTP;

	static {
		try {
			TOTP = new TimeBasedOneTimePasswordGenerator();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	@Handler(auth = true, instances = CLIENT)
	public static RS_Logout rq_logout(ExeletContext context, RQ_Logout rq) {
		log.debug("Processing logout request from: {}", context.connector.get(ConnectionOid.REMOTE_ADDRESS).asString());
		context.connector.close();

		return RS_Logout.LOGOUT_OK;
	}

	@Handler(auth = false, instances = CLIENT)
	public static RS_Login rq_login(ExeletContext context, RQ_Login rq) throws InterruptedException {
		log.debug("Processing login request from: {}", context.connector.get(ConnectionOid.REMOTE_ADDRESS).asString());

		// Validate username
		String username = rq.getUsername();
		if (!USERNAME_VALIDATOR.matcher(username).matches()) {
			log.debug("The username ({}) is invalid", username);
			return RS_Login.LOGIN_INVALID_USERNAME;
		}

		User user = UserStore.getByUsername(username).orElse(null);
		if (user == null) {
			log.debug("The user ({}) does not exist", username);
			return RS_Login.LOGIN_FAILED;
		}

		// Check expiration
		if (user.isExpired()) {
			log.debug("The user ({}) is expired", username);
			return RS_Login.LOGIN_FAILED_EXPIRED_USER;
		}

		// Check OTP if required
		if (user.get(UserOid.TOTP_SECRET).isPresent()) {
			var key = new SecretKeySpec(user.get(UserOid.TOTP_SECRET).asBytes(), TOTP.getAlgorithm());
			try {
				if (rq.getToken() != TOTP.generateOneTimePassword(key, Instant.now())) {
					log.debug("OTP validation failed", username);

					// Sleep a random amount of time on failure
					Thread.sleep(S7SRandom.secure.nextLong(100, 500));
					return RS_Login.LOGIN_FAILED;
				}
			} catch (InvalidKeyException e) {
				log.error("Invalid TOTP secret", e);
				return RS_Login.LOGIN_INVALID_TOKEN;
			} finally {
				try {
					key.destroy();
				} catch (DestroyFailedException e) {
					log.error("Failed to destroy TOTP secret", e);
					return RS_Login.LOGIN_FAILED;
				}
			}
		}

		// Check password
		if (!S7SPassword.of(rq.getPassword()).checkPBKDF2(user.get(UserOid.HASH).asString())) {
			log.debug("Password validation failed", username);

			// Sleep a random amount of time on failure
			Thread.sleep(S7SRandom.secure.nextLong(100, 500));
			return RS_Login.LOGIN_FAILED;
		}

		log.debug("Accepting login request for user: {}", username);

		// Mark connection as authenticated
		context.connector.authenticate();

		// Update login metadata
		ProfileStore.getClient(username).ifPresentOrElse(profile -> {
			profile.set(ClientOid.IP, context.connector.get(ConnectionOid.REMOTE_ADDRESS).asString());
		}, () -> {
			ProfileStore.create(profile -> {
				profile.set(ProfileOid.UUID, context.connector.get(ConnectionOid.REMOTE_ADDRESS).asString());
				profile.set(ProfileOid.INSTANCE_TYPE,
						context.connector.get(ConnectionOid.REMOTE_INSTANCE).asInstanceType());
				profile.set(ProfileOid.INSTANCE_FLAVOR,
						context.connector.get(ConnectionOid.REMOTE_INSTANCE_FLAVOR).asInstanceFlavor());
				profile.set(ClientOid.USERNAME, username);
				profile.set(ClientOid.IP, context.connector.get(ConnectionOid.REMOTE_ADDRESS).asString());
			});
		});

		// TODO append instead of replace
		user.set(UserOid.CURRENT_SID, new int[] { context.connector.get(ConnectionOid.REMOTE_SID).asInt() });

		return RS_Login.LOGIN_OK;
	}

	private LoginExe() {
	}
}
