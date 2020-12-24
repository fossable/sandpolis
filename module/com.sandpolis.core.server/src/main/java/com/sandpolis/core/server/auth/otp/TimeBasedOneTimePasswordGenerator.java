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
package com.sandpolis.core.server.auth.otp;

import javax.crypto.Mac;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;

public class TimeBasedOneTimePasswordGenerator extends HmacOneTimePasswordGenerator {
	private final Duration timeStep;

	/**
	 * The default time-step for a time-based one-time password generator (30
	 * seconds).
	 */
	public static final Duration DEFAULT_TIME_STEP = Duration.ofSeconds(30);

	/**
	 * A string identifier for the HMAC-SHA1 algorithm (required by HOTP and allowed
	 * by TOTP). HMAC-SHA1 is the default algorithm for TOTP.
	 */
	public static final String TOTP_ALGORITHM_HMAC_SHA1 = "HmacSHA1";

	/**
	 * A string identifier for the HMAC-SHA256 algorithm (allowed by TOTP).
	 */
	public static final String TOTP_ALGORITHM_HMAC_SHA256 = "HmacSHA256";

	/**
	 * A string identifier for the HMAC-SHA512 algorithm (allowed by TOTP).
	 */
	public static final String TOTP_ALGORITHM_HMAC_SHA512 = "HmacSHA512";

	/**
	 * Constructs a new time-based one-time password generator with a default
	 * time-step (30 seconds), password length
	 * ({@value com.sandpolis.core.server.auth.otp.HmacOneTimePasswordGenerator#DEFAULT_PASSWORD_LENGTH}
	 * decimal digits), and HMAC algorithm
	 * ({@value com.sandpolis.core.server.auth.otp.HmacOneTimePasswordGenerator#HOTP_HMAC_ALGORITHM}).
	 *
	 * @throws NoSuchAlgorithmException if the underlying JRE doesn't support the
	 *                                  {@value com.sandpolis.core.server.auth.otp.HmacOneTimePasswordGenerator#HOTP_HMAC_ALGORITHM}
	 *                                  algorithm, which should never happen except
	 *                                  in cases of serious misconfiguration
	 */
	public TimeBasedOneTimePasswordGenerator() throws NoSuchAlgorithmException {
		this(DEFAULT_TIME_STEP);
	}

	/**
	 * Constructs a new time-based one-time password generator with the given
	 * time-step and a default password length
	 * ({@value com.sandpolis.core.server.auth.otp.HmacOneTimePasswordGenerator#DEFAULT_PASSWORD_LENGTH}
	 * decimal digits) and HMAC algorithm
	 * ({@value com.sandpolis.core.server.auth.otp.HmacOneTimePasswordGenerator#HOTP_HMAC_ALGORITHM}).
	 *
	 * @param timeStep the time-step for this generator
	 *
	 * @throws NoSuchAlgorithmException if the underlying JRE doesn't support the
	 *                                  {@value com.sandpolis.core.server.auth.otp.HmacOneTimePasswordGenerator#HOTP_HMAC_ALGORITHM}
	 *                                  algorithm, which should never happen except
	 *                                  in cases of serious misconfiguration
	 */
	public TimeBasedOneTimePasswordGenerator(final Duration timeStep) throws NoSuchAlgorithmException {
		this(timeStep, HmacOneTimePasswordGenerator.DEFAULT_PASSWORD_LENGTH);
	}

	/**
	 * Constructs a new time-based one-time password generator with the given
	 * time-step and password length and a default HMAC algorithm
	 * ({@value com.sandpolis.core.server.auth.otp.HmacOneTimePasswordGenerator#HOTP_HMAC_ALGORITHM}).
	 *
	 * @param timeStep       the time-step for this generator
	 * @param passwordLength the length, in decimal digits, of the one-time
	 *                       passwords to be generated; must be between 6 and 8,
	 *                       inclusive
	 *
	 * @throws NoSuchAlgorithmException if the underlying JRE doesn't support the
	 *                                  {@value com.sandpolis.core.server.auth.otp.HmacOneTimePasswordGenerator#HOTP_HMAC_ALGORITHM}
	 *                                  algorithm, which should never happen except
	 *                                  in cases of serious misconfiguration
	 */
	public TimeBasedOneTimePasswordGenerator(final Duration timeStep, final int passwordLength)
			throws NoSuchAlgorithmException {
		this(timeStep, passwordLength, TOTP_ALGORITHM_HMAC_SHA1);
	}

	/**
	 * Constructs a new time-based one-time password generator with the given
	 * time-step, password length, and HMAC algorithm.
	 *
	 * @param timeStep       the time-step for this generator
	 * @param passwordLength the length, in decimal digits, of the one-time
	 *                       passwords to be generated; must be between 6 and 8,
	 *                       inclusive
	 * @param algorithm      the name of the {@link javax.crypto.Mac} algorithm to
	 *                       use when generating passwords; TOTP allows for
	 *                       {@value com.sandpolis.core.server.auth.otp.TimeBasedOneTimePasswordGenerator#TOTP_ALGORITHM_HMAC_SHA1},
	 *                       {@value com.sandpolis.core.server.auth.otp.TimeBasedOneTimePasswordGenerator#TOTP_ALGORITHM_HMAC_SHA256},
	 *                       and
	 *                       {@value com.sandpolis.core.server.auth.otp.TimeBasedOneTimePasswordGenerator#TOTP_ALGORITHM_HMAC_SHA512}
	 *
	 * @throws NoSuchAlgorithmException if the underlying JRE doesn't support the
	 *                                  given algorithm
	 *
	 * @see com.sandpolis.core.server.auth.otp.TimeBasedOneTimePasswordGenerator#TOTP_ALGORITHM_HMAC_SHA1
	 * @see com.sandpolis.core.server.auth.otp.TimeBasedOneTimePasswordGenerator#TOTP_ALGORITHM_HMAC_SHA256
	 * @see com.sandpolis.core.server.auth.otp.TimeBasedOneTimePasswordGenerator#TOTP_ALGORITHM_HMAC_SHA512
	 */
	public TimeBasedOneTimePasswordGenerator(final Duration timeStep, final int passwordLength, final String algorithm)
			throws NoSuchAlgorithmException {
		super(passwordLength, algorithm);

		this.timeStep = timeStep;
	}

	/**
	 * Generates a one-time password using the given key and timestamp.
	 *
	 * @param key       the key to be used to generate the password
	 * @param timestamp the timestamp for which to generate the password
	 *
	 * @return an integer representation of a one-time password; callers will need
	 *         to format the password for display on their own
	 *
	 * @throws InvalidKeyException if the given key is inappropriate for
	 *                             initializing the {@link Mac} for this generator
	 */
	public int generateOneTimePassword(final Key key, final Instant timestamp) throws InvalidKeyException {
		return this.generateOneTimePassword(key, timestamp.toEpochMilli() / this.timeStep.toMillis());
	}

	/**
	 * Returns the time step used by this generator.
	 *
	 * @return the time step used by this generator
	 */
	public Duration getTimeStep() {
		return this.timeStep;
	}
}
