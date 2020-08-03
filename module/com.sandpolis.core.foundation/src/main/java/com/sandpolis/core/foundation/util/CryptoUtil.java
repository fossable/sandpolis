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
package com.sandpolis.core.foundation.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;

/**
 * Cryptographic utilities including password hashing and SAND5 key generation.
 *
 * @author cilki
 * @since 4.0.0
 */
public final class CryptoUtil {
	private CryptoUtil() {
	}

	/**
	 * Sign the nonce using SHA256.
	 *
	 * @param nonce The data to be signed
	 * @param key   The signing key
	 * @return The signed nonce in base64
	 */
	public static String sign(String nonce, String key) {
		return sign(nonce, key.getBytes());
	}

	/**
	 * Sign the nonce using SHA256.
	 *
	 * @param nonce The data to be signed
	 * @param key   The signing key
	 * @return The signed nonce in base64
	 */
	public static String sign(String nonce, byte[] key) {
		return sign(nonce.getBytes(), key);
	}

	/**
	 * Sign the nonce using SHA256.
	 *
	 * @param nonce The data to be signed
	 * @param key   The signing key
	 * @return The signed nonce in base64
	 */
	public static String sign(byte[] nonce, byte[] key) {
		return BaseEncoding.base64()
				.encode(Hashing.sha256().newHasher().putBytes(nonce).putBytes(key).hash().asBytes());
	}

	/**
	 * Get a random byte array of given size.
	 *
	 * @param length The size of the resulting nonce
	 * @return A secure nonce
	 */
	public static byte[] getNonce(int length) {
		byte[] nonce = new byte[length];
		new SecureRandom().nextBytes(nonce);
		return nonce;
	}

	/**
	 * A wrapper for the Password-Based Key Derivation Function 2 (PBKDF2).
	 *
	 * @author cilki
	 * @since 5.0.0
	 */
	public static final class PBKDF2 {

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

		/**
		 * Compute a hash of the given password with a random salt. Repeated calls to
		 * this method with the same input should always produce different outputs.
		 *
		 * @param pass The password to hash
		 * @return A PBKDF2 hash of the password
		 */
		public static String hash(String pass) {
			if (pass == null)
				throw new IllegalArgumentException();

			byte[] salt = CryptoUtil.getNonce(SALT_LENGTH);
			byte[] hash = pbkdf2(pass.toCharArray(), salt, ITERATIONS, HASH_LENGTH);
			return ITERATIONS + ":" + BaseEncoding.base16().encode(salt) + ":" + BaseEncoding.base64().encode(hash);
		}

		/**
		 * Check a password against a known hash. This implementation does not run in
		 * constant time, so brute-force timing attacks could possibly reveal the hash
		 * given enough attempts.
		 *
		 * @param pass The password to verify
		 * @param ref  The reference password hash
		 * @return True if the password matches the hash; false otherwise
		 */
		public static boolean check(String pass, String ref) {
			if (pass == null)
				throw new IllegalArgumentException();
			if (ref == null)
				throw new IllegalArgumentException();

			String[] params = ref.split(":");
			if (params.length != 3)
				throw new IllegalArgumentException("Invalid hash structure");

			// Decode hash parameters
			int iterations = Integer.parseInt(params[0]);
			byte[] salt = BaseEncoding.base16().decode(params[1]);
			byte[] hash = BaseEncoding.base64().decode(params[2]);

			// Additional check to ensure both parameters are not simultaneously null, which
			// would cause the method to return true
			if (hash == null)
				throw new NullPointerException();

			return Arrays.equals(hash, pbkdf2(pass.toCharArray(), salt, iterations, hash.length));
		}

		/**
		 * Compute a raw PBKDF2 hash.
		 *
		 * @param pass       The characters to hash
		 * @param salt       The unencoded salt to include in the hash
		 * @param iterations The number of iterations to perform
		 * @param bytes      The size of the key
		 * @return The unencoded PBKDF2 hash
		 */
		public static byte[] pbkdf2(char[] pass, byte[] salt, int iterations, int bytes) {
			PBEKeySpec spec = new PBEKeySpec(pass, salt, iterations, bytes * 8);
			try {
				return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded();
			} catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
		}

	}

}
