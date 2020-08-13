package com.sandpolis.core.net.handler.sand5;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
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
import java.util.Objects;

import com.sandpolis.core.instance.state.Document;

public class ReciprocalKeyPair extends VirtReciprocalKeyPair {

	/**
	 * The key size in bits.
	 */
	public static final int KEY_SIZE = 1024;

	/**
	 * The size of the verification nonce in bytes.
	 */
	public static final int NONCE_SIZE = 512;

	public ReciprocalKeyPair(Document document, byte[] signer, byte[] verifier) {
		super(document);
	}

	/**
	 * Sign the given nonce.
	 *
	 * @param nonce The original nonce
	 * @return The signed nonce
	 */
	public byte[] sign(byte[] nonce) {
		Objects.requireNonNull(nonce);

		try {
			PrivateKey prv = KeyFactory.getInstance("DSA").generatePrivate(new PKCS8EncodedKeySpec(getSigner()));
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
	 * @param nonce     The original nonce
	 * @param signature The signed nonce
	 * @return True if {@code signature} was produced by the reciprocal SAND5
	 *         keypair; false otherwise
	 */
	public boolean check(byte[] nonce, byte[] signature) {
		Objects.requireNonNull(nonce);
		Objects.requireNonNull(signature);

		try {
			PublicKey pub = KeyFactory.getInstance("DSA").generatePublic(new X509EncodedKeySpec(getVerifier()));
			Signature dsa = Signature.getInstance("SHA1withDSA", "SUN");
			dsa.initVerify(pub);
			dsa.update(nonce);
			return dsa.verify(signature);
		} catch (SignatureException | InvalidKeySpecException | InvalidKeyException | NoSuchProviderException
				| NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public static ReciprocalKeyPair[] generate() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("DSA", "SUN");
			generator.initialize(KEY_SIZE, SecureRandom.getInstance("SHA1PRNG", "SUN"));

			var k1 = generator.generateKeyPair();
			var k2 = generator.generateKeyPair();

			return new ReciprocalKeyPair[] {
					// The client keypair (consists of k1's private key and k2's public key)
					new ReciprocalKeyPair(null, k1.getPrivate().getEncoded(), k2.getPublic().getEncoded()),
					// The server keypair (consists of k1's public key and k2's private key)
					new ReciprocalKeyPair(null, k2.getPrivate().getEncoded(), k1.getPublic().getEncoded()) };
		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			throw new RuntimeException(e);
		}
	}
}
