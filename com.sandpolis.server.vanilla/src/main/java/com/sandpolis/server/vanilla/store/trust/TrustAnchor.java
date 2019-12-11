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
package com.sandpolis.server.vanilla.store.trust;

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

	@Column(length = 4096)
	@Convert(converter = CertificateConverter.class)
	private X509Certificate certificate;

	public TrustAnchor(String name, X509Certificate certificate) {
		this.name = Objects.requireNonNull(name);
		this.certificate = Objects.requireNonNull(certificate);
	}

	TrustAnchor() {
	}

	public String getName() {
		return name;
	}

	public X509Certificate getCertificate() {
		return certificate;
	}

	@Converter
	public static class CertificateConverter implements AttributeConverter<X509Certificate, String> {

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
				return CertUtil.parseCert(dbData);
			} catch (CertificateException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
