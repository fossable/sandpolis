/*******************************************************************************
 *                                                                             *
 *                Copyright © 2015 - 2019 Subterranean Security                *
 *                                                                             *
 *  Licensed under the Apache License, Version 2.0 (the "License");            *
 *  you may not use this file except in compliance with the License.           *
 *  You may obtain a copy of the License at                                    *
 *                                                                             *
 *      http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                             *
 *  Unless required by applicable law or agreed to in writing, software        *
 *  distributed under the License is distributed on an "AS IS" BASIS,          *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 *  See the License for the specific language governing permissions and        *
 *  limitations under the License.                                             *
 *                                                                             *
 ******************************************************************************/
package com.sandpolis.core.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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

/**
 * Certificate utilities.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class CertUtil {

	/**
	 * Get the root server certificate. This certificate is used for authenticating
	 * server connections.
	 *
	 * @return The root certificate
	 */
	public static X509Certificate getServerRoot() {
		try (InputStream in = CertUtil.class.getResourceAsStream("/cert/server.cert")) {
			return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the root plugin certificate. This certificate is used for authenticating
	 * plugin binaries.
	 *
	 * @return The root certificate
	 */
	public static X509Certificate getPluginRoot() {
		try (InputStream in = CertUtil.class.getResourceAsStream("/cert/plugin.cert")) {
			return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Indicates whether the given certificate is within the timestamp constraints.
	 *
	 * @param cert The certificate to check
	 * @return True if the current time is after the begin timestamp, but before the
	 *         expiration timestamp
	 */
	public static boolean checkValidity(X509Certificate cert) {
		try {
			Objects.requireNonNull(cert).checkValidity();
			return true;
		} catch (CertificateExpiredException | CertificateNotYetValidException e) {
			return false;
		}
	}

	/**
	 * Check that the given hostname matches the given certificate.
	 *
	 * @param cert
	 * @param hostname
	 * @return
	 * @throws CertificateParsingException
	 */
	public static boolean checkHostname(X509Certificate cert, String hostname) throws CertificateException {
		var sans = cert.getSubjectAlternativeNames();
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
	 * Load an {@link X509Certificate}.
	 *
	 * @param cert A Base64-encoded certificate
	 * @return The new certificate
	 * @throws CertificateException If the certificate format is invalid
	 */
	public static X509Certificate parseCert(String cert) throws CertificateException {
		return parseCert(Base64.getDecoder().decode(Objects.requireNonNull(cert)));
	}

	/**
	 * Load an {@link X509Certificate}.
	 *
	 * @param cert A certificate on the filesystem
	 * @return The new certificate
	 * @throws CertificateException If the certificate format is invalid
	 * @throws IOException          If there was an error reading the file
	 */
	public static X509Certificate parseCert(File cert) throws CertificateException, IOException {
		return parseCert(Files.readAllBytes(Objects.requireNonNull(cert).toPath()));
	}

	/**
	 * Load an {@link X509Certificate}.
	 *
	 * @param cert A decoded certificate
	 * @return The new certificate
	 * @throws CertificateException If the certificate format is invalid
	 */
	public static X509Certificate parseCert(byte[] cert) throws CertificateException {
		Objects.requireNonNull(cert);

		try (InputStream in = new ByteArrayInputStream(cert)) {
			return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Load a {@link PrivateKey}.
	 *
	 * @param key A decoded private key
	 * @return The new private key
	 * @throws InvalidKeySpecException If the key format is invalid
	 */
	public static PrivateKey parseKey(byte[] key) throws InvalidKeySpecException {
		try {
			return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(key));
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get certificate information as a formatted String.
	 *
	 * @param cert A Base64-encoded certificate
	 * @return A nicely formatted String
	 * @throws CertificateException If the certificate format is invalid
	 */
	public static String getInfoString(String cert) throws CertificateException {
		return getInfoString(parseCert(Objects.requireNonNull(cert)));
	}

	/**
	 * Get certificate information as a formatted String.
	 *
	 * @param cert A certificate
	 * @return A nicely formatted String
	 */
	public static String getInfoString(X509Certificate cert) {
		Objects.requireNonNull(cert);

		StringBuffer buffer = new StringBuffer();

		// signature algorithm
		buffer.append(String.format("Signature: (%s)%n", cert.getSigAlgName()));

		// principal name
		buffer.append(String.format("\t%s%n", cert.getSubjectX500Principal().getName()));

		// validity
		buffer.append(String.format("\tValidity%n"));
		buffer.append(String.format("\t\tNot Before: %s%n", cert.getNotBefore()));
		buffer.append(String.format("\t\tNot After: %s%n", cert.getNotAfter()));

		// public key
		PublicKey pub = cert.getPublicKey();
		buffer.append(String.format("Public key: (%s)%n", pub.getAlgorithm()));
		buffer.append(formatKey(pub.getEncoded(), 16, "\t"));

		return buffer.toString();
	}

	/**
	 * Get certificate information as a formatted HTML String.
	 *
	 * @param cert A certificate
	 * @return A nicely formatted HTML String
	 */
	public static String getInfoHtml(X509Certificate cert) {
		Objects.requireNonNull(cert);

		return String.format("<html>%s</html>",
				getInfoString(cert).replaceAll(String.format("%n"), "<br>").replaceAll("\t", "&emsp;"));
	}

	/**
	 * Format a binary key in hexidecimal-colon form.
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

	private CertUtil() {
	}
}
