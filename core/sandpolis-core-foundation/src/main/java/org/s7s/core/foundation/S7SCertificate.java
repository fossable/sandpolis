//============================================================================//
//                                                                            //
//            Copyright © 2015 - 2022 Sandpolis Software Foundation           //
//                                                                            //
//  This source file is subject to the terms of the Mozilla Public License    //
//  version 2. You may not use this file except in compliance with the MPLv2. //
//                                                                            //
//============================================================================//
package org.s7s.core.foundation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;

public record S7SCertificate(X509Certificate certificate) {

	public static S7SCertificate of(X509Certificate certificate) {
		return new S7SCertificate(certificate);
	}

	public static S7SCertificate of(String string) throws CertificateException {
		try (InputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(string))) {
			return of(in);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static S7SCertificate of(byte[] bytes) throws CertificateException {
		try (InputStream in = new ByteArrayInputStream(bytes)) {
			return of(in);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static S7SCertificate of(Path file) throws CertificateException, IOException {
		return of(Files.newInputStream(file));
	}

	public static S7SCertificate of(InputStream input) throws CertificateException {
		return new S7SCertificate((X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(input));
	}

	/**
	 * Get the root server certificate. This certificate is used for authenticating
	 * server connections.
	 *
	 * @return The root certificate
	 * @throws IOException
	 */
	public static S7SCertificate getServerRoot() throws IOException {
		try (InputStream in = S7SCertificate.class.getResourceAsStream("/cert/server.cert")) {
			return of(in);
		} catch (CertificateException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the root plugin certificate. This certificate is used for authenticating
	 * plugin binaries.
	 *
	 * @return The root certificate
	 * @throws IOException
	 */
	public static S7SCertificate getPluginRoot() {
		try (InputStream in = S7SCertificate.class.getResourceAsStream("/cert/plugin.cert")) {
			return of(in);
		} catch (CertificateException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static S7SCertificate getDefaultCert() {
		try (InputStream in = S7SCertificate.class.getResourceAsStream("/cert/default.cert")) {
			return of(in);
		} catch (CertificateException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static PrivateKey getDefaultKey() {
		try (InputStream in = S7SCertificate.class.getResourceAsStream("/cert/default.key")) {
			return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(in.readAllBytes()));
		} catch (NoSuchAlgorithmException | IOException | InvalidKeySpecException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Indicates whether the given certificate is within the timestamp constraints.
	 *
	 * @return True if the current time is after the begin timestamp, but before the
	 *         expiration timestamp
	 */
	public boolean checkValidity() {
		try {
			Objects.requireNonNull(certificate).checkValidity();
			return true;
		} catch (CertificateExpiredException | CertificateNotYetValidException e) {
			return false;
		}
	}

	/**
	 * Check that the given hostname matches the given certificate.
	 *
	 * @param hostname
	 * @return
	 * @throws CertificateParsingException
	 */
	public boolean checkHostname(String hostname) throws CertificateException {
		var sans = certificate.getSubjectAlternativeNames();
		if (sans == null)
			throw new CertificateException();

		for (var san : sans) {
			if ((int) san.get(0) == 2) { // 2 indicates DNS
				if (hostname.equals(san.get(1)))
					return true;
			}
		}

		return false;
	}

	/**
	 * Get certificate information as a formatted String.
	 *
	 * @return A nicely formatted String
	 */
	public String getInfoString() {

		StringBuffer buffer = new StringBuffer();

		// signature algorithm
		buffer.append(String.format("Signature: (%s)%n", certificate.getSigAlgName()));

		// principal name
		buffer.append(String.format("\t%s%n", certificate.getSubjectX500Principal().getName()));

		// validity
		buffer.append(String.format("\tValidity%n"));
		buffer.append(String.format("\t\tNot Before: %s%n", certificate.getNotBefore()));
		buffer.append(String.format("\t\tNot After: %s%n", certificate.getNotAfter()));

		// public key
		PublicKey pub = certificate.getPublicKey();
		buffer.append(String.format("Public key: (%s)%n", pub.getAlgorithm()));
		buffer.append(formatKey(pub.getEncoded(), 16, "\t"));

		return buffer.toString();
	}

	/**
	 * Format a binary key in hexadecimal-colon form.
	 *
	 * @param data    Key data
	 * @param columns Formatted key width in bytes
	 * @param padding A padding String to go before the first byte in each row
	 * @return The formatted key
	 */
	private static String formatKey(byte[] data, int columns, String padding) {
		StringBuffer buffer = new StringBuffer();

		for (int i = 0; i < data.length;) {
			buffer.append(padding);
			for (int j = 0; j < columns && i < data.length; j++, i++) {
				buffer.append(String.format("%02x:", data[i]));
			}
			buffer.append('\n');
		}

		// fencepost
		buffer.deleteCharAt(buffer.length() - 2);
		return buffer.toString();
	}

	/**
	 * Get certificate information as a formatted HTML String.
	 *
	 * @return A nicely formatted HTML String
	 */
	public String getInfoHtml() {
		return String.format("<html>%s</html>",
				getInfoString().replaceAll(String.format("%n"), "<br>").replaceAll("\t", "&emsp;"));
	}

}
