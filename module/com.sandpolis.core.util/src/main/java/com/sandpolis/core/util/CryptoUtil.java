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
package com.sandpolis.core.util;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

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
	 * The insecure MD5 algorithm which should be rarely used.
	 */
	public static final MessageDigest MD5;

	/**
	 * The weak SHA1 algorithm which shoud be rarely used.
	 */
	public static final MessageDigest SHA1;

	/**
	 * The SHA256 algorithm.
	 */
	public static final MessageDigest SHA256;

	static {
		try {
			MD5 = MessageDigest.getInstance("MD5");
			SHA1 = MessageDigest.getInstance("SHA-1");
			SHA256 = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Compute a hash of the inputs. Parameter order changes the result of the hash!
	 *
	 * @param digest The hash algorithm
	 * @param input  The hash inputs
	 * @return The hash as a lowercase hexadecimal String
	 */
	public static String hash(MessageDigest digest, char[]... input) {
		synchronized (digest) {
			digest.reset();
			for (char[] array : input)
				for (char c : array) {
					// Digest each byte of each char
					digest.update((byte) (c & 0xFF00));
					digest.update((byte) (c & 0xFF));
				}

			return BaseEncoding.base16().lowerCase().encode(digest.digest());
		}
	}

	/**
	 * Compute a hash of the inputs. Parameter order changes the result of the hash!
	 *
	 * @param digest The hash algorithm
	 * @param input  The hash inputs
	 * @return The unencoded hash
	 */
	public static byte[] hash(MessageDigest digest, byte[]... input) {
		synchronized (digest) {
			digest.reset();
			for (byte[] b : input)
				digest.update(b);

			return digest.digest();
		}
	}

	/**
	 * Compute a hash of the inputs. Parameter order changes the result of the hash!
	 *
	 * @param digest The hash algorithm
	 * @param input  The hash inputs
	 * @return The hash as a lowercase hexadecimal String
	 */
	public static String hash(MessageDigest digest, String... input) {

		// Convert Strings to bytes
		byte[][] in = new byte[input.length][];
		for (int i = 0; i < in.length; i++)
			in[i] = input[i].getBytes();

		return BaseEncoding.base16().lowerCase().encode(hash(digest, in));
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
		return BaseEncoding.base64().encode(hash(SHA256, nonce, key));
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
	 * This class provides fundamental SAND5 utilities and the
	 * {@code ReciprocalKeyPair} class which is essential to the authentication
	 * process.
	 *
	 * @author cilki
	 * @since 5.0.0
	 */
	public static final class SAND5 {

		/**
		 * The key size in bits.
		 */
		public static final int KEY_SIZE = 1024;

		/**
		 * The size of the verification nonce in bytes.
		 */
		public static final int NONCE_SIZE = 512;

		/**
		 * A {@code ReciprocalKeyPair} contains two keys and allows the instance to both
		 * sign and verify nonces.<br>
		 *
		 * Each {@code ReciprocalKeyPair} has a public and private key that are
		 * completely independent. The corresponding private key for the public key in a
		 * given {@code ReciprocalKeyPair} is located in the keypair's reciprocal
		 * {@code ReciprocalKeyPair} (which may be located on a different machine).
		 *
		 * @author cilki
		 * @since 5.0.0
		 */
		@Entity
		public static class ReciprocalKeyPair {

			@Id
			@Column
			@GeneratedValue(strategy = GenerationType.AUTO)
			private int db_id;

			@Column(nullable = false)
			private byte[] signer;

			@Column(nullable = false)
			private byte[] verifier;

			public ReciprocalKeyPair() {
			}

			public ReciprocalKeyPair(byte[] signer, byte[] verifier) {
				this.signer = signer;
				this.verifier = verifier;
			}

			public byte[] getSigner() {
				return signer;
			}

			public byte[] getVerifier() {
				return verifier;
			}

		}

		/**
		 * Generate a new DSA keypair for use as a SAND5 reciprocal keypair.
		 *
		 * @return A new {@code KeyPair}
		 */
		public static KeyPair generate() {
			try {
				KeyPairGenerator generator = KeyPairGenerator.getInstance("DSA", "SUN");
				generator.initialize(KEY_SIZE, SecureRandom.getInstance("SHA1PRNG", "SUN"));

				return generator.generateKeyPair();
			} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * Sign the given nonce.
		 *
		 * @param key   The SAND5 keypair to use during signing
		 * @param nonce The original nonce
		 * @return The signed nonce
		 */
		public static byte[] sign(ReciprocalKeyPair key, byte[] nonce) {
			if (key == null)
				throw new IllegalArgumentException();
			if (nonce == null)
				throw new IllegalArgumentException();

			try {
				PrivateKey prv = KeyFactory.getInstance("DSA")
						.generatePrivate(new PKCS8EncodedKeySpec(key.getSigner()));
				Signature dsa = Signature.getInstance("SHA1withDSA", "SUN");
				dsa.initSign(prv);
				dsa.update(nonce);
				return dsa.sign();
			} catch (SignatureException | InvalidKeySpecException | InvalidKeyException | NoSuchProviderException
					| NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * Verify a SAND5 signature.
		 *
		 * @param key       The SAND5 keypair to use during verification
		 * @param nonce     The original nonce
		 * @param signature The signed nonce
		 * @return True if {@code signature} was produced by the reciprocal SAND5
		 *         keypair; false otherwise
		 */
		public static boolean check(ReciprocalKeyPair key, byte[] nonce, byte[] signature) {
			if (key == null)
				throw new IllegalArgumentException();
			if (nonce == null)
				throw new IllegalArgumentException();
			if (signature == null)
				throw new IllegalArgumentException();

			try {
				PublicKey pub = KeyFactory.getInstance("DSA").generatePublic(new X509EncodedKeySpec(key.getVerifier()));
				Signature dsa = Signature.getInstance("SHA1withDSA", "SUN");
				dsa.initVerify(pub);
				dsa.update(nonce);
				return dsa.verify(signature);
			} catch (SignatureException | InvalidKeySpecException | InvalidKeyException | NoSuchProviderException
					| NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * Get a random nonce for use in a SAND5 exchange.
		 *
		 * @return A random nonce of size {@link NONCE_SIZE}
		 */
		public static byte[] getNonce() {
			return CryptoUtil.getNonce(NONCE_SIZE);
		}
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
