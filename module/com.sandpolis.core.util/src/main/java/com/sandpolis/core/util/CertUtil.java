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
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * Certificate utilities.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class CertUtil {
	private CertUtil() {
	}

	/**
	 * Load a {@link X509Certificate}.
	 *
	 * @param cert A Base64-encoded certificate
	 * @return The new certificate
	 * @throws CertificateException If the certificate format is invalid
	 */
	public static X509Certificate parse(String cert) throws CertificateException {
		if (cert == null)
			throw new IllegalArgumentException();

		return parse(Base64.getDecoder().decode(cert));
	}

	/**
	 * Load a {@link X509Certificate}.
	 *
	 * @param cert A certificate on the filesystem
	 * @return The new certificate
	 * @throws CertificateException If the certificate format is invalid
	 * @throws IOException          If there was an error reading the file
	 */
	public static X509Certificate parse(File cert) throws CertificateException, IOException {
		if (cert == null)
			throw new IllegalArgumentException();

		return parse(Files.readAllBytes(cert.toPath()));
	}

	/**
	 * Load a {@link X509Certificate}.
	 *
	 * @param cert An unencoded certificate
	 * @return The new certificate
	 * @throws CertificateException If the certificate format is invalid
	 */
	public static X509Certificate parse(byte[] cert) throws CertificateException {
		if (cert == null)
			throw new IllegalArgumentException();

		try (InputStream in = new ByteArrayInputStream(cert)) {
			return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
		} catch (IOException e) {
			throw new IllegalArgumentException("The argument caused an IOException");
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
		if (cert == null)
			throw new IllegalArgumentException();

		return getInfoString(parse(cert));
	}

	/**
	 * Get certificate information as a formatted String.
	 *
	 * @param cert A certificate
	 * @return A nicely formatted String
	 */
	public static String getInfoString(X509Certificate cert) {
		if (cert == null)
			throw new IllegalArgumentException();

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
		if (cert == null)
			throw new IllegalArgumentException();

		return String.format("<html>%s</html>",
				getInfoString(cert).replaceAll(String.format("%n"), "<br>").replaceAll("\t", "&emsp;"));
	}

	/**
	 * Format a binary key into hexidecimal colon format.
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
	 * Get the root Sandpolis certificate.
	 *
	 * @return The root certificate
	 * @throws CertificateException If the certificate format is invalid
	 * @throws IOException          If there was an error reading the resource
	 */
	public static X509Certificate getRoot() throws CertificateException, IOException {
		try (InputStream in = CertUtil.class.getResourceAsStream("/cert/root.cert")) {
			return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
		}
	}

	/**
	 * Indicates whether the given certificate is within the timestamp constraints.
	 *
	 * @param certificate The certificate to check
	 * @return True if the current time is after the begin timestamp, but before the
	 *         expiration timestamp
	 */
	public static boolean getValidity(X509Certificate certificate) {
		if (certificate == null)
			throw new IllegalArgumentException();

		try {
			certificate.checkValidity();
			return true;
		} catch (CertificateExpiredException | CertificateNotYetValidException e) {
			return false;
		}
	}
}
