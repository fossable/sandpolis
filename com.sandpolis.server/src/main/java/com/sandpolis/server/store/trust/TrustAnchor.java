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

import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Objects;

import javax.persistence.AttributeConverter;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Converter;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import com.sandpolis.core.util.CertUtil;

/**
 * A container for a single root CA certificate.
 * 
 * @author cilki
 * @since 5.0.0
 */
@Entity
public class TrustAnchor {

	@Id
	@Column
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int db_id;

	@Column
	private String name;

	@Column
	@Convert(converter = CertificateConverter.class)
	private X509Certificate certificate;

	public TrustAnchor(String name, X509Certificate certificate) {
		this.name = Objects.requireNonNull(name);
		this.certificate = Objects.requireNonNull(certificate);
	}

	public String getName() {
		return name;
	}

	public X509Certificate getCertificate() {
		return certificate;
	}

	@Converter
	private class CertificateConverter implements AttributeConverter<X509Certificate, String> {
	
		@Override
		public String convertToDatabaseColumn(X509Certificate cert) {
			try {
				return Base64.getEncoder().encodeToString(cert.getEncoded());
			} catch (CertificateEncodingException e) {
				throw new RuntimeException(e);
			}
		}
	
		@Override
		public X509Certificate convertToEntityAttribute(String dbData) {
			try {
				return CertUtil.parse(dbData);
			} catch (CertificateException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
