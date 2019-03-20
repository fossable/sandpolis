/******************************************************************************
 *                                                                            *
 *                    Copyright 2019 Subterranean Security                    *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *      http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 *                                                                            *
 *****************************************************************************/
package com.sandpolis.server.store.trust;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Resources;
import com.sandpolis.core.instance.MainDispatch;
import com.sandpolis.core.instance.Store.ManualInitializer;
import com.sandpolis.core.instance.storage.StoreProvider;
import com.sandpolis.core.instance.storage.StoreProviderFactory;
import com.sandpolis.core.instance.storage.database.Database;
import com.sandpolis.core.util.CertUtil;

/**
 * The {@link TrustStore} contains trust anchors for plugin certificate
 * authorities.
 * 
 * @author cilki
 * @since 5.0.0
 */
@ManualInitializer
public final class TrustStore {

	private static final Logger log = LoggerFactory.getLogger(TrustStore.class);

	private static StoreProvider<TrustAnchor> provider;

	public static void init(StoreProvider<TrustAnchor> provider) {
		TrustStore.provider = Objects.requireNonNull(provider);

		// Install root CA if required
		if (get("PLUGIN CA").isEmpty()) {
			try {
				add(new TrustAnchor("PLUGIN CA",
						CertUtil.parse(Resources.toByteArray(MainDispatch.class.getResource("/cert/plugin.cert")))));
			} catch (CertificateException | IOException e) {
				throw new RuntimeException("Failed to load certificate", e);
			}
		}

		if (log.isDebugEnabled())
			log.debug("Initialized store containing {} entities", provider.count());
	}

	public static void load(Database main) {
		init(StoreProviderFactory.database(TrustAnchor.class, Objects.requireNonNull(main)));
	}

	/**
	 * Add a new trust anchor to the store.
	 * 
	 * @param anchor A new trust anchor
	 */
	public static void add(TrustAnchor anchor) {
		provider.add(Objects.requireNonNull(anchor));
	}

	/**
	 * Get a trust anchor from the store.
	 * 
	 * @param name The name of the trust anchor
	 * @return The requested {@link TrustAnchor}
	 */
	public static Optional<TrustAnchor> get(String name) {
		return provider.get("name", name);
	}

	/**
	 * Verify a plugin certificate against the trust anchors in the store.
	 * 
	 * @param cert The plugin's certificate
	 * @return Whether the certificate could be validated
	 */
	public static boolean verifyPluginCertificate(X509Certificate cert) {
		Objects.requireNonNull(cert);

		PKIXParameters params;
		try (Stream<TrustAnchor> stream = provider.stream()) {
			params = new PKIXParameters(stream.map(t -> new java.security.cert.TrustAnchor(t.getCertificate(), null))
					.collect(Collectors.toSet()));
			params.setRevocationEnabled(false);
		} catch (InvalidAlgorithmParameterException e) {
			throw new RuntimeException(e);
		}

		try {
			CertPathValidator.getInstance("PKIX")
					.validate(CertificateFactory.getInstance("X.509").generateCertPath(List.of(cert)), params);
		} catch (CertPathValidatorException | CertificateException e) {
			return false;
		} catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
			throw new RuntimeException(e);
		}

		log.debug("Successfully verified certificate: {}", cert.getSerialNumber());
		return true;
	}

	private TrustStore() {
	}
}
