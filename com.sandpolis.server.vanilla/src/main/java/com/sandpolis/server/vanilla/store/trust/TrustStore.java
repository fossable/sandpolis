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
package com.sandpolis.server.vanilla.store.trust;

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
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandpolis.core.instance.storage.MemoryMapStoreProvider;
import com.sandpolis.core.instance.storage.database.Database;
import com.sandpolis.core.instance.store.MapStore;
import com.sandpolis.core.instance.store.StoreBase.StoreConfig;
import com.sandpolis.core.util.CertUtil;
import com.sandpolis.server.vanilla.store.trust.TrustStore.TrustStoreConfig;

/**
 * The {@link TrustStore} contains trust anchors for plugin certificate
 * authorities.
 *
 * @author cilki
 * @since 5.0.0
 */
public final class TrustStore extends MapStore<String, TrustAnchor, TrustStoreConfig> {

	private static final Logger log = LoggerFactory.getLogger(TrustStore.class);

	public TrustStore() {
		super(log);
	}

	/**
	 * Verify a plugin certificate against the trust anchors in the store.
	 *
	 * @param cert The plugin's certificate
	 * @return Whether the certificate could be validated
	 */
	public boolean verifyPluginCertificate(X509Certificate cert) {
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

	@Override
	public TrustStore init(Consumer<TrustStoreConfig> configurator) {
		var config = new TrustStoreConfig();
		configurator.accept(config);

		// Install root CA if required
		if (get("PLUGIN CA").isEmpty()) {
			add(new TrustAnchor("PLUGIN CA", CertUtil.getPluginRoot()));
		}

		return (TrustStore) super.init(null);
	}

	public final class TrustStoreConfig extends StoreConfig {

		@Override
		public void ephemeral() {
			provider = new MemoryMapStoreProvider<>(TrustAnchor.class, TrustAnchor::getName);
		}

		@Override
		public void persistent(Database database) {
			provider = database.getConnection().provider(TrustAnchor.class, "name");
		}

	}

	public static final TrustStore TrustStore = new TrustStore();
}
