//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.foundation;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.random.RandomGenerator;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import com.google.common.io.BaseEncoding;

public record S7SPassword(String password) {

	/**
	 * The length of a password salt in bytes.
	 */
	private static final int SALT_LENGTH = 16;

	/**
	 * The length of a password hash in bytes.
	 */
	private static final int HASH_LENGTH = 48;

	/**
	 * The number of iterations to run.
	 */
	private static final int ITERATIONS = 4284;

	public static S7SPassword of(String password) {
		return new S7SPassword(password);
	}

	/**
	 * Compute a hash of the given password with a random salt. Repeated calls to
	 * this method should always produce different outputs.
	 *
	 * @return A PBKDF2 hash of the password
	 */
	public String hashPBKDF2() {

		byte[] salt = new byte[SALT_LENGTH];
		S7SRandom.secure.nextBytes(salt);

		byte[] hash = computePBKDF2(password.toCharArray(), salt, ITERATIONS, HASH_LENGTH);
		return ITERATIONS + ":" + BaseEncoding.base16().encode(salt) + ":" + BaseEncoding.base64().encode(hash);
	}

	/**
	 * Check the password against the given hash.
	 *
	 * @param hash The reference password hash
	 * @return True if the password matches the hash; false otherwise
	 */
	public boolean checkPBKDF2(String hash) {
		if (hash == null)
			throw new IllegalArgumentException();

		String[] params = hash.split(":");
		if (params.length != 3)
			throw new IllegalArgumentException("Invalid hash structure");

		// Decode hash parameters
		int iterations = Integer.parseInt(params[0]);
		byte[] salt = BaseEncoding.base16().decode(params[1]);
		byte[] h = BaseEncoding.base64().decode(params[2]);

		return Arrays.equals(h, computePBKDF2(password.toCharArray(), salt, iterations, h.length));
	}

	/**
	 * Compute a raw PBKDF2 hash.
	 *
	 * @param pass       The characters to hash
	 * @param salt       The salt to include in the hash
	 * @param iterations The number of iterations to perform
	 * @param bytes      The size of the key
	 * @return The PBKDF2 hash
	 */
	private static byte[] computePBKDF2(char[] pass, byte[] salt, int iterations, int bytes) {
		PBEKeySpec spec = new PBEKeySpec(pass, salt, iterations, bytes * 8);
		try {
			return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
		} catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
}
